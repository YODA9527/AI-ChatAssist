package com.chatassist.data.repository

import com.chatassist.data.llm.network.OpenAiApiServiceFactory
import com.chatassist.data.llm.network.WhitelistInterceptor
import com.chatassist.data.llm.sse.StreamingCallExecutor
import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.BuiltPrompt
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.Message
import com.chatassist.domain.model.MessageRole
import com.chatassist.domain.model.ModelCallEvent
import com.chatassist.domain.repository.ConfigRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Provider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [ModelApiRepositoryImpl] 单元测试（v0.3）。
 *
 * 用 MockWebServer 跑端到端集成，覆盖：
 * 1. [testConnection] 成功
 * 2. [testConnection] 401 失败
 * 3. [testConnection] 5xx 重试 3 次后失败
 * 4. [testConnection] apiKey 为空抛 AUTH_401（不发请求）
 * 5. [call] 流式正常完成
 */
class ModelApiRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var whitelist: WhitelistInterceptor
    private lateinit var configRepo: ConfigRepository
    private lateinit var configRepoProvider: Provider<ConfigRepository>
    private lateinit var streamingExecutor: StreamingCallExecutor
    private lateinit var repo: ModelApiRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().build()
        whitelist = WhitelistInterceptor()
        configRepo = mockk()
        configRepoProvider = mockk()
        every { configRepoProvider.get() } returns configRepo
        streamingExecutor = StreamingCallExecutor(client, json)
        val factory = OpenAiApiServiceFactory(client, json)
        repo = ModelApiRepositoryImpl(configRepoProvider, factory, streamingExecutor, whitelist)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `testConnection success returns latency`() = runTest {
        val baseUrl = server.url("/v1").toString().trimEnd('/')
        coEvery { configRepo.snapshot() } returns sampleConfig(apiUrl = baseUrl)

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"id":"1","object":"chat.completion","created":1,"model":"x","choices":[{"index":0,"message":{"role":"assistant","content":"hi"},"finish_reason":"stop"}],"usage":{"prompt_tokens":2,"completion_tokens":1,"total_tokens":3}}""",
                ),
        )

        val result = repo.testConnection(maxAttempts = 1)
        assertTrue(result.isSuccess)
        val latency = result.getOrNull()
        assertNotNull(latency)
        assertTrue(latency!! >= 0)
        // 白名单应已被设置
        assertEquals("127.0.0.1", whitelist.allowedHost)
    }

    @Test
    fun `testConnection returns AUTH_401 without sending request when apiKey blank`() = runTest {
        coEvery { configRepo.snapshot() } returns sampleConfig(apiKey = "")

        val result = repo.testConnection(maxAttempts = 1)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertEquals(ErrorCode.AUTH_401, (ex as AppError).code)
        // 没发请求，所以 MockWebServer 队列里没东西被消费
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `testConnection returns AUTH_401 on 401 response`() = runTest {
        val baseUrl = server.url("/v1").toString().trimEnd('/')
        coEvery { configRepo.snapshot() } returns sampleConfig(apiUrl = baseUrl)

        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":{"message":"bad key"}}"""),
        )

        val result = repo.testConnection(maxAttempts = 1)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull() as AppError
        assertEquals(ErrorCode.AUTH_401, ex.code)
        assertTrue(!ex.retryable)
    }

    @Test
    fun `testConnection retries 3 times on 5xx then fails`() = runTest {
        val baseUrl = server.url("/v1").toString().trimEnd('/')
        coEvery { configRepo.snapshot() } returns sampleConfig(apiUrl = baseUrl)

        // 3 次 500
        repeat(3) {
            server.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setHeader("Content-Type", "text/plain")
                    .setBody("internal error"),
            )
        }

        val result = repo.testConnection(maxAttempts = 3)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull() as AppError
        assertEquals(ErrorCode.SERVER_5XX, ex.code)
        // 应该请求了 3 次（重试 2 次）
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `testConnection succeeds on 2nd attempt when 5xx then 200`() = runTest {
        val baseUrl = server.url("/v1").toString().trimEnd('/')
        coEvery { configRepo.snapshot() } returns sampleConfig(apiUrl = baseUrl)

        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "text/plain")
                .setBody("error"),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"id":"1","object":"chat.completion","created":1,"model":"x","choices":[{"index":0,"message":{"role":"assistant","content":"hi"},"finish_reason":"stop"}],"usage":{"prompt_tokens":2,"completion_tokens":1,"total_tokens":3}}""",
                ),
        )

        val result = repo.testConnection(maxAttempts = 3)
        assertTrue(result.isSuccess)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `call emits StreamDelta and StreamDone on success`() = runTest {
        val baseUrl = server.url("/v1").toString().trimEnd('/')
        coEvery { configRepo.snapshot() } returns sampleConfig(apiUrl = baseUrl)

        val sseBody = """
            data: {"id":"1","object":"chunk","created":1,"model":"x","choices":[{"index":0,"delta":{"content":"hi"}}]}

            data: {"id":"1","object":"chunk","created":1,"model":"x","choices":[{"index":0,"delta":{"content":" there"},"finish_reason":"stop"}],"usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}

            data: [DONE]

        """.trimIndent()
        server.enqueue(
            MockResponse()
                .setBody(sseBody)
                .setHeader("Content-Type", "text/event-stream"),
        )

        val events = repo.call(samplePrompt()).toList()
        val deltas = events.filterIsInstance<ModelCallEvent.StreamDelta>().map { it.text }
        assertEquals(listOf("hi", " there"), deltas)
        val done = events.filterIsInstance<ModelCallEvent.StreamDone>().firstOrNull()
        assertNotNull(done)
        assertEquals(3, done!!.usage.promptTokens)
        assertEquals(2, done.usage.completionTokens)
    }

    @Test
    fun `call emits AUTH_401 Error when apiKey blank`() = runTest {
        coEvery { configRepo.snapshot() } returns sampleConfig(apiKey = "")

        val events = repo.call(samplePrompt()).toList()
        val err = events.filterIsInstance<ModelCallEvent.Error>().firstOrNull()
        assertNotNull(err)
        assertEquals(ErrorCode.AUTH_401, err!!.error.code)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `call updates whitelist host before request`() = runTest {
        val baseUrl = server.url("/v1").toString().trimEnd('/')
        coEvery { configRepo.snapshot() } returns sampleConfig(apiUrl = baseUrl)

        val sseBody = """
            data: {"id":"1","object":"chunk","created":1,"model":"x","choices":[{"index":0,"delta":{"content":"x"}}]}

            data: [DONE]

        """.trimIndent()
        server.enqueue(
            MockResponse()
                .setBody(sseBody)
                .setHeader("Content-Type", "text/event-stream"),
        )

        repo.call(samplePrompt()).toList()
        // 关键安全铁闸：whitelist 必须被正确设置
        assertEquals("127.0.0.1", whitelist.allowedHost)
    }

    private fun sampleConfig(
        apiKey: String = "test-key",
        apiUrl: String = "https://api.example.com/v1",
        model: String = "test-model",
    ): AppConfig = AppConfig(
        apiKey = apiKey,
        apiUrl = apiUrl,
        model = model,
    )

    private fun samplePrompt(): BuiltPrompt = BuiltPrompt(
        systemPrompt = "You are a helpful assistant.",
        messages = listOf(Message(MessageRole.USER, "hi")),
        temperature = 0.7,
        maxTokens = 100,
        model = "test-model",
    )
}
