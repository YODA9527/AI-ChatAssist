package com.chatassist.domain.parser

import com.chatassist.domain.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 滚动续接去重器（v0.10：D 项）。
 *
 * 场景：
 * - OCR 通道每 2s 轮询一次 → 同一批消息会反复出现
 * - 无障碍通道重启后 → 整屏会重读
 * - 用户滚动屏幕 → 顶部/底部消息会重复识别
 *
 * 本类用 **content+sender+timestamp** 哈希作指纹，
 * 5 分钟内同指纹视为重复，跳过。
 *
 * 状态：单实例 @Singleton，进程内共享指纹表。
 * 注意：去重表不会持久化（应用重启清空），避免长期累积的"假去重"误伤。
 */
@Singleton
class MessageDeduplicator @Inject constructor() {

    /** 重复判定时间窗（毫秒），默认 5 分钟。 */
    var dedupWindowMs: Long = 5 * 60 * 1000L

    /** 指纹 → 首次见到的时间戳（用于过期清理）。 */
    private val seenFingerprints = HashMap<String, Long>()

    private fun nowMs(): Long = System.currentTimeMillis()

    private fun fingerprint(m: ChatMessage): String {
        // timestamp 不可用时仅用 content+sender 哈希
        val ts = if (m.timestamp > 0) m.timestamp.toString() else "0"
        return "${m.sender}|${m.content}|$ts"
    }

    /**
     * 过滤重复消息。
     * 副作用：把新消息指纹记入 seen 表（5 分钟后自动过期）。
     */
    fun filter(messages: List<ChatMessage>): List<ChatMessage> {
        if (messages.isEmpty()) return messages
        val now = nowMs()
        // 1) 顺手清理过期指纹
        val expired = seenFingerprints.entries.filter { now - it.value > dedupWindowMs }
        expired.forEach { seenFingerprints.remove(it.key) }

        // 2) 过滤
        val out = mutableListOf<ChatMessage>()
        for (m in messages) {
            val fp = fingerprint(m)
            if (fp in seenFingerprints) continue
            seenFingerprints[fp] = now
            out += m
        }
        return out
    }

    /** 清空去重表（用户主动重置 / 切换 App 时调用）。 */
    fun reset() {
        seenFingerprints.clear()
    }

    /** 当前指纹表大小（仅测试用）。 */
    fun size(): Int = seenFingerprints.size
}
