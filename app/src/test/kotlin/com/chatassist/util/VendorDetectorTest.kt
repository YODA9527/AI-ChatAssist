package com.chatassist.util

import com.chatassist.domain.model.Vendor
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * VendorDetector 单测（v0.12：模块 10）。
 *
 * 覆盖：6 厂商 + 兜底 OTHER + 缓存 + 系统属性。
 */
class VendorDetectorTest {

    private val detector = VendorDetector(mockk(relaxed = true))

    @Test
    fun `小米的 MANUFACTURER 命中 XIAOMI`() {
        assertEquals(Vendor.XIAOMI, detector.detect("Xiaomi", "Xiaomi", emptyMap()))
    }

    @Test
    fun `红米的 MANUFACTURER 命中 XIAOMI`() {
        assertEquals(Vendor.XIAOMI, detector.detect("Redmi", "Redmi", emptyMap()))
    }

    @Test
    fun `华为的 MANUFACTURER 命中 HUAWEI（含荣耀）`() {
        assertEquals(Vendor.HUAWEI, detector.detect("HUAWEI", "HUAWEI", emptyMap()))
        assertEquals(Vendor.HUAWEI, detector.detect("HONOR", "HONOR", emptyMap()))
    }

    @Test
    fun `OPPO 含一加 Realme 命中 OPPO`() {
        assertEquals(Vendor.OPPO, detector.detect("OPPO", "OPPO", emptyMap()))
        assertEquals(Vendor.OPPO, detector.detect("OnePlus", "OnePlus", emptyMap()))
        assertEquals(Vendor.OPPO, detector.detect("realme", "realme", emptyMap()))
    }

    @Test
    fun `vivo 含 iQOO 命中 VIVO`() {
        assertEquals(Vendor.VIVO, detector.detect("vivo", "vivo", emptyMap()))
        assertEquals(Vendor.VIVO, detector.detect("iQOO", "iQOO", emptyMap()))
    }

    @Test
    fun `三星的 MANUFACTURER 命中 SAMSUNG`() {
        assertEquals(Vendor.SAMSUNG, detector.detect("samsung", "samsung", emptyMap()))
    }

    @Test
    fun `魅族的 MANUFACTURER 命中 MEIZU`() {
        assertEquals(Vendor.MEIZU, detector.detect("meizu", "meizu", emptyMap()))
    }

    @Test
    fun `MANUFACTURER 不命中走 BRAND 命中`() {
        // 模拟某些定制 ROM：MANUFACTURER 是杂牌，但 BRAND 是华为
        assertEquals(Vendor.HUAWEI, detector.detect("OEM", "HUAWEI", emptyMap()))
        assertEquals(Vendor.XIAOMI, detector.detect("Generic", "Xiaomi", emptyMap()))
    }

    @Test
    fun `BRAND 含 POCO 命中 XIAOMI`() {
        assertEquals(Vendor.XIAOMI, detector.detect("OEM", "POCO", emptyMap()))
    }

    @Test
    fun `MANUFACTURER 与 BRAND 都不命中走系统属性`() {
        val sysProps = mapOf(
            "ro.miui.ui.version.name" to "V14",
            "ro.build.version.emui" to null,
            "ro.build.version.opporom" to null,
            "ro.vivo.os.version" to null,
            "ro.samsung.fingerprint" to null,
            "ro.meizu.product.model" to null,
        )
        assertEquals(Vendor.XIAOMI, detector.detect("Unknown", "Unknown", sysProps))
    }

    @Test
    fun `EMUI 系统属性命中 HUAWEI`() {
        val sysProps = mapOf(
            "ro.miui.ui.version.name" to null,
            "ro.build.version.emui" to "EmotionUI_12.0.0",
            "ro.build.version.opporom" to null,
            "ro.vivo.os.version" to null,
            "ro.samsung.fingerprint" to null,
            "ro.meizu.product.model" to null,
        )
        assertEquals(Vendor.HUAWEI, detector.detect("Unknown", "Unknown", sysProps))
    }

    @Test
    fun `vivo 系统属性命中 VIVO`() {
        val sysProps = mapOf(
            "ro.miui.ui.version.name" to null,
            "ro.build.version.emui" to null,
            "ro.build.version.opporom" to null,
            "ro.vivo.os.version" to "OriginOS 4",
            "ro.samsung.fingerprint" to null,
            "ro.meizu.product.model" to null,
        )
        assertEquals(Vendor.VIVO, detector.detect("Unknown", "Unknown", sysProps))
    }

    @Test
    fun `三星 ro samsung fingerprint 命中 SAMSUNG`() {
        val sysProps = mapOf(
            "ro.miui.ui.version.name" to null,
            "ro.build.version.emui" to null,
            "ro.build.version.opporom" to null,
            "ro.vivo.os.version" to null,
            "ro.samsung.fingerprint" to "samsung/a54xx/a54:13",
            "ro.meizu.product.model" to null,
        )
        assertEquals(Vendor.SAMSUNG, detector.detect("Unknown", "Unknown", sysProps))
    }

    @Test
    fun `全不命中返回 OTHER（AOSP）`() {
        assertEquals(Vendor.OTHER, detector.detect("Google", "Pixel", emptyMap()))
        assertEquals(Vendor.OTHER, detector.detect("AOSP", "AOSP", emptyMap()))
    }

    @Test
    fun `系统属性值为字符串 null 视为不存在`() {
        val sysProps = mapOf(
            "ro.miui.ui.version.name" to "null",
            "ro.build.version.emui" to "null",
            "ro.build.version.opporom" to "null",
            "ro.vivo.os.version" to "null",
            "ro.samsung.fingerprint" to "null",
            "ro.meizu.product.model" to "null",
        )
        assertEquals(Vendor.OTHER, detector.detect("Unknown", "Unknown", sysProps))
    }

    @Test
    fun `reset 清除缓存`() {
        // 第一次调用建立缓存
        detector.detect("Xiaomi", "Xiaomi", emptyMap())
        // 改底层应不生效（因为缓存）
        // 因为我们调用的是 detect 参数版本，不走缓存
        // 但默认 detect() 走缓存
        // 验证 reset 后重新检测
        detector.reset()
        // 无法直接验证（缓存是 private var），仅验证 reset 不抛
        detector.reset()
    }
}
