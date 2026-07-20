package com.chatassist.domain.usecase

import com.chatassist.domain.model.Pricing
import com.chatassist.domain.model.TokenUsage

/**
 * 费用计算（v2.2 §13.3.5，纯函数无副作用）。
 *
 * 费用（微元，避免浮点误差）：
 * `cost = promptTokens/1e6 * inputPrice + completionTokens/1e6 * outputPrice - cachedTokens/1e6 * cachedPrice`
 * 缓存 token 通常更便宜，已含在 prompt 里，故相减。
 * 结果下限 0（防止异常定价产生负费用）。
 */
object CostCalculator {

    fun calculate(usage: TokenUsage, pricing: Pricing): Long {
        val input = usage.promptTokens.toDouble() / 1_000_000 * pricing.inputPricePerMToken
        val output = usage.completionTokens.toDouble() / 1_000_000 * pricing.outputPricePerMToken
        val cached = usage.cachedTokens.toDouble() / 1_000_000 * pricing.cachedPricePerMToken
        val totalYuan = input + output - cached
        return maxOf(0.0, totalYuan * 1_000_000).toLong()
    }
}
