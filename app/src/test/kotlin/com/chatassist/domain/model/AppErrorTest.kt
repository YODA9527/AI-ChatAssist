package com.chatassist.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppErrorTest {

    @Test
    fun `of 工厂方法默认产出 Recoverable`() {
        val error = AppError.of(
            code = ErrorCode.NETWORK,
            userMessage = "网络异常",
            technicalDetail = "IO",
        )
        assertTrue(error is AppError.Recoverable, "默认应为 Recoverable")
        assertEquals(ErrorCode.NETWORK, error.code)
        assertEquals("网络异常", error.userMessage)
        assertFalse(error.retryable, "未指定 retryable 应为 false")
        assertNull(error.suggestedAction)
    }

    @Test
    fun `fatal 工厂方法产出 Fatal`() {
        val error = AppError.fatal(
            code = ErrorCode.UNCAUGHT,
            userMessage = "未捕获异常",
            technicalDetail = "NPE",
        )
        assertTrue(error is AppError.Fatal)
        assertFalse(error.retryable, "Fatal 永远不 retryable")
        assertEquals(SuggestedAction.CONTACT_SUPPORT, error.suggestedAction, "Fatal 永远引导反馈")
    }

    @Test
    fun `Recoverable 子类 retryable 可显式控制`() {
        val retryable = AppError.of(
            code = ErrorCode.NETWORK,
            userMessage = "网络异常",
            technicalDetail = "IO",
            retryable = true,
            suggestedAction = SuggestedAction.RETRY,
        )
        assertTrue(retryable is AppError.Recoverable)
        assertTrue(retryable.retryable)
        assertEquals(SuggestedAction.RETRY, retryable.suggestedAction)
    }

    @Test
    fun `cause 链透传`() {
        val original = IllegalStateException("原始")
        val wrapped = AppError.of(
            code = ErrorCode.CONFIG_INVALID,
            userMessage = "状态非法",
            technicalDetail = "cause",
            cause = original,
        )
        assertEquals(original, wrapped.cause)
    }

    @Test
    fun `sealed 子类分支匹配`() {
        val fatal: AppError = AppError.fatal(ErrorCode.UNCAUGHT, "x", "y")
        val recoverable: AppError = AppError.of(ErrorCode.NETWORK, "x", "y")
        when (fatal) {
            is AppError.Fatal -> { /* 命中 */ }
            is AppError.Recoverable -> error("fatal 不应被识别为 Recoverable")
        }
        when (recoverable) {
            is AppError.Recoverable -> { /* 命中 */ }
            is AppError.Fatal -> error("recoverable 不应被识别为 Fatal")
        }
    }

    @Test
    fun `extends RuntimeException 可直接抛出`() {
        val error = AppError.of(ErrorCode.NETWORK, "网络异常", "IO")
        assertNotNull(error as RuntimeException)
    }

    @Test
    fun `ErrorCode 枚举覆盖全部 14 项`() {
        // 防止 ErrorCode 漏值
        val expected = listOf(
            "NETWORK", "DNS", "AUTH_401", "AUTH_403",
            "RATE_LIMIT_429", "SERVER_5XX", "TIMEOUT",
            "OCR_LOW_CONFIDENCE", "PARSE_LOW_CONFIDENCE",
            "PERMISSION_DENIED", "STORAGE_FULL", "CONFIG_INVALID",
            "UNCAUGHT", "UNKNOWN",
        )
        val actual = ErrorCode.values().map { it.name }
        assertEquals(expected.toSet(), actual.toSet(), "ErrorCode 枚举值有变更请同步 ErrorClassifier 文案")
    }
}
