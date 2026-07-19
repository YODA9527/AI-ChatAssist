package com.chatassist.data.llm.network

import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [WhitelistInterceptor] 单元测试（v0.3）。
 *
 * 覆盖：
 * 1. 白名单 host 通过
 * 2. 非白名单 host 抛 [AppError]（NETWORK）
 * 3. allowedHost=null 抛 [AppError]（NETWORK）
 * 4. host 大小写不敏感
 * 5. 端口不参与匹配
 */
class WhitelistInterceptorTest {

    private val interceptor = WhitelistInterceptor()

    @Test
    fun `allowed host passes through`() {
        interceptor.allowedHost = "api.example.com"
        val req = Request.Builder().url("https://api.example.com/v1/chat/completions").build()
        val chain = mockk<Interceptor.Chain>()
        val resp = mockk<Response>(relaxed = true)
        every { chain.request() } returns req
        every { chain.proceed(req) } returns resp

        val result = interceptor.intercept(chain)

        assertEquals(resp, result)
        verify(exactly = 1) { chain.proceed(req) }
    }

    @Test
    fun `disallowed host throws AppError NETWORK`() {
        interceptor.allowedHost = "api.example.com"
        val req = Request.Builder().url("https://evil.com/steal").build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req

        val ex = assertThrows<AppError> { interceptor.intercept(chain) }
        assertEquals(ErrorCode.NETWORK, ex.code)
        assertTrue(ex.userMessage.contains("evil.com"))
        assertTrue(ex.userMessage.contains("api.example.com"))
    }

    @Test
    fun `null allowed host throws AppError`() {
        interceptor.allowedHost = null
        val req = Request.Builder().url("https://api.example.com/").build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req

        val ex = assertThrows<AppError> { interceptor.intercept(chain) }
        assertEquals(ErrorCode.NETWORK, ex.code)
    }

    @Test
    fun `host matching is case-insensitive`() {
        interceptor.allowedHost = "API.Example.COM"
        val req = Request.Builder().url("https://api.example.com/v1").build()
        val chain = mockk<Interceptor.Chain>()
        val resp = mockk<Response>(relaxed = true)
        every { chain.request() } returns req
        every { chain.proceed(req) } returns resp

        interceptor.intercept(chain)
        verify(exactly = 1) { chain.proceed(req) }
    }

    @Test
    fun `port is not part of whitelist check`() {
        // 同一 host 不同端口均应放行
        interceptor.allowedHost = "api.example.com"
        val req443 = Request.Builder().url("https://api.example.com:443/v1").build()
        val req8443 = Request.Builder().url("https://api.example.com:8443/v1").build()
        val chain443 = mockk<Interceptor.Chain>()
        val chain8443 = mockk<Interceptor.Chain>()
        val resp = mockk<Response>(relaxed = true)
        every { chain443.request() } returns req443
        every { chain443.proceed(req443) } returns resp
        every { chain8443.request() } returns req8443
        every { chain8443.proceed(req8443) } returns resp

        interceptor.intercept(chain443)
        interceptor.intercept(chain8443)
        verify(exactly = 1) { chain443.proceed(req443) }
        verify(exactly = 1) { chain8443.proceed(req8443) }
    }
}
