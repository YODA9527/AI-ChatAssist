package com.chatassist.domain.model

/**
 * 捕获通道模式（混合方案状态机）。
 *
 * - TEXT_ONLY：无障碍读屏主通道（v0.4 真实可用）
 * - OCR_ONLY：MediaProjection 截图 + ML Kit OCR 兜底（v0.5 接通）
 * - HYBRID：两者融合增强（后续版本）
 */
import kotlinx.serialization.Serializable

@Serializable
enum class CaptureMode { TEXT_ONLY, OCR_ONLY, HYBRID }
