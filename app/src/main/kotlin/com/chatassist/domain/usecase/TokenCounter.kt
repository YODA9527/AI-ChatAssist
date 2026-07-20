package com.chatassist.domain.usecase

import com.chatassist.domain.model.BuiltPrompt
import com.chatassist.domain.model.TokenUsage
import javax.inject.Inject

/**
 * Token 统计（v2.2 §13.3.4）。
 *
 * - 优先用模型返回的 usage 字段（精确，由 [com.chatassist.domain.model.ModelCallEvent.StreamDone] 提供）
 * - 模型不返回时（usage 全 0）用字符数估算：中文 ≈ 1.5 字/token，英文 ≈ 4 字/token
 *
 * 纯计算、无 Android 依赖，便于单测。
 */
class TokenCounter @Inject constructor() {

    /**
     * 估算文本 token 数。
     */
    fun estimate(text: String): Int {
        val chinese = text.count { it.code in 0x4E00..0x9FFF }
        val other = text.length - chinese
        return (chinese * 1.5 + other / 4.0).toInt().coerceAtLeast(1)
    }

    /**
     * 估算一次调用的输入/输出 token（模型未返回 usage 时兜底）。
     */
    fun estimateUsage(prompt: BuiltPrompt, response: String): TokenUsage {
        val promptText = buildString {
            append(prompt.systemPrompt)
            prompt.messages.forEach { append(it.content) }
        }
        return TokenUsage.of(
            prompt = estimate(promptText),
            completion = estimate(response),
        )
    }
}
