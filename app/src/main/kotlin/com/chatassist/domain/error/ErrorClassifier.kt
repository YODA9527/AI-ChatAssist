package com.chatassist.domain.error

import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.SuggestedAction
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 异常 → [AppError] 分类器（v0.11.0：模块 12 A 项）。
 *
 * 唯一职责：把任意 [Throwable] 映射成统一 [AppError]，让上层不必逐异常类型处理。
 *
 * 设计原则：
 * - **已分类的 [AppError] 透传**（不重复包装）
 * - **未知异常归为 [AppError.Fatal] + [ErrorCode.UNCAUGHT]**（强制走 CrashLogger + GlobalErrorHandler）
 * - **网络层用 [HttpException.code()] 分桶**（401/403/429/5xx/其他）
 * - **保留 cause 链**（便于 CrashLogger 还原栈）
 */
@Singleton
class ErrorClassifier @Inject constructor() {

    fun classify(throwable: Throwable): AppError {
        // 1) 已分类的 AppError 透传
        if (throwable is AppError) return throwable

        // 2) 网络层异常
        return when (throwable) {
            is UnknownHostException -> recoverable(
                code = ErrorCode.DNS,
                detail = "DNS 解析失败: ${throwable.message}",
                cause = throwable,
            )
            is SocketTimeoutException -> recoverable(
                code = ErrorCode.TIMEOUT,
                detail = "请求超时: ${throwable.message}",
                cause = throwable,
                retryable = true,
            )
            is HttpException -> classifyHttp(throwable)
            is IOException -> recoverable(
                code = ErrorCode.NETWORK,
                detail = "网络异常: ${throwable.message}",
                cause = throwable,
                retryable = true,
            )
            is SecurityException -> recoverable(
                code = ErrorCode.PERMISSION_DENIED,
                detail = "权限被拒: ${throwable.message}",
                cause = throwable,
            )
            is IllegalStateException -> recoverable(
                code = ErrorCode.CONFIG_INVALID,
                detail = "状态非法: ${throwable.message}",
                cause = throwable,
            )
            else -> fatal(
                code = ErrorCode.UNCAUGHT,
                detail = "未捕获异常: ${throwable.javaClass.simpleName}: ${throwable.message}",
                cause = throwable,
            )
        }
    }

    private fun classifyHttp(http: HttpException): AppError = when (val code = http.code()) {
        401 -> recoverable(
            code = ErrorCode.AUTH_401,
            detail = "API Key 无效 (HTTP 401)",
            cause = http,
            action = SuggestedAction.CHECK_API_KEY,
        )
        403 -> recoverable(
            code = ErrorCode.AUTH_403,
            detail = "无权限访问该模型 (HTTP 403)",
            cause = http,
            action = SuggestedAction.CHECK_API_KEY,
        )
        429 -> recoverable(
            code = ErrorCode.RATE_LIMIT_429,
            detail = "请求过于频繁 (HTTP 429)",
            cause = http,
            retryable = true,
        )
        in 500..599 -> recoverable(
            code = ErrorCode.SERVER_5XX,
            detail = "服务异常 (HTTP $code)",
            cause = http,
            retryable = true,
        )
        else -> recoverable(
            code = ErrorCode.UNKNOWN,
            detail = "未识别 HTTP 状态: $code",
            cause = http,
        )
    }

    private fun recoverable(
        code: ErrorCode,
        detail: String,
        cause: Throwable? = null,
        retryable: Boolean = false,
        action: SuggestedAction? = null,
    ): AppError = AppError.of(
        code = code,
        userMessage = userMessageOf(code),
        technicalDetail = detail,
        retryable = retryable,
        suggestedAction = action,
        cause = cause,
    )

    private fun fatal(
        code: ErrorCode,
        detail: String,
        cause: Throwable? = null,
    ): AppError = AppError.fatal(
        code = code,
        userMessage = userMessageOf(code),
        technicalDetail = detail,
        cause = cause,
    )

    private fun userMessageOf(code: ErrorCode): String = when (code) {
        ErrorCode.NETWORK -> "网络异常，请检查网络连接"
        ErrorCode.DNS -> "无法解析服务器地址，请检查 API URL 是否正确"
        ErrorCode.AUTH_401 -> "API Key 无效，请在设置中检查"
        ErrorCode.AUTH_403 -> "无权限访问该模型，请尝试切换模型"
        ErrorCode.RATE_LIMIT_429 -> "请求过于频繁，已自动等待重试"
        ErrorCode.SERVER_5XX -> "模型服务异常，已自动重试"
        ErrorCode.TIMEOUT -> "模型响应超时，可重试或换模型"
        ErrorCode.OCR_LOW_CONFIDENCE -> "识别不确定，请核对后再生成"
        ErrorCode.PARSE_LOW_CONFIDENCE -> "解析不确定，请检查屏幕内容"
        ErrorCode.PERMISSION_DENIED -> "权限被拒，请在设置中开启"
        ErrorCode.STORAGE_FULL -> "存储空间已满，请清理历史记录"
        ErrorCode.CONFIG_INVALID -> "配置无效，请在设置中检查"
        ErrorCode.UNCAUGHT -> "出现未预期错误，已记录到本地日志"
        ErrorCode.UNKNOWN -> "未知错误，请稍后重试"
    }
}
