package com.chatassist.domain.model

import kotlinx.serialization.Serializable

/**
 * 应用错误统一类型。
 *
 * 设计原则：
 * - `code` 机器可读
 * - `userMessage` 用户可读（中文）
 * - `technicalDetail` 日志用
 * - `retryable` 决定是否走自动重试
 * - `suggestedAction` 决定 UI 引导路径
 */
@Serializable
data class AppError(
    val code: ErrorCode,
    val userMessage: String,
    val technicalDetail: String,
    val retryable: Boolean,
    val suggestedAction: SuggestedAction? = null,
) : RuntimeException(userMessage)

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
