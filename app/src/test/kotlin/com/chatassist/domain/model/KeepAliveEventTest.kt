package com.chatassist.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * KeepAliveEvent 单测（v0.12：模块 10 契约）。
 *
 * 覆盖：sealed class 单例性 + data class 相等性。
 */
class KeepAliveEventTest {

    @Test
    fun `ServiceStarted 是 data object 单例`() {
        val a: KeepAliveEvent = KeepAliveEvent.ServiceStarted
        val b: KeepAliveEvent = KeepAliveEvent.ServiceStarted
        assertSame(a, b, "data object 应为同一实例")
    }

    @Test
    fun `ServiceStopped 是 data object 单例`() {
        val a: KeepAliveEvent = KeepAliveEvent.ServiceStopped
        val b: KeepAliveEvent = KeepAliveEvent.ServiceStopped
        assertSame(a, b, "data object 应为同一实例")
    }

    @Test
    fun `VendorPushRequested data class 相等性`() {
        val a = KeepAliveEvent.VendorPushRequested(Vendor.XIAOMI)
        val b = KeepAliveEvent.VendorPushRequested(Vendor.XIAOMI)
        val c = KeepAliveEvent.VendorPushRequested(Vendor.HUAWEI)
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertEquals(Vendor.XIAOMI, a.vendor)
    }

    @Test
    fun `RevivedByWorker data class 相等性`() {
        val a = KeepAliveEvent.RevivedByWorker("worker1")
        val b = KeepAliveEvent.RevivedByWorker("worker1")
        val c = KeepAliveEvent.RevivedByWorker("worker2")
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertEquals("worker1", a.workerName)
    }
}
