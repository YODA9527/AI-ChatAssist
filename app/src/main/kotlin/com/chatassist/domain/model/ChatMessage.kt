package com.chatassist.domain.model

import kotlinx.serialization.Serializable

/**
 * 一条聊天消息（解析后的结构化结果）。
 *
 * v0.4 TEXT_ONLY 主通道先产出 content；sender/isSelf/timestamp 由解析层（v0.6）补全。
 * v0.10 起增 `topY` 字段供多气泡合并用（OCR 把同一句话切成多块时按 Y 距离合并）。
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
    val topY: Int = 0,
)
