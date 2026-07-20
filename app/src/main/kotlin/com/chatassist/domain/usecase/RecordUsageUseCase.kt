package com.chatassist.domain.usecase

import com.chatassist.domain.model.BuiltPrompt
import com.chatassist.domain.model.TokenUsage
import com.chatassist.domain.model.UsageRecord
import com.chatassist.domain.model.UsageStatus
import com.chatassist.domain.model.deriveProvider
import com.chatassist.domain.repository.PricingRepository
import com.chatassist.domain.repository.UsageRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 计费记录写入用例（v2.2 §13.3.9）。
 *
 * 模型调用完成后由 [com.chatassist.domain.usecase.ReplySuggestionUseCase] 调用：
 * - 成功：按模型返回的 usage（或估算）计算费用并落库
 * - 失败/取消：仍落库（status=FAILED/CANCELLED），便于统计成功率
 *
 * 费用计算失败、写库失败一律兜底忽略，绝不阻塞生成主链路。
 */
@Singleton
class RecordUsageUseCase @Inject constructor(
    private val usageRepo: UsageRepository,
    private val pricingRepo: PricingRepository,
    private val tokenCounter: TokenCounter,
) {

    suspend fun recordSuccess(
        prompt: BuiltPrompt,
        fullResponse: String,
        targetPackage: String,
        usage: TokenUsage,
    ) {
        recordInternal(prompt, fullResponse, targetPackage, usage, UsageStatus.SUCCESS)
    }

    suspend fun recordFailure(targetPackage: String) {
        recordInternal(null, "", targetPackage, TokenUsage.of(0, 0, 0), UsageStatus.FAILED)
    }

    private suspend fun recordInternal(
        prompt: BuiltPrompt?,
        fullResponse: String,
        targetPackage: String,
        rawUsage: TokenUsage,
        status: UsageStatus,
    ) {
        runCatching {
            // 成功但模型未返回 usage（全 0）→ 估算兜底
            val usage = if (status == UsageStatus.SUCCESS && rawUsage.totalTokens == 0) {
                tokenCounter.estimateUsage(prompt!!, fullResponse)
            } else {
                rawUsage
            }
            val model = prompt?.model ?: "unknown"
            val cost = if (status == UsageStatus.SUCCESS) {
                pricingRepo.calculateCost(usage, model)
            } else {
                0L
            }
            usageRepo.record(
                UsageRecord(
                    createdAt = System.currentTimeMillis(),
                    provider = if (prompt != null) deriveProvider(model) else "unknown",
                    model = model,
                    promptTokens = usage.promptTokens,
                    completionTokens = usage.completionTokens,
                    totalTokens = usage.totalTokens,
                    cachedTokens = usage.cachedTokens,
                    costMicro = cost,
                    targetPackage = targetPackage,
                    status = status,
                ),
            )
        }.onFailure { Timber.w(it, "计费记录写入失败（已忽略）") }
    }
}
