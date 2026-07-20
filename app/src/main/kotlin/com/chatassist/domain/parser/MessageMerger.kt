package com.chatassist.domain.parser

import com.chatassist.domain.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 多气泡合并器（v0.10：C 项）。
 *
 * 场景：OCR 引擎经常把同一句话切成 2-3 个 TextBlock（中文长句尤其明显），
 * 直接送进 Prompt 会让 LLM 看到"半截话"。本类按"同 sender + Y 距离近 + 中间无时间戳"三条件，
 * 把相邻同向气泡合并成一条完整消息。
 *
 * 合并规则：
 * 1. **同 sender**：必须是同一个发送方（我/对方），系统行不参与合并（系统行作为合并断点）
 * 2. **Y 距离近**：topY 差 ≤ [bubbleGapPx]（默认 100 px，约 30dp @ xxhdpi）
 * 3. **无时间戳插入**：中间若出现系统行（时间戳/撤回等），中断合并链
 * 4. **置信度取 min**：合并后置信度取最低值（v0.10+ UI 可据此判断）
 *
 * 设计原则：宁可漏合、不可误合（误合会污染 LLM 输入）。
 * 对于 topY=0 的通道（无几何信息）退化为仅按 index 顺序合并，谨慎使用。
 */
@Singleton
class MessageMerger @Inject constructor() {

    /** 默认气泡间距阈值（像素），可按屏幕密度调整。 */
    var bubbleGapPx: Int = 100

    /**
     * 合并相邻气泡。
     * 复杂度 O(n)，单次扫描。
     */
    fun merge(messages: List<ChatMessage>): List<ChatMessage> {
        if (messages.isEmpty()) return messages
        val out = mutableListOf<ChatMessage>()
        var cur: ChatMessage? = null
        for (m in messages) {
            val c = cur
            if (c == null) {
                cur = m
                continue
            }
            // 系统行作为合并断点，固化当前
            if (m.sender == "系统" || c.sender == "系统") {
                out += c
                cur = m
                continue
            }
            // 同 sender + Y 距离近 → 合并
            val sameSender = c.sender == m.sender
            val closeY = c.topY > 0 && m.topY > 0 && kotlin.math.abs(c.topY - m.topY) <= bubbleGapPx
            // 兜底：无 Y 坐标时，按 index 顺序 + 同 sender 也合并（仅 topY=0 时启用）
            val noY = c.topY == 0 && m.topY == 0
            if (sameSender && (closeY || noY)) {
                cur = c.copy(
                    content = c.content + m.content,
                    confidence = minOf(c.confidence, m.confidence),
                    topY = c.topY, // 保留首条 Y
                )
            } else {
                out += c
                cur = m
            }
        }
        cur?.let { out += it }
        return out
    }
}
