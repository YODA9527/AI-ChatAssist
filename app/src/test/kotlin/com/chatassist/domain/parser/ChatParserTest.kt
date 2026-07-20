package com.chatassist.domain.parser

import com.chatassist.domain.model.CaptureMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatParserTest {

    private val parser = ChatParser()

    // ============ A. 置信度评分 ============

    @Test
    fun `A1 TEXT_ONLY 完整几何 长文本 confidence 接近 1`() {
        // 基准 0.7 + TEXT_ONLY 0.1 + 有几何 0.1 + 长文本 0.05 + 命中预设 0.05 = 1.0
        val msg = parser.parse(
            "今晚一起吃火锅吗",
            centerX = 200,
            screenWidth = 1000,
            pkg = "com.tencent.mm",
            source = CaptureMode.TEXT_ONLY,
            index = 0,
        )
        assertTrue(msg.confidence >= 0.99f, "预期 ~1.0 实际 ${msg.confidence}")
    }

    @Test
    fun `A2 OCR_ONLY 完整几何 confidence 高于无几何`() {
        val geo = parser.parse("在吗", centerX = 200, screenWidth = 1000, pkg = "", source = CaptureMode.OCR_ONLY, index = 0)
        val noGeo = parser.parse("在吗", centerX = -1, screenWidth = 0, pkg = "", source = CaptureMode.OCR_ONLY, index = 1)
        assertTrue(geo.confidence > noGeo.confidence, "几何应加分：${geo.confidence} vs ${noGeo.confidence}")
    }

    @Test
    fun `A3 太短文本 1-2 字 confidence 显著下降`() {
        val short = parser.parse("在", centerX = 200, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 0)
        val long = parser.parse("今晚一起吃火锅吗", centerX = 200, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 1)
        assertTrue(short.confidence < long.confidence - 0.3f, "短文本应显著扣分：${short.confidence} vs ${long.confidence}")
        assertTrue(short.confidence < 0.7f, "1 字应 < 0.7 阈值：${short.confidence}")
    }

    @Test
    fun `A4 位置贴边 confidence 下降`() {
        val edge = parser.parse("你好呀", centerX = 50, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 0)
        val center = parser.parse("你好呀", centerX = 500, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 1)
        assertTrue(edge.confidence < center.confidence, "贴边应扣分：${edge.confidence} vs ${center.confidence}")
    }

    @Test
    fun `A5 系统消息 confidence 固定 0_5`() {
        val msg = parser.parse("上午 10:30", centerX = 500, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 0)
        assertEquals(0.5f, msg.confidence, 0.001f)
    }

    @Test
    fun `A6 命中 App 预设 confidence 略增`() {
        val preset = parser.parse("今晚吃火锅", centerX = 200, screenWidth = 1000, pkg = "com.tencent.mm", source = CaptureMode.TEXT_ONLY, index = 0)
        val nopreset = parser.parse("今晚吃火锅", centerX = 200, screenWidth = 1000, pkg = "com.unknown.app", source = CaptureMode.TEXT_ONLY, index = 1)
        assertTrue(preset.confidence > nopreset.confidence, "命中预设应加分：${preset.confidence} vs ${nopreset.confidence}")
    }

    @Test
    fun `A7 confidence 永远在 0 到 1 之间`() {
        val cases = listOf(
            Triple("", 500, 1000),
            Triple("a", 0, 0),
            Triple("在吗", 5, 1000),
            Triple("在吗", 995, 1000),
            Triple("很长的对话内容超过十个字符啊啊啊", 500, 1000),
        )
        for ((t, x, w) in cases) {
            val msg = parser.parse(t, x, w, "", CaptureMode.OCR_ONLY, 0)
            assertTrue(msg.confidence in 0f..1f, "confidence=${msg.confidence} text=$t")
        }
    }

    // ============ B. App 预设规则库 ============

    @Test
    fun `B1 微信右侧为自己`() {
        val msg = parser.parse("在吗", centerX = 800, screenWidth = 1000, pkg = "com.tencent.mm", source = CaptureMode.TEXT_ONLY, index = 0)
        assertTrue(msg.isSelf)
    }

    @Test
    fun `B2 企业微信右侧为自己`() {
        val msg = parser.parse("收到", centerX = 800, screenWidth = 1000, pkg = "com.tencent.wework", source = CaptureMode.TEXT_ONLY, index = 0)
        assertTrue(msg.isSelf, "企业微信右侧应判自己")
    }

    @Test
    fun `B3 钉钉飞书 QQ 三个内置 App 右侧均为自己`() {
        listOf("com.alibaba.dingtalk", "com.ss.android.lark", "com.tencent.mobileqq").forEach { pkg ->
            val msg = parser.parse("ok", centerX = 800, screenWidth = 1000, pkg = pkg, source = CaptureMode.TEXT_ONLY, index = 0)
            assertTrue(msg.isSelf, "$pkg 右侧应判自己")
        }
    }

    @Test
    fun `B4 未知 App 按通用规则右侧为自己`() {
        val msg = parser.parse("在吗", centerX = 800, screenWidth = 1000, pkg = "com.unknown.app", source = CaptureMode.TEXT_ONLY, index = 0)
        assertTrue(msg.isSelf)
    }

    // ============ F. 系统关键词扩充 ============

    @Test
    fun `F1 红包已拆开是系统消息`() {
        val msg = parser.parse("红包已拆开", centerX = 500, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 0)
        assertEquals("系统", msg.sender)
    }

    @Test
    fun `F2 拍了拍是系统消息`() {
        val msg = parser.parse("你拍了拍我", centerX = 500, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 0)
        assertEquals("系统", msg.sender)
    }

    @Test
    fun `F3 消息发送失败是系统消息`() {
        val msg = parser.parse("消息发送失败", centerX = 500, screenWidth = 1000, pkg = "", source = CaptureMode.TEXT_ONLY, index = 0)
        assertEquals("系统", msg.sender)
    }

    // ============ 基础位置 / 时间戳（v0.6 既有）============

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
