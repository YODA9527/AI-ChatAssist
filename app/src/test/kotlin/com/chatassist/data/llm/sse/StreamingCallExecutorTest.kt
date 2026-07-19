package com.chatassist.data.llm.sse

import com.chatassist.data.llm.dto.MessageDto
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.ModelCallEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [StreamingCallExecutor] 单元测试（v0.3）。
 *
 * 用 MockWebServer 模拟 OpenAI 兼容 SSE 协议，覆盖：
 * 1. 正常多 chunk 流（含 usage + [DONE]）
 * 2. 401 鉴权失败 → Error(AUTH_401)
 * 3. 5xx 服务异常 → Error(SERVER_5XX)
 * 4. JSON 损坏 chunk 自动跳过
 */
class StreamingCallExecutorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var executor: StreamingCallExecutor
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().build()
        executor = StreamingCallExecutor(client, json)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `parses normal SSE stream with usage and DONE`() = runTest {
        val sseBody = """
            data: {"id":"1","object":"chat.completion.chunk","created":1,"model":"x","choices":[{"index":0,"delta":{"role":"assistant"}}]}

            data: {"id":"1","object":"chat.completion.chunk","created":1,"model":"x","choices":[{"index":0,"delta":{"content":"hello"}}]}

            data: {"id":"1","object":"chat.completion.chunk","created":1,"model":"x","choices":[{"index":0,"delta":{"content":" world"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}

            data: [DONE]

        """.trimIndent()
        server.enqueue(
            MockResponse()
                .setBody(sseBody)
                .setHeader("Content-Type", "text/event-stream"),
        )

        val events = collectEvents(server.url("/v1").toString())

        val deltas = events.filterIsInstance<ModelCallEvent.StreamDelta>().map { it.text }
        assertEquals(listOf("hello", " world"), deltas)

        val done = events.filterIsInstance<ModelCallEvent.StreamDone>().firstOrNull()
        assertNotNull(done)
        assertEquals(10, done!!.usage.promptTokens)
        assertEquals(5, done.usage.completionTokens)
        assertEquals(15, done.usage.totalTokens)
        assertTrue(done.totalLatencyMs >= 0)
    }

    @Test
    fun `401 returns AUTH_401 error`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":{"message":"bad api key","type":"auth"}}"""),
        )

        val events = collectEvents(server.url("/v1").toString())

        val err = events.filterIsInstance<ModelCallEvent.Error>().firstOrNull()
        assertNotNull(err)
        assertEquals(ErrorCode.AUTH_401, err!!.error.code)
        assertTrue(err.error.userMessage.contains("API Key"))
    }

    @Test
    fun `500 returns SERVER_5XX error`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "text/json")
                .setBody("""{"error":"internal"}"""),
        )

        val events = collectEvents(server.url("/v1").toString())

        val err = events.filterIsInstance<ModelCallEvent.Error>().firstOrNull()
        assertNotNull(err)
        assertEquals(ErrorCode.SERVER_5XX, err!!.error.code)
        assertTrue(err.error.retryable)
    }

    @Test
    fun `malformed JSON chunk is skipped without error`() = runTest {
        val sseBody = """
            data: this is not json

            data: {"id":"1","object":"chunk","created":1,"model":"x","choices":[{"index":0,"delta":{"content":"ok"}}]}

            data: [DONE]

        """.trimIndent()
        server.enqueue(
            MockResponse()
                .setBody(sseBody)
                .setHeader("Content-Type", "text/event-stream"),
        )

        val events = collectEvents(server.url("/v1").toString())

        val deltas = events.filterIsInstance<ModelCallEvent.StreamDelta>().map { it.text }
        assertEquals(listOf("ok"), deltas)
        // 必须有 StreamDone 终结
        assertTrue(events.any { it is ModelCallEvent.StreamDone })
    }

    private suspend fun collectEvents(baseUrl: String): List<ModelCallEvent> =
        executor.execute(
            baseUrl = baseUrl,
            apiKey = "test-key",
            model = "test-model",
            messages = listOf(MessageDto("user", "hi")),
            temperature = 0.7,
            maxTokens = 100,
        ).toList()
}
