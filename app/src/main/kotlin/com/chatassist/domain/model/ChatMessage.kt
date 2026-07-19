package com.chatassist.domain.model

import kotlinx.serialization.Serializable

/**
 * 一条聊天消息（解析后的结构化结果）。
 *
 * v0.4 TEXT_ONLY 主通道先产出 content；sender/isSelf/timestamp 由解析层（v0.6）补全。
 */
@Serializable
data class ChatMessage(
    val id: String,
    val sender: String = "",
    val content: String,
    val timestamp: Long = 0L,
    val isSelf: Boolean = false,
    val confidence: Float = 1f,
    val source: CaptureMode = CaptureMode.TEXT_ONLY,
)
