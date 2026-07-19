package com.chatassist.domain.model

import kotlinx.serialization.Serializable

/**
 * 构造好的 Prompt（v0.3 引入）。
 *
 * 来自 [BuildPromptUseCase]（v0.6 接入），喂给 [com.chatassist.domain.repository.ModelApiRepository.call]。
 * 不可变（val），内部全部为只读结构。
 */
@Serializable
data class BuiltPrompt(
    /** System Prompt：人设、规则、目标语气。 */
    val systemPrompt: String,
    /** 多轮消息（已拼接 history，按时间升序）。 */
    val messages: List<Message>,
    /** 采样温度 0-2，默认 0.7。 */
    val temperature: Double = 0.7,
    /** 单次响应最大 Token 数，默认 1024。 */
    val maxTokens: Int = 1024,
    /** 目标模型名（如 "deepseek-chat"），由调用方指定链路决定。 */
    val model: String,
)

/**
 * 单条对话消息。
 *
 * `role` 决定消息语义。`content` 始终是纯文本（多模态 v0.3 不支持，留 callWithImage）。
 */
@Serializable
data class Message(
    val role: MessageRole,
    val content: String,
)

/**
 * 消息角色枚举。
 *
 * 仅暴露与 OpenAI 协议对齐的三个角色 + 预留 TOOL（v0.x 不使用，但避免后续 breaking change）。
 */
@Serializable
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL,
}
