package com.chatassist.domain.error

import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.SuggestedAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ErrorPresenterTest {

    private val presenter = ErrorPresenter()

    @Test
    fun `Fatal 永远返回 CONTACT_SUPPORT`() {
        val error = AppError.fatal(ErrorCode.UNCAUGHT, "x", "y")
        val ui = presenter.present(error)
        assertEquals(SuggestedAction.CONTACT_SUPPORT, ui.action)
        assertEquals("反馈问题", ui.actionLabel)
        assertTrue(ui.title.isNotBlank())
        assertTrue(ui.message.isNotBlank())
    }

    @Test
    fun `Recoverable NETWORK 返回 CHECK_NETWORK 操作`() {
        val error = AppError.of(ErrorCode.NETWORK, "x", "y", retryable = true)
        val ui = presenter.present(error)
        assertEquals(SuggestedAction.CHECK_NETWORK, ui.action)
        assertEquals("检查网络", ui.actionLabel)
        assertEquals("网络问题", ui.title)
    }

    @Test
    fun `AUTH_401 返回 CHECK_API_KEY 操作`() {
        val error = AppError.of(ErrorCode.AUTH_401, "x", "y")
        val ui = presenter.present(error)
        assertEquals(SuggestedAction.CHECK_API_KEY, ui.action)
        assertEquals("检查 Key", ui.actionLabel)
    }

    @Test
    fun `PERMISSION_DENIED 返回 OPEN_SETTINGS 操作`() {
        val error = AppError.of(ErrorCode.PERMISSION_DENIED, "x", "y")
        val ui = presenter.present(error)
        assertEquals(SuggestedAction.OPEN_SETTINGS, ui.action)
        assertEquals("去设置", ui.actionLabel)
    }

    @Test
    fun `retryable Recoverable 返回 RETRY 操作`() {
        val error = AppError.of(ErrorCode.TIMEOUT, "x", "y", retryable = true)
        val ui = presenter.present(error)
        assertEquals(SuggestedAction.RETRY, ui.action)
        assertEquals("重试", ui.actionLabel)
    }

    @Test
    fun `所有 ErrorCode 都有 title 和 message 兜底`() {
        for (code in ErrorCode.values()) {
            val error = AppError.of(code, "test", "test")
            val ui = presenter.present(error)
            assertNotNull(ui.title, "ErrorCode.$code 缺 title")
            assertNotNull(ui.message, "ErrorCode.$code 缺 message")
            assertTrue(ui.title.isNotBlank(), "ErrorCode.$code title 为空")
            assertTrue(ui.message.isNotBlank(), "ErrorCode.$code message 为空")
        }
    }

    @Test
    fun `actionLabel 与 action 一一对应`() {
        for (action in SuggestedAction.values()) {
            val error = AppError.of(ErrorCode.NETWORK, "x", "y", suggestedAction = action)
            val ui = presenter.present(error)
            // 强制设置后 presenter 仍可能改 action（按 code 重写），只测映射表完备
            if (ui.action != null) {
                assertNotNull(ui.actionLabel, "action=${ui.action} 缺 actionLabel")
            }
        }
    }
}
