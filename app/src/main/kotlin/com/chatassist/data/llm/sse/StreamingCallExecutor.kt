package com.chatassist.data.llm.sse

import com.chatassist.data.llm.dto.ChatRequestDto
import com.chatassist.data.llm.dto.ChatResponseDto
import com.chatassist.data.llm.dto.MessageDto
import com.chatassist.data.llm.dto.StreamOptionsDto
import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.ModelCallEvent
import com.chatassist.domain.model.SuggestedAction
import com.chatassist.domain.model.TokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SSE 流式调用执行器（v0.3 引入）。
 *
 * 把 OkHttp EventSource 包装成 `Flow<ModelCallEvent>`，业务层只关心事件流不关心 SSE 协议细节。
 *
 * **协议约定**（OpenAI 兼容）：
 * - 请求：`POST {baseUrl}/chat/completions` + `Accept: text/event-stream` + `stream=true`
 * - 响应：每行 `data: {json}`，流结束发 `data: [DONE]`
 * - 最后一个 chunk 可能含 `usage` 字段（依赖 `stream_options.include_usage=true`）
 *
 * **终结事件互斥**：`StreamDone` 与 `Error` 只发其一，由 [AtomicBoolean] 保证原子性。
 *
 * **取消语义**：协程被取消时 [awaitClose] 触发，自动 `eventSource.cancel()`。
 */
