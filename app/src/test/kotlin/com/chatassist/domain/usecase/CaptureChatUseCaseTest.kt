package com.chatassist.domain.usecase

import com.chatassist.data.capture.CaptureStrategyManager
import com.chatassist.domain.capture.CaptureResult
import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CaptureChatUseCase 单测（v0.14 全量测试，P2 补 UseCase 覆盖）。
 *
 * 覆盖：invoke 透传 stateFlow + captureOnce 透传 strategy。
 */
class CaptureChatUseCaseTest {

    @Test
    fun `invoke 透传 strategy latestMessages`() {
        val strategy: CaptureStrategyManager = mockk(relaxed = true)
        val messages = listOf(
            ChatMessage(id = "1", content = "a", isSelf = true, sender = "我"),
            ChatMessage(id = "1", content = "b", isSelf = false, sender = "对方"),
        )
        val flow = MutableStateFlow(messages)
        every { strategy.latestMessages } returns flow

        val useCase = CaptureChatUseCase(strategy)
        val result = useCase()

        assertEquals(flow, result)
        assertEquals(2, result.value.size)
    }

    @Test
    fun `captureOnce 成功透传 strategy 结果`() = runTest {
        val strategy: CaptureStrategyManager = mockk(relaxed = true)
        val captureResult = CaptureResult(
            messages = listOf(ChatMessage(id = "1", content = "x", isSelf = true, sender = "我")),
            mode = CaptureMode.TEXT_ONLY,
        )
        coEvery { strategy.captureOnce() } returns Result.success(captureResult)

        val useCase = CaptureChatUseCase(strategy)
        val result = useCase.captureOnce()

        assertTrue(result.isSuccess)
        assertEquals(captureResult, result.getOrThrow())
    }

    @Test
    fun `captureOnce 失败透传 strategy 错误`() = runTest {
        val strategy: CaptureStrategyManager = mockk(relaxed = true)
        coEvery { strategy.captureOnce() } returns Result.failure(RuntimeException("no perm"))

        val useCase = CaptureChatUseCase(strategy)
        val result = useCase.captureOnce()

        assertTrue(result.isFailure)
        assertEquals("no perm", result.exceptionOrNull()?.message)
    }
}
