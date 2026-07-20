package com.chatassist.presentation.usage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatassist.domain.model.BudgetConfig
import com.chatassist.domain.model.BudgetStatus
import com.chatassist.domain.model.UsageByApp
import com.chatassist.domain.model.UsageByModel
import com.chatassist.domain.model.UsageRecord
import com.chatassist.domain.model.UsageSummary
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * 用量统计页 ViewModel（v2.2 §13.3.8）。
 *
 * 数据全来自 [UsageRepository]（响应式聚合）；预算进度由 [budget] + 本月用量实时推算。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UsageViewModel @Inject constructor(
    private val usageRepo: UsageRepository,
    private val configRepo: ConfigRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _range = MutableStateFlow(UsageRange.TODAY)
    val range: StateFlow<UsageRange> = _range.asStateFlow()

    private val rangeBounds: Flow<Pair<Long, Long>> = _range.map { it.bounds() }

    val summary: StateFlow<UsageSummary> = rangeBounds.flatMapLatest { (s, e) ->
        usageRepo.observeSummary(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsageSummary())

    val byModel: StateFlow<List<UsageByModel>> = rangeBounds.flatMapLatest { (s, e) ->
        usageRepo.observeByModel(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val byApp: StateFlow<List<UsageByApp>> = rangeBounds.flatMapLatest { (s, e) ->
        usageRepo.observeByApp(s, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budget: StateFlow<BudgetConfig> = configRepo.observeAll().map { it.budget }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetConfig())

    /** 本月预算进度（阈值与 [com.chatassist.domain.usecase.BudgetManager] 一致）。 */
    val budgetProgress: StateFlow<BudgetProgress> = combine(
        budget,
        usageRepo.observeSummary(monthStart(), System.currentTimeMillis())
            .map { it.costMicro / 1_000_000.0 },
    ) { cfg, usedYuan ->
        if (cfg.monthlyBudgetYuan <= 0) {
            BudgetProgress(usedYuan, 0.0, 0.0, BudgetStatus.OK)
        } else {
            val ratio = usedYuan / cfg.monthlyBudgetYuan
            val status = when {
                ratio >= 1.0 && cfg.blockAtExceed -> BudgetStatus.EXCEEDED_BLOCK
                ratio >= 1.0 -> BudgetStatus.EXCEEDED_WARN
                ratio >= 0.8 -> BudgetStatus.NEAR_LIMIT
                else -> BudgetStatus.OK
            }
            BudgetProgress(usedYuan, cfg.monthlyBudgetYuan, ratio, status)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetProgress(0.0, 0.0, 0.0, BudgetStatus.OK))

    private val _exportPath = MutableStateFlow<String?>(null)
    val exportPath: StateFlow<String?> = _exportPath.asStateFlow()

    fun setRange(r: UsageRange) { _range.value = r }

    fun clearAll() = viewModelScope.launch { usageRepo.clearAll() }

    fun exportUsage() = viewModelScope.launch {
        val records = usageRepo.observeAll().first()
        val csv = buildCsv(records)
        val dir = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        val file = File(dir, "usage_export_${ts()}.csv")
        runCatching { file.writeText(csv) }.onSuccess { _exportPath.value = file.absolutePath }
    }

    fun clearExportResult() { _exportPath.value = null }

    private fun buildCsv(records: List<UsageRecord>): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("时间,服务商,模型,输入token,输出token,总token,缓存token,费用(元),目标应用,状态")
        records.forEach { r ->
            sb.appendLine(
                listOf(
                    fmt.format(java.util.Date(r.createdAt)),
                    r.provider,
                    r.model,
                    r.promptTokens,
                    r.completionTokens,
                    r.totalTokens,
                    r.cachedTokens,
                    "%.6f".format(r.costMicro / 1_000_000.0),
                    r.targetPackage.ifBlank { "-" },
                    r.status.name,
                ).joinToString(","),
            )
        }
        return sb.toString()
    }

    private fun monthStart(): Long = Calendar.getInstance().let { cal ->
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    private fun ts(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(java.util.Date())
}

/** 时间范围选择。 */
enum class UsageRange(val label: String) {
    TODAY("今日"), WEEK("本周"), MONTH("本月"), ALL("全部");

    fun bounds(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return when (this) {
            TODAY -> dayStart() to now
            WEEK -> now - 7L * 86_400_000L to now
            MONTH -> monthStart() to now
            ALL -> 0L to Long.MAX_VALUE
        }
    }

    private fun dayStart(): Long = Calendar.getInstance().let { cal ->
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    private fun monthStart(): Long = Calendar.getInstance().let { cal ->
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
}

/** 预算进度快照（UI 用）。 */
data class BudgetProgress(
    val usedYuan: Double,
    val budgetYuan: Double,
    val ratio: Double,
    val status: BudgetStatus,
)