@Singleton
class StreamingCallExecutor @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    /**
     * 执行一次流式调用。
     *
     * @param baseUrl 用户配置的 API base（已含 path prefix，如 `https://api.deepseek.com/v1`）
     * @param apiKey Bearer Token
     * @param model 模型名
     * @param messages 多轮对话消息
     * @param temperature 采样温度
     * @param maxTokens 单次响应最大 Token
     */
    fun execute(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<MessageDto>,
        temperature: Double,
        maxTokens: Int,
    ): Flow<ModelCallEvent> = callbackFlow {
        val started = System.currentTimeMillis()
        val terminated = AtomicBoolean(false)
        var lastUsage: TokenUsage? = null

        // 构造请求体
        val requestBody = ChatRequestDto(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = true,
            streamOptions = StreamOptionsDto(includeUsage = true),
        )
        val bodyJson = json.encodeToString(ChatRequestDto.serializer(), requestBody)
        val okBody = bodyJson.toRequestBody(JSON_MEDIA_TYPE)

        val fullUrl = buildFullUrl(baseUrl)
        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(okBody)
            .build()

        fun finish(event: ModelCallEvent) {
            if (terminated.compareAndSet(false, true)) {
                trySend(event)
                close()
            }
        }

        val listener = object : EventSourceListener() {

            override fun onOpen(eventSource: EventSource, response: Response) {
                if (!response.isSuccessful) {
                    val errBody = runCatching { response.peekBody(BODY_PEEK_BYTES).string() }
                        .getOrDefault("")
                    val appErr = mapHttpError(response.code, errBody)
                    finish(ModelCallEvent.Error(appErr))
                    eventSource.cancel()
                }
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                if (data.isBlank() || data == "[DONE]") return

                val chunk = runCatching {
                    json.decodeFromString(ChatResponseDto.serializer(), data)
                }.getOrElse { t ->
                    Timber.w(t, "Failed to parse SSE chunk, skip. data(prefix)=${data.take(200)}")
                    return
                }

                // 提取增量文本
                chunk.choices.firstOrNull()?.delta?.content?.let { delta ->
                    if (delta.isNotEmpty()) {
                        trySend(ModelCallEvent.StreamDelta(delta))
                    }
                }

                // 记录最终 usage（OpenAI/DeepSeek 在最后一个 chunk 携带）
                chunk.usage?.let { u ->
                    lastUsage = TokenUsage.of(u.promptTokens, u.completionTokens, u.totalTokens)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                val usage = lastUsage ?: TokenUsage.of(0, 0, 0)
                val latency = System.currentTimeMillis() - started
                finish(ModelCallEvent.StreamDone(usage, latency))
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val appErr = when {
                    t is AppError -> t
                    t is SocketTimeoutException -> AppError(
                        code = ErrorCode.TIMEOUT,
                        userMessage = "模型响应超时，可重试或换模型",
                        technicalDetail = "SocketTimeoutException: ${t.message}",
                        retryable = true,
                        suggestedAction = SuggestedAction.RETRY,
                    )
                    t is UnknownHostException -> AppError(
                        code = ErrorCode.DNS,
                        userMessage = "请检查 API URL 是否正确（无法解析域名 ${t.message}）",
                        technicalDetail = "UnknownHostException: ${t.message}",
                        retryable = false,
                        suggestedAction = SuggestedAction.OPEN_SETTINGS,
                    )
                    t is IOException -> AppError(
                        code = ErrorCode.NETWORK,
                        userMessage = "网络异常：${t.message ?: "未知 IO 错误"}",
                        technicalDetail = "IOException: ${t.message}",
                        retryable = true,
                        suggestedAction = SuggestedAction.CHECK_NETWORK,
                    )
                    response != null && !response.isSuccessful -> {
                        val errBody = runCatching { response.peekBody(BODY_PEEK_BYTES).string() }
                            .getOrDefault("")
                        mapHttpError(response.code, errBody)
                    }
                    else -> AppError(
                        code = ErrorCode.UNKNOWN,
                        userMessage = "未知错误：${t?.message ?: "no detail"}",
                        technicalDetail = "Unknown failure: ${t?.javaClass?.name}: ${t?.message}",
                        retryable = true,
                    )
                }
                finish(ModelCallEvent.Error(appErr))
            }
        }

        val factory = EventSources.createFactory(client)
        val eventSource = factory.newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 拼出完整请求 URL：baseUrl 去尾 `/` + `/chat/completions`。
     */
    private fun buildFullUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return "$trimmed/chat/completions"
    }

    /**
     * HTTP 状态码 → [AppError] 映射。
     */
    private fun mapHttpError(code: Int, errBody: String): AppError {
        // 尝试从 body 解析出更具语义的 userMessage
        val parsedMessage = runCatching {
            json.parseToJsonElement(errBody)
                .let { (it as? kotlinx.serialization.json.JsonObject)
                    ?.get("error")?.let { e -> (e as? kotlinx.serialization.json.JsonObject)?.get("message") }
                    ?.let { m -> (m as? kotlinx.serialization.json.JsonPrimitive)?.content }
            }.orEmpty()
        }.getOrNull().orEmpty()

        return when (code) {
            401 -> AppError(
                code = ErrorCode.AUTH_401,
                userMessage = if (parsedMessage.isNotBlank()) "API Key 无效：$parsedMessage" else "API Key 无效",
                technicalDetail = "HTTP 401, body=$errBody",
                retryable = false,
                suggestedAction = SuggestedAction.CHECK_API_KEY,
            )
            403 -> AppError(
                code = ErrorCode.AUTH_403,
                userMessage = if (parsedMessage.isNotBlank()) "无权限访问：$parsedMessage" else "无权限访问该模型",
                technicalDetail = "HTTP 403, body=$errBody",
                retryable = false,
                suggestedAction = SuggestedAction.OPEN_SETTINGS,
            )
            429 -> AppError(
                code = ErrorCode.RATE_LIMIT_429,
                userMessage = "请求过于频繁，已自动等待重试",
                technicalDetail = "HTTP 429, body=$errBody",
                retryable = true,
                suggestedAction = SuggestedAction.RETRY,
            )
            in 500..599 -> AppError(
                code = ErrorCode.SERVER_5XX,
                userMessage = "模型服务异常（HTTP $code），已重试",
                technicalDetail = "HTTP $code, body=$errBody",
                retryable = true,
                suggestedAction = SuggestedAction.RETRY,
            )
            else -> AppError(
                code = ErrorCode.UNKNOWN,
                userMessage = "HTTP $code${if (parsedMessage.isNotBlank()) "：$parsedMessage" else ""}",
                technicalDetail = "HTTP $code, body=$errBody",
                retryable = false,
            )
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        /** peekBody 上限：8KB，足够容纳错误信息。 */
        private const val BODY_PEEK_BYTES = 8L * 1024L
    }
}
