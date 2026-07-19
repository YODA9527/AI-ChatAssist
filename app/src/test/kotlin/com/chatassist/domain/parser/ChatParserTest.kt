package com.chatassist.domain.parser

import com.chatassist.domain.model.CaptureMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatParserTest {

    private val parser = ChatParser()

    @Test
    fun `右侧文本判定为自己`() {
        val msg = parser.parse("在吗", centerX = 900, screenWidth = 1000, pkg = "com.tencent.mm", source = CaptureMode.TEXT_ONLY, index = 0)
        assertTrue(msg.isSelf)
        assertEquals("我", msg.sender)
    }

    @Test
    fun `左侧文本判定为对方`() {
        val msg = parser.parse("你好呀", centerX = 200, screenWidth = 1000, pkg = "com.tencent.mm", source = CaptureMode.TEXT_ONLY, index = 0)
        assertFalse(msg.isSelf)
        assertEquals("对方", msg.sender)
    }

    @Test
    fun `无几何信息时退化为未知位置`() {
        val msg = parser.parse("你好", centerX = -1, screenWidth = 0, pkg = "", source = CaptureMode.OCR_ONLY, index = 0)
        assertFalse(msg.isSelf)
        assertEquals("对方", msg.sender)
        assertEquals(CaptureMode.OCR_ONLY, msg.source)
    }

    @Test
    fun `时间戳行被标记为系统消息`() {
        val msg = parser.parse("上午 10:30", centerX = 500, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 0)
        assertEquals("系统", msg.sender)
        assertFalse(msg.isSelf)
        assertTrue(msg.timestamp > 0)
    }

    @Test
    fun `撤回提示被标记为系统消息`() {
        val msg = parser.parse("你撤回了一条消息", centerX = 500, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 0)
        assertEquals("系统", msg.sender)
    }

    @Test
    fun `普通对话不解析出时间戳`() {
        val msg = parser.parse("今晚一起吃火锅吗", centerX = 200, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 0)
        assertEquals(0L, msg.timestamp)
    }
}
