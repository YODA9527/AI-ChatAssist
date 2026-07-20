package com.chatassist.domain.error

import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.SuggestedAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorClassifierTest {

    private val classifier = ErrorClassifier()

    @Test
    fun `已分类的 AppError 透传不重复包装`() {
        val original = AppError.of(ErrorCode.NETWORK, "原始错误", "detail")
        val result = classifier.classify(original)
        assertSame(original, result, "已分类的 AppError 应透传")
    }

    @Test
    fun `UnknownHostException 映射为 DNS`() {
        val ex = UnknownHostException("api.deepseek.com")
        val result = classifier.classify(ex)
        assertEquals(ErrorCode.DNS, result.code)
        assertTrue(result is AppError.Recoverable)
    }

    @Test
    fun `SocketTimeoutException 映射为 TIMEOUT 可重试`() {
        val ex = SocketTimeoutException("Read timed out")
        val result = classifier.classify(ex)
        assertEquals(ErrorCode.TIMEOUT, result.code)
        assertTrue(result.retryable)
    }

    @Test
    fun `IOException 映射为 NETWORK 可重试`() {
        val ex = IOException("Connection reset")
        val result = classifier.classify(ex)
        assertEquals(ErrorCode.NETWORK, result.code)
        assertTrue(result.retryable)
    }

    @Test
    fun `SecurityException 映射为 PERMISSION_DENIED`() {
        val ex = SecurityException("需要悬浮窗权限")
        val result = classifier.classify(ex)
        assertEquals(ErrorCode.PERMISSION_DENIED, result.code)
    }

    @Test
    fun `IllegalStateException 映射为 CONFIG_INVALID`() {
        val ex = IllegalStateException("状态非法")
        val result = classifier.classify(ex)
        assertEquals(ErrorCode.CONFIG_INVALID, result.code)
    }

    @Test
    fun `HttpException 401 映射为 AUTH_401 引导检查 Key`() {
        val ex = httpException(401)
        val result = classifier.classify(ex)
        assertEquals(ErrorCode.AUTH_401, result.code)
        assertEquals(SuggestedAction.CHECK_API_KEY, result.suggestedAction)
    }

    @Test
    fun `HttpException 403 映射为 AUTH_403 引导检查 Key`() {
        val ex = httpException(403)
        val result = classifier.classify(ex)
        assertEquals(ErrorCode.AUTH_403, result.code)
    }

    @Test
    fun `HttpException 429 映射为 RATE_LIMIT 可重试`() {
        val ex = httpException(429)
        val result = classifier.classify(ex)
        assertEquals(ErrorCode.RATE_LIMIT_429, result.code)
        assertTrue(result.retryable)
    }

    @Test
    fun `HttpException 5xx 映射为 SERVER_5xx 可重试`() {
        for (code in 500..599) {
            val result = classifier.classify(httpException(code))
            assertEquals(ErrorCode.SERVER_5XX, result.code, "HTTP $code 应映射 SERVER_5XX")
            assertTrue(result.retryable, "HTTP $code 应可重试")
        }
    }

    @Test
    fun `HttpException 404 映射为 UNKNOWN`() {
        val result = classifier.classify(httpException(404))
        assertEquals(ErrorCode.UNKNOWN, result.code)
    }

    @Test
    fun `未知异常映射为 Fatal UNCAUGHT`() {
        val ex = RuntimeException("unknown")
        val result = classifier.classify(ex)
        assertTrue(result is AppError.Fatal, "未知异常应归为 Fatal")
        assertEquals(ErrorCode.UNCAUGHT, result.code)
        assertNotNull(result.cause)
    }

    @Test
    fun `技术详情保留 cause 链`() {
        val inner = IOException("底层网络故障")
        val outer = RuntimeException("包装异常", inner)
        val result = classifier.classify(outer)
        // cause 链透传：外层 throwable 是 AppError.cause
        assertEquals(outer, result.cause)
        // AppError 自身是 RuntimeException 子类
        assertTrue(result is RuntimeException)
    }

    @Test
    fun `userMessage 非空兜底`() {
        val ex = RuntimeException("any")
        val result = classifier.classify(ex)
        assertTrue(result.userMessage.isNotBlank(), "userMessage 不应为空")
    }

    private fun httpException(code: Int): HttpException {
        val response = Response.error<Any>(code, okhttp3.ResponseBody.create(null, "error"))
        return HttpException(response)
    }
}
