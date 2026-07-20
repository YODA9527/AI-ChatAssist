package com.chatassist.domain.usecase

import com.chatassist.domain.model.BudgetStatus
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.UsageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预算管理（v2.2 §13.3.7）。
 *
 * - 用户设月度预算（元），0 = 不设
 * - 达 80% → [BudgetStatus.NEAR_LIMIT]
 * - 超 100% → [BudgetStatus.EXCEEDED_WARN]（未拦截）/ [BudgetStatus.EXCEEDED_BLOCK]（拦截）
 *
 * 预算拦截由调用方（[com.chatassist.domain.usecase.ReplySuggestionUseCase]）在生成前读取本状态，
 * 绝不能静默——必须用户可见（错误提示 / 通知）。
 */
@Singleton
class BudgetManager @Inject constructor(
    private val usageRepo: UsageRepository,
    private val configRepo: ConfigRepository,
) {

    /** 当前预算快照（已用 / 预算 / 状态），供拦截与通知共用。 */
    suspend fun currentState(): BudgetState {
        val budget = configRepo.observeAll().map { it.budget }.first()
        if (budget.monthlyBudgetYuan <= 0) return BudgetState(BudgetStatus.OK, 0.0, 0.0)

        val usedMicro = usageRepo.totalCost(monthStartMillis(), System.currentTimeMillis())
        val usedYuan = usedMicro / 1_000_000.0
        val ratio = usedYuan / budget.monthlyBudgetYuan

        val status = when {
            ratio >= 1.0 && budget.blockAtExceed -> BudgetStatus.EXCEEDED_BLOCK
            ratio >= 1.0 -> BudgetStatus.EXCEEDED_WARN
            ratio >= 0.8 -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.OK
        }
        return BudgetState(status, usedYuan, budget.monthlyBudgetYuan)
    }

    /** 仅返回状态（拦截判断用）。 */
    suspend fun checkBudget(): BudgetStatus = currentState().status

    /** 当前自然月 1 日 00:00 的本地时间戳。 */
    private fun monthStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}

/**
 * 预算状态快照。
 */
data class BudgetState(
    val status: BudgetStatus,
    val usedYuan: Double,
    val budgetYuan: Double,
)
