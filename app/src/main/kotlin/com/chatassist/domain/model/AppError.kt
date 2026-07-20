package com.chatassist.domain.model

import kotlinx.serialization.Serializable

/**
 * 应用错误统一类型（v0.11.0 重构为 sealed class）。
 *
 * 设计原则：
 * - `code` 机器可读
 * - `userMessage` 用户可读（中文）
 * - `technicalDetail` 日志用
 * - `retryable` 决定是否走自动重试
 * - `suggestedAction` 决定 UI 引导路径
 *
 * v0.11.0 起从 data class 升级为 sealed class：
 * - [Fatal]：致命错误，App 无法继续运行（启动崩溃、关键配置丢失）
 * - [Recoverable]：可恢复错误，用户可见但 App 可继续运行（网络、API、权限等）
 *
 * 工厂方法 [of] 默认产出 [Recoverable]，与原 data class 构造签名一致，迁移成本最低。
 */
sealed class AppError(
    open val code: ErrorCode,
    open val userMessage: String,
    open val technicalDetail: String,
    open val retryable: Boolean,
    open val suggestedAction: SuggestedAction? = null,
    override val cause: Throwable? = null,
) : RuntimeException(userMessage, cause) {

    /** 致命错误：App 无法继续运行，必须重启或重装。 */
    data class Fatal(
        override val code: ErrorCode,
        override val userMessage: String,
        override val technicalDetail: String,
        override val cause: Throwable? = null,
    ) : AppError(
        code = code,
        userMessage = userMessage,
        technicalDetail = technicalDetail,
        retryable = false,
        suggestedAction = SuggestedAction.CONTACT_SUPPORT,
        cause = cause,
    )

    /** 可恢复错误：用户可见但 App 可继续运行（默认）。 */
    data class Recoverable(
        override val code: ErrorCode,
        override val userMessage: String,
        override val technicalDetail: String,
        override val retryable: Boolean,
        override val suggestedAction: SuggestedAction? = null,
        override val cause: Throwable? = null,
    ) : AppError(
        code = code,
        userMessage = userMessage,
        technicalDetail = technicalDetail,
        retryable = retryable,
        suggestedAction = suggestedAction,
        cause = cause,
    )

    companion object {
        /**
         * 工厂方法（默认 [Recoverable]）。
         * 与原 data class 构造签名一致，便于全工程 27+ 处 `AppError(...)` → `AppError.of(...)` 机械替换。
         */
        fun of(
            code: ErrorCode,
            userMessage: String,
            technicalDetail: String,
            retryable: Boolean = false,
            suggestedAction: SuggestedAction? = null,
            cause: Throwable? = null,
        ): AppError = Recoverable(
            code = code,
            userMessage = userMessage,
            technicalDetail = technicalDetail,
            retryable = retryable,
            suggestedAction = suggestedAction,
            cause = cause,
        )

        /** 致命错误工厂。 */
        fun fatal(
            code: ErrorCode,
            userMessage: String,
            technicalDetail: String,
            cause: Throwable? = null,
        ): AppError = Fatal(
            code = code,
            userMessage = userMessage,
            technicalDetail = technicalDetail,
            cause = cause,
        )
    }
}

@Serializable
enum class ErrorCode {
    NETWORK,
    DNS,
    AUTH_401,
    AUTH_403,
    RATE_LIMIT_429,
    SERVER_5XX,
    TIMEOUT,
    OCR_LOW_CONFIDENCE,
    PARSE_LOW_CONFIDENCE,
    PERMISSION_DENIED,
    STORAGE_FULL,
    CONFIG_INVALID,
    /** v0.11.0 新增：未捕获异常（GlobalErrorHandler 兜底）。 */
    UNCAUGHT,
    UNKNOWN,
}

@Serializable
enum class SuggestedAction {
    RETRY,
    CHECK_API_KEY,
    CHECK_NETWORK,
    OPEN_SETTINGS,
    CONTACT_SUPPORT,
}
