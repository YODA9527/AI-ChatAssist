package com.chatassist.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DegradationManagerTest {

    private val mgr = DegradationManager()

    @Test
    fun `初始 level 为 NONE`() {
        assertEquals(DegradationManager.Level.NONE, mgr.level.value)
        assertEquals(1.0f, mgr.pollMultiplier)
        assertTrue(mgr.streamEnabled)
        assertEquals(8000, mgr.maxContextTokens)
        assertFalse(mgr.shouldDropHistory)
    }

    @Test
    fun `连续 3 次同轴上报升 MILD`() {
        repeat(3) { mgr.report(DegradationManager.Axis.NETWORK) }
        assertEquals(DegradationManager.Level.MILD, mgr.level.value)
        assertEquals(1.5f, mgr.pollMultiplier)
        assertTrue(mgr.streamEnabled, "MILD 仍启用流式")
        assertEquals(4000, mgr.maxContextTokens)
    }

    @Test
    fun `连续 5 次同轴上报升 SEVERE`() {
        repeat(5) { mgr.report(DegradationManager.Axis.OCR) }
        assertEquals(DegradationManager.Level.SEVERE, mgr.level.value)
        assertEquals(3.0f, mgr.pollMultiplier)
        assertFalse(mgr.streamEnabled, "SEVERE 关闭流式")
        assertEquals(2000, mgr.maxContextTokens)
        assertTrue(mgr.shouldDropHistory, "SEVERE 丢弃历史")
    }

    @Test
    fun `不同轴独立计数`() {
        mgr.report(DegradationManager.Axis.NETWORK)
        mgr.report(DegradationManager.Axis.OCR)
        mgr.report(DegradationManager.Axis.MEMORY)
        assertEquals(DegradationManager.Level.NONE, mgr.level.value, "每轴各 1 次应 NONE")
    }

    @Test
    fun `成功上报降档`() {
        repeat(5) { mgr.report(DegradationManager.Axis.NETWORK) }
        assertEquals(DegradationManager.Level.SEVERE, mgr.level.value)
        repeat(2) { mgr.reportSuccess(DegradationManager.Axis.NETWORK) }
        assertEquals(DegradationManager.Level.MILD, mgr.level.value, "成功 2 次后从 SEVERE 降 MILD")
    }

    @Test
    fun `reportSuccess 不指定轴则全轴降`() {
        mgr.report(DegradationManager.Axis.NETWORK)
        mgr.report(DegradationManager.Axis.OCR)
        mgr.report(DegradationManager.Axis.MEMORY)
        repeat(3) { mgr.reportSuccess() }
        assertEquals(DegradationManager.Level.NONE, mgr.level.value)
    }

    @Test
    fun `reset 立刻回到 NONE`() {
        repeat(5) { mgr.report(DegradationManager.Axis.NETWORK) }
        mgr.reset()
        assertEquals(DegradationManager.Level.NONE, mgr.level.value)
        assertEquals(1.0f, mgr.pollMultiplier)
    }

    @Test
    fun `reportSuccess 计数为 0 不变`() {
        repeat(10) { mgr.reportSuccess() }
        assertEquals(DegradationManager.Level.NONE, mgr.level.value)
    }
}
