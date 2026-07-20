package com.chatassist.domain.usecase

import com.chatassist.domain.model.BuiltPrompt
import com.chatassist.domain.model.Message
import com.chatassist.domain.model.MessageRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * TokenCounter 单测（v2.2 §13.3.4）。
 */
class TokenCounterTest {

    private val counter = TokenCounter()

    @Test
    fun `纯中文按 15 字每 token 估算`() {
        // 4 个中文字 → 4 * 1.5 = 6
        assertEquals(6, counter.estimate("你好世界"))
    }

    @Test
    fun `纯英文按 4 字每 token 估算 至少 1`() {
        // 5 个英文字 → 5/4 = 1.25 → 1
        assertEquals(1, counter.estimate("hello"))
    }

    @Test
    fun `空文本兜底为 1`() {
        assertEquals(1, counter.estimate(""))
    }

    @Test
    fun `estimateUsage 合并系统提示与消息`() {
        val prompt = BuiltPrompt(
            systemPrompt = "你是助手",
            messages = listOf(Message(MessageRole.USER, "在吗")),
            model = "deepseek-chat",
        )
        val usage = counter.estimateUsage(prompt, "我在的")
        // systemPrompt(4 中文=6) + "在吗"(2 中文=3) = 9 prompt tokens
        assertEquals(9, usage.promptTokens)
        // "我在的" = 3 中文 = 4.5 → 4
        assertEquals(4, usage.completionTokens)
        assertEquals(usage.promptTokens + usage.completionTokens, usage.totalTokens)
    }
}
