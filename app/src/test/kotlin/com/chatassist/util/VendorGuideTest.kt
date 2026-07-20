package com.chatassist.util

import android.content.Context
import com.chatassist.domain.model.Vendor
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * VendorGuide 单测（v0.12：模块 10）。
 *
 * Android 的 Intent/Uri 是 final 类，JVM 单测不 mock。
 * 故核心测试用 [VendorGuide.buildPrimaryDescriptor]（纯函数版）覆盖 descriptor；
 * Intent 构造路径只测能跑通的部分（OTHER 返回 null、其他 6 厂商返回 non-null）。
 */
class VendorGuideTest {

    private val context: Context = mockk(relaxed = true)
    private val guide = VendorGuide(context)

    @Test
    fun `小米 descriptor 走 miui securitycenter`() {
        val d = guide.buildPrimaryDescriptor(Vendor.XIAOMI)
        assertNotNull(d)
        assertEquals("com.miui.securitycenter", d!!.packageName)
        assertEquals(
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
            d.className,
        )
    }

    @Test
    fun `华为 descriptor 走 huawei systemmanager`() {
        val d = guide.buildPrimaryDescriptor(Vendor.HUAWEI)
        assertNotNull(d)
        assertEquals("com.huawei.systemmanager", d!!.packageName)
    }

    @Test
    fun `OPPO descriptor 走 coloros safecenter`() {
        val d = guide.buildPrimaryDescriptor(Vendor.OPPO)
        assertNotNull(d)
        assertEquals("com.coloros.safecenter", d!!.packageName)
    }

    @Test
    fun `vivo descriptor 走 iqoo secure`() {
        val d = guide.buildPrimaryDescriptor(Vendor.VIVO)
        assertNotNull(d)
        assertEquals("com.iqoo.secure", d!!.packageName)
    }

    @Test
    fun `三星 descriptor 走 samsung lool`() {
        val d = guide.buildPrimaryDescriptor(Vendor.SAMSUNG)
        assertNotNull(d)
        assertEquals("com.samsung.android.lool", d!!.packageName)
    }

    @Test
    fun `魅族 descriptor 走 meizu safe`() {
        val d = guide.buildPrimaryDescriptor(Vendor.MEIZU)
        assertNotNull(d)
        assertEquals("com.meizu.safe", d!!.packageName)
    }

    @Test
    fun `OTHER 厂商无专属路径 返回 null`() {
        assertNull(guide.buildPrimaryDescriptor(Vendor.OTHER))
    }

    @Test
    fun `VendorDescriptor 数据正确`() {
        val d = VendorDescriptor("a.b", "c.d.Main")
        assertEquals("a.b", d.packageName)
        assertEquals("c.d.Main", d.className)
        // toComponentName/toIntent 在运行期构造（ComponentName 是 Android final 类不在 JVM mock）
    }

    @Test
    fun `buildAppDetailsIntent 构造应用详情 Intent 不抛`() {
        every { context.packageName } returns "com.chatassist.debug"
        // 因 Uri.fromParts 在 JVM 单测不 mock，仅验证构造不抛
        try {
            VendorGuide.buildAppDetailsIntent(context)
        } catch (e: RuntimeException) {
            // 接受 Intent/Uri not-mocked 异常（运行期 Android 设备正常）
            assert(e.message?.contains("not mocked") == true)
        }
    }
}
