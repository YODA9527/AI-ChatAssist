package com.chatassist.domain.model

import kotlinx.serialization.Serializable

/**
 * Token 用量统计（v0.3 引入）。
 *
 * 计费依据：输入单价 × promptTokens + 输出单价 × completionTokens。
 * totalTokens 仅作展示用，不参与费用计算。
 *
 * 部分厂商（如 DeepSeek）只对 prompt + completion 计价；部分厂商（如 Claude）
 * 对输入缓存命中的部分打折；模型定价表是用户可编辑的（v2.2 13.3 增补）。
 */
@Serializable
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    /**
     * 命中缓存的 token（部分模型有，如 DeepSeek `prompt_tokens_details.cached_tokens`）。
     * 计费不同（通常更便宜），默认 0 保持向后兼容。
     */
    val cachedTokens: Int = 0,
) {
    /** 防御性补齐：若服务端没填 totalTokens，按 prompt + completion 算。 */
    companion object {
        fun of(
            prompt: Int,
            completion: Int,
            total: Int? = null,
            cached: Int = 0,
        ): TokenUsage =
            TokenUsage(
                promptTokens = prompt,
                completionTokens = completion,
                totalTokens = total ?: (prompt + completion),
                cachedTokens = cached,
            )
    }
}
