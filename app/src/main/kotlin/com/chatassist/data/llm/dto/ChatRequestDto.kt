package com.chatassist.data.llm.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI 兼容协议的 chat completions 请求体（v0.3 引入）。
 *
 * 覆盖：OpenAI / DeepSeek / 通义千问 dashscope compatible-mode / 月之暗面 Kimi /
 * 智谱 GLM / 书生 InternLM / 字节豆包 / 腾讯混元 / 零一万物 等所有 OpenAI 兼容厂商。
 *
 * 字段顺序与 OpenAI 官方一致，便于和文档对照。
 */
@Serializable
data class ChatRequestDto(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Double? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val stream: Boolean = false,
    @SerialName("stream_options")
    val streamOptions: StreamOptionsDto? = null,
    @SerialName("top_p")
    val topP: Double? = null,
    val user: String? = null,
)

/**
 * 流式响应时附带的选项。
 *
 * `include_usage = true` → 服务端在最后一个 chunk 附带 `usage` 字段（OpenAI / DeepSeek 支持）。
 * 不支持该字段的服务端会忽略（Kotlinx Serialization 编码时始终带上）。
 */
@Serializable
data class StreamOptionsDto(
    @SerialName("include_usage")
    val includeUsage: Boolean = true,
)

/**
 * 单条消息。
 *
 * `role`: system / user / assistant
 * `name`: 可选，用于区分同角色多用户（OpenAI 协议可选字段）
 */
@Serializable
data class MessageDto(
    val role: String,
    val content: String,
    val name: String? = null,
)
