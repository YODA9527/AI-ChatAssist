package com.chatassist.data.llm.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI 兼容协议的 chat completions 响应体（v0.3 引入）。
 *
 * 同时承担两种角色：
 * - 非流式：完整响应一次性返回（choices 里的 `message` 必有）
 * - 流式：每条 SSE 事件一个 chunk（choices 里的 `delta` 必有，message 必为 null）
 */
@Serializable
data class ChatResponseDto(
    val id: String,
    @SerialName("object")
    val objectType: String? = null,
    val created: Long,
    val model: String,
    val choices: List<ChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
    val error: ErrorDto? = null,
)

/**
 * 单个 choice。
 *
 * 非流式：`message` 有值，`delta` 为 null。
 * 流式：每条 chunk `delta` 有值（可能只含 `content`、可能只含 `role`、可能二者都有）。
 * 终止时 `finish_reason` = "stop" / "length" / "content_filter" / "tool_calls"。
 */
@Serializable
data class ChoiceDto(
    val index: Int,
    val message: MessageDto? = null,
    val delta: DeltaDto? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

/**
 * 流式增量片段。
 *
 * 首条 chunk 通常只含 `role`（"assistant"），后续 chunk 只含 `content`，
 * 终止 chunk 的 `content` 为 null 而 `finish_reason` 有值。
 */
@Serializable
data class DeltaDto(
    val role: String? = null,
    val content: String? = null,
)

/**
 * Token 用量。
 *
 * `prompt_tokens` + `completion_tokens` 应等于 `total_tokens`，但部分厂商不保证等式（DeepSeek 等）。
 * 计费按各自单价的 `prompt_tokens` / `completion_tokens` 分别计算。
 */
@Serializable
data class UsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0,
    @SerialName("prompt_tokens_details")
    val promptTokensDetails: PromptTokensDetailsDto? = null,
)

/** 输入 token 明细（部分厂商返回，如 DeepSeek 的缓存命中）。 */
@Serializable
data class PromptTokensDetailsDto(
    @SerialName("cached_tokens")
    val cachedTokens: Int = 0,
)

/**
 * 服务端错误（如 400 / 401 / 429 / 5xx body 里的 error 字段）。
 */
@Serializable
data class ErrorDto(
    val message: String,
    val type: String? = null,
    val code: String? = null,
    val param: String? = null,
)
