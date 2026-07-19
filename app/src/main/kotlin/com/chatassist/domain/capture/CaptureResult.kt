package com.chatassist.domain.capture

import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.CaptureMode

/**
 * 单次捕获的结果。
 *
 * @param mode   本次实际使用的通道
 * @param messages 提取到的消息
 * @param confidence 整体置信度（OCR 通道有，无障碍通道为 null）
 * @param note   额外说明（如 "OCR 通道 v0.5 未接入"）
 */
data class CaptureResult(
    val mode: CaptureMode,
    val messages: List<ChatMessage>,
    val targetPackage: String = "",
    val confidence: Float? = null,
    val note: String? = null,
)
