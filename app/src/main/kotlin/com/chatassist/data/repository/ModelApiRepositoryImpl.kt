package com.chatassist.data.repository

import com.chatassist.data.llm.dto.ChatRequestDto
import com.chatassist.data.llm.dto.MessageDto
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
import com.chatassist.domain.model.SuggestedAction
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.ModelApiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 模型 API 仓库实现（v0.3 引入）。
 *
 * **唯一网络出口**：本类是 [WhitelistInterceptor] 的唯一驱动器。
 * 每次调用前根据用户配置更新白名单 host。
 *
 * **重试策略**：
 * - [call]（流式）—— 不在 Repository 层做自动重试。流已开始后中途失败不可幂等，
 *   错误的 Error 事件透传给上层 UseCase 由其决定（v0.4 接入）。
 * - [testConnection]（非流式）—— 3 次指数退避（1s / 2s / 4s），仅对 5xx / IOException 重试，
 *   4xx（参数/鉴权错误）直接抛。
 *
 * **配置响应**：每次调用时 `snapshot()` 拿最新配置；OkHttpClient 共享保证连接池复用。
 */
@Singleton
class ModelApiRepositoryImpl @Inject constructor(
    private val configRepoProvider: Provider<ConfigRepository>,
    private val serviceFactory: OpenAiApiServiceFactory,
    private val streamingExecutor: StreamingCallExecutor,
    private val whitelist: WhitelistInterceptor,
) : ModelApiRepository {

    // =========================== 流式调用 ===========================

    override fun call(prompt: BuiltPrompt): Flow<ModelCallEvent> = flow {
        val cfg = configRepoProvider.get().snapshot()
        val apiKey = cfg.apiKey.trim()
        if (apiKey.isBlank()) {
            emit(
                ModelCallEvent.Error(
                    AppError(
                        code = ErrorCode.AUTH_401,
                        userMessage = "请先在设置中填写 API Key",
                        technicalDetail = "apiKey is blank in snapshot",
                        retryable = false,
                        suggestedAction = SuggestedAction.CHECK_API_KEY,
                    ),
                ),
            )
            return@flow
        }
        if (cfg.apiUrl.isBlank()) {
            emit(
                ModelCallEvent.Error(
                    AppError(
                        code = ErrorCode.CONFIG_INVALID,
                        userMessage = "请先在设置中填写 API URL",
                        technicalDetail = "apiUrl is blank",
                        retryable = false,
                        suggestedAction = SuggestedAction.OPEN_SETTINGS,
                    ),
                ),
            )
            return@flow
        }

        whitelist.allowedHost = parseHost(cfg.apiUrl)
        val messages = buildMessages(prompt)
        Timber.d("Model call start: host=${whitelist.allowedHost}, model=${prompt.model}, msgs=${messages.size}")

        streamingExecutor.execute(
            baseUrl = cfg.apiUrl,
            apiKey = apiKey,
            model = prompt.model,
            messages = messages,
            temperature = prompt.temperature,
            maxTokens = prompt.maxTokens,
        ).collect { event ->
            emit(event)
        }
    }.flowOn(Dispatchers.IO)

    // =========================== 测试连接（带重试）===========================

    override suspend fun testConnection(maxAttempts: Int): Result<Long> = runCatching {
        val cfg = configRepoProvider.get().snapshot()
        val apiKey = cfg.apiKey.trim()
        if (apiKey.isBlank()) {
            throw AppError(
                code = ErrorCode.AUTH_401,
                userMessage = "请先填写 API Key",
                technicalDetail = "apiKey is blank",
                retryable = false,
                suggestedAction = SuggestedAction.CHECK_API_KEY,
            )
        }
        if (cfg.apiUrl.isBlank()) {
            throw AppError(
                code = ErrorCode.CONFIG_INVALID,
                userMessage = "请先填写 API URL",
                technicalDetail = "apiUrl is blank",
                retryable = false,
                suggestedAction = SuggestedAction.OPEN_SETTINGS,
            )
        }

        whitelist.allowedHost = parseHost(cfg.apiUrl)
        val service = serviceFactory.create(cfg.apiUrl)
        val authHeader = "Bearer $apiKey"
        Timber.d("Test connection: auth header set (keyLen=${apiKey.length}), url=${cfg.apiUrl}, model=${cfg.model}")
        val requestBody = ChatRequestDto(
            model = cfg.model,
            messages = listOf(MessageDto(role = "user", content = "hi")),
            temperature = null,
            maxTokens = 1,
            stream = false,
        )

        val effectiveMax = maxAttempts.coerceAtLeast(1)
        val started = System.currentTimeMillis()
        var attempt = 0
        var lastError: Throwable? = null

        while (attempt < effectiveMax) {
            try {
                service.chatCompletions(authHeader, requestBody)
                val latency = System.currentTimeMillis() - started
                Timber.i("Test connection ok: ${latency}ms (attempt=${attempt + 1}/$effectiveMax)")
                return@runCatching latency
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: HttpException) {
                val appErr = mapHttpException(e)
                if (appErr.retryable && attempt < effectiveMax - 1) {
                    Timber.w("Test attempt ${attempt + 1} retryable error: ${appErr.code}")
                    lastError = appErr
                    delay(backoffMs(attempt))
                    attempt++
                } else {
                    throw appErr
                }
            } catch (e: IOException) {
                if (attempt < effectiveMax - 1) {
                    Timber.w(e, "Test attempt ${attempt + 1} IO error, will retry")
                    lastError = e
                    delay(backoffMs(attempt))
                    attempt++
                } else {
                    throw mapIoException(e)
                }
            }
        }
        // 理论上不会到达这里
        throw (lastError ?: IllegalStateException("Test connection exhausted with no error captured"))
    }

    // =========================== 内部辅助 ===========================

    private fun buildMessages(prompt: BuiltPrompt): List<MessageDto> = buildList {
        if (prompt.systemPrompt.isNotBlank()) {
            add(MessageDto(role = "system", content = prompt.systemPrompt))
        }
        prompt.messages.forEach { msg ->
            add(MessageDto(role = msg.role.toWireString(), content = msg.content))
        }
    }

    private fun parseHost(url: String): String = runCatching {
        URL(url).host
    }.getOrElse {
        // 兜底：去掉 scheme 和 path，只留 host
        url.substringAfter("://").substringBefore('/').substringBefore(':')
    }

    private fun mapHttpException(e: HttpException): AppError {
        val code = e.code()
        val errBody = runCatching { e.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
        return when (code) {
            401 -> AppError(
                code = ErrorCode.AUTH_401,
                userMessage = "API Key 无效（HTTP 401）",
                technicalDetail = "HTTP 401, body=$errBody",
                retryable = false,
                suggestedAction = SuggestedAction.CHECK_API_KEY,
            )
            403 -> AppError(
                code = ErrorCode.AUTH_403,
                userMessage = "无权限访问该模型（HTTP 403）",
                technicalDetail = "HTTP 403, body=$errBody",
                retryable = false,
                suggestedAction = SuggestedAction.OPEN_SETTINGS,
            )
            429 -> AppError(
                code = ErrorCode.RATE_LIMIT_429,
                userMessage = "请求过于频繁（HTTP 429）",
                technicalDetail = "HTTP 429, body=$errBody",
                retryable = true,
                suggestedAction = SuggestedAction.RETRY,
            )
            in 500..599 -> AppError(
                code = ErrorCode.SERVER_5XX,
                userMessage = "模型服务异常（HTTP $code）",
                technicalDetail = "HTTP $code, body=$errBody",
                retryable = true,
                suggestedAction = SuggestedAction.RETRY,
            )
            else -> AppError(
                code = ErrorCode.UNKNOWN,
                userMessage = "HTTP $code",
                technicalDetail = "HTTP $code, body=$errBody",
                retryable = false,
            )
        }
    }

    private fun mapIoException(e: IOException): AppError = when (e) {
        is UnknownHostException -> AppError(
            code = ErrorCode.DNS,
            userMessage = "请检查 API URL 是否正确（无法解析域名）",
            technicalDetail = "UnknownHostException: ${e.message}",
            retryable = false,
            suggestedAction = SuggestedAction.OPEN_SETTINGS,
        )
        is SocketTimeoutException -> AppError(
            code = ErrorCode.TIMEOUT,
            userMessage = "连接超时，可重试或换模型",
            technicalDetail = "SocketTimeoutException: ${e.message}",
            retryable = true,
            suggestedAction = SuggestedAction.RETRY,
        )
        else -> AppError(
            code = ErrorCode.NETWORK,
            userMessage = "网络异常：${e.message ?: e.javaClass.simpleName}",
            technicalDetail = "IOException: ${e.javaClass.name}: ${e.message}",
            retryable = true,
            suggestedAction = SuggestedAction.CHECK_NETWORK,
        )
    }

    private fun backoffMs(attempt: Int): Long = BASE_BACKOFF_MS * (1L shl attempt)

    companion object {
        private const val BASE_BACKOFF_MS = 1_000L
    }
}

/** [MessageRole] → OpenAI 协议字符串。 */
private fun MessageRole.toWireString(): String = when (this) {
    MessageRole.SYSTEM -> "system"
    MessageRole.USER -> "user"
    MessageRole.ASSISTANT -> "assistant"
    MessageRole.TOOL -> "tool"
}

/** 防止 IDE 警告未使用参数。 */
@Suppress("unused")
private fun touchMessages(msgs: List<Message>): Int = msgs.size
