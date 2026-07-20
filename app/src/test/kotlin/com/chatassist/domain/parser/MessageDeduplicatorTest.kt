package com.chatassist.domain.parser

import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class MessageDeduplicatorTest {

    private fun msg(text: String, sender: String, ts: Long = 0L) =
        ChatMessage(
            id = "test-$text",
            sender = sender,
            content = text,
            timestamp = ts,
            source = CaptureMode.OCR_ONLY,
        )

    @Test
    fun `D1 完全相同消息去重`() {
        val dedup = MessageDeduplicator()
        val list = listOf(
            msg("在吗", "对方"),
            msg("在吗", "对方"), // 重复
            msg("在的", "我"),
        )
        val out = dedup.filter(list)
        assertEquals(2, out.size, "应去重 1 条")
        assertEquals(listOf("在吗", "在的"), out.map { it.content })
    }

    @Test
    fun `D2 不同 sender 不算重复`() {
        val dedup = MessageDeduplicator()
        val list = listOf(
            msg("在吗", "对方"),
            msg("在吗", "我"),
        )
        val out = dedup.filter(list)
        assertEquals(2, out.size, "不同 sender 不去重")
    }

    @Test
    fun `D3 不同时间戳不同内容不算重复`() {
        val dedup = MessageDeduplicator()
        val list = listOf(
            msg("在吗", "对方", ts = 1000L),
            msg("在吗", "对方", ts = 2000L), // 时间戳不同
        )
        val out = dedup.filter(list)
        assertEquals(2, out.size, "时间戳不同不去重")
    }

    @Test
    fun `D4 reset 清空去重表`() {
        val dedup = MessageDeduplicator()
        dedup.filter(listOf(msg("在吗", "对方")))
        assertEquals(1, dedup.size())
        dedup.reset()
        assertEquals(0, dedup.size())
    }

    @Test
    fun `D5 reset 后可以重新接受同一消息`() {
        val dedup = MessageDeduplicator()
        dedup.filter(listOf(msg("在吗", "对方")))
        assertEquals(1, dedup.size())
        dedup.reset()
        assertEquals(0, dedup.size())
        val out = dedup.filter(listOf(msg("在吗", "对方")))
        assertEquals(1, out.size, "reset 后应重新接受同一消息")
    }

    @Test
    fun `D6 时间窗调小后过期指纹自动清理`() {
        val dedup = MessageDeduplicator()
        dedup.dedupWindowMs = 5L // 5ms 窗口（给 JVM 一点喘息时间避免 System.currentTimeMillis 返回相同值）
        dedup.filter(listOf(msg("在吗", "对方")))
        Thread.sleep(20) // 20ms > 5ms 窗口
        val out = dedup.filter(listOf(msg("在吗", "对方")))
        assertEquals(1, out.size, "时间窗过小后应被当作过期而重新接受")
    }
}
