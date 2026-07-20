package com.chatassist.domain.parser

import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessageMergerTest {

    private val merger = MessageMerger()

    private fun msg(text: String, sender: String, topY: Int = 0, isSelf: Boolean = false) =
        ChatMessage(
            id = "test-$text-$topY",
            sender = sender,
            content = text,
            timestamp = 0L,
            isSelf = isSelf,
            source = CaptureMode.OCR_ONLY,
            topY = topY,
        )

    @Test
    fun `C1 同 sender Y 接近 合并`() {
        val list = listOf(
            msg("今晚一起", "对方", topY = 100, isSelf = false),
            msg("吃火锅吗", "对方", topY = 130, isSelf = false),
        )
        val out = merger.merge(list)
        assertEquals(1, out.size, "应合并为 1 条")
        assertEquals("今晚一起吃火锅吗", out[0].content)
    }

    @Test
    fun `C2 不同 sender 不合并`() {
        val list = listOf(
            msg("在吗", "对方", topY = 100, isSelf = false),
            msg("在的", "我", topY = 110, isSelf = true),
        )
        val out = merger.merge(list)
        assertEquals(2, out.size, "不同 sender 不合并")
    }

    @Test
    fun `C3 Y 距离超过阈值不合并`() {
        val list = listOf(
            msg("第一条", "对方", topY = 100, isSelf = false),
            msg("第二条", "对方", topY = 500, isSelf = false), // 差 400px 超过阈值
        )
        val out = merger.merge(list)
        assertEquals(2, out.size, "Y 距离超阈值不合并")
    }

    @Test
    fun `C4 系统行作为合并断点`() {
        val list = listOf(
            msg("第一条", "对方", topY = 100, isSelf = false),
            msg("10:30", "系统", topY = 200, isSelf = false),
            msg("第二条", "对方", topY = 110, isSelf = false), // Y 接近 100 但系统行中断
        )
        val out = merger.merge(list)
        assertEquals(3, out.size, "系统行作为断点")
    }

    @Test
    fun `C5 topY 都为 0 退化为按 index 合并`() {
        val list = listOf(
            msg("第一", "对方", topY = 0, isSelf = false),
            msg("第二", "对方", topY = 0, isSelf = false),
        )
        val out = merger.merge(list)
        assertEquals(1, out.size, "topY=0 兜底合并")
        assertEquals("第一第二", out[0].content)
    }

    @Test
    fun `C6 合并后 confidence 取 min`() {
        val list = listOf(
            msg("第一条", "对方", topY = 100, isSelf = false).copy(confidence = 0.9f),
            msg("第二条", "对方", topY = 110, isSelf = false).copy(confidence = 0.6f),
        )
        val out = merger.merge(list)
        assertEquals(0.6f, out[0].confidence, 0.001f)
    }

    @Test
    fun `C7 空列表返回空`() {
        assertEquals(0, merger.merge(emptyList()).size)
    }
}
