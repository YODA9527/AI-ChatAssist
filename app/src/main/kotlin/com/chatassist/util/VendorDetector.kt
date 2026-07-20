package com.chatassist.util

import android.content.Context
import android.os.Build
import com.chatassist.domain.model.Vendor
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ROM 厂商检测器（v0.12：模块 10 后台保活）。
 *
 * **检测策略（按权威度从高到低）**：
 * 1. **Build.MANUFACTURER** 最权威（小米=“Xiaomi”、华为=“HUAWEI”、OPPO=“OPPO”、vivo=“vivo”、三星=“samsung”、魅族=“meizu”）
 * 2. **Build.BRAND** 兜底（部分定制 ROM 只动 BRAND 不动 MANUFACTURER）
 * 3. **系统属性** 兜底（ro.miui.ui.version.name 存在 → MIUI / ro.build.version.emui → EMUI / ro.vivo.os.version → OriginOS）
 * 4. 未匹配 → [Vendor.OTHER]
 *
 * 设计原则：
 * - **单例 + 缓存**（detectOnce() 首次检测后缓存结果，避免重复读系统属性）
 * - **失败兜底 OTHER**（不抛异常）
 * - **日志埋点**（首次检测时 Timber.i 记录，便于调试 ROM 适配）
 * - **核心逻辑纯函数化**（[detect] 接受 manufacturer/brand/systemProps 参数，便于单测覆盖所有 ROM 分支）
 *
 * 注：反射读系统属性在 Android 10+ 受限（ro.* 系统属性只允许 system 进程读），
 * 故用 try-catch 包住，失败不影响主流程。
 */
@Singleton
class VendorDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var cached: Vendor? = null

    /**
     * 检测当前 ROM 厂商（首次调用后缓存）。
     */
    fun detect(): Vendor {
        cached?.let { return it }
        val vendor = detect(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            systemProps = readSystemProperties(),
        )
        cached = vendor
        Timber.tag(TAG).i("Detected ROM vendor: $vendor (MANUFACTURER=${Build.MANUFACTURER}, BRAND=${Build.BRAND})")
        return vendor
    }

    /**
     * 核心检测逻辑（纯函数，可单测）。
     *
     * @param manufacturer Build.MANUFACTURER
     * @param brand Build.BRAND
     * @param systemProps 系统属性（key=属性名, value=属性值；null/空/"null" 视为不存在）
     */
    fun detect(
        manufacturer: String,
        brand: String,
        systemProps: Map<String, String?> = emptyMap(),
    ): Vendor {
        // 第 1 优先级：Build.MANUFACTURER
        byManufacturer(manufacturer.lowercase(Locale.ROOT))?.let { return it }
        // 第 2 优先级：Build.BRAND
        byBrand(brand.lowercase(Locale.ROOT))?.let { return it }
        // 第 3 优先级：系统属性
        bySystemProperty(systemProps)?.let { return it }
        // 兜底
        return Vendor.OTHER
    }

    /** 主动重置缓存（设置页切换语言/区域后调用，重新检测）。 */
    fun reset() {
        cached = null
    }

    private fun byManufacturer(m: String): Vendor? = when {
        m.contains("xiaomi") || m.contains("redmi") -> Vendor.XIAOMI
        m.contains("huawei") || m.contains("honor") -> Vendor.HUAWEI
        m.contains("oppo") || m.contains("realme") || m.contains("oneplus") -> Vendor.OPPO
        m.contains("vivo") || m.contains("iqoo") -> Vendor.VIVO
        m.contains("samsung") -> Vendor.SAMSUNG
        m.contains("meizu") -> Vendor.MEIZU
        else -> null
    }

    private fun byBrand(b: String): Vendor? = when {
        b.contains("xiaomi") || b.contains("redmi") || b.contains("poco") -> Vendor.XIAOMI
        b.contains("huawei") || b.contains("honor") -> Vendor.HUAWEI
        b.contains("oppo") || b.contains("realme") || b.contains("oneplus") -> Vendor.OPPO
        b.contains("vivo") || b.contains("iqoo") -> Vendor.VIVO
        b.contains("samsung") -> Vendor.SAMSUNG
        b.contains("meizu") -> Vendor.MEIZU
        else -> null
    }

    private fun bySystemProperty(props: Map<String, String?>): Vendor? {
        val miui = props["ro.miui.ui.version.name"]
        val emui = props["ro.build.version.emui"]
        val oppo = props["ro.build.version.opporom"]
        val vivo = props["ro.vivo.os.version"]
        val samsung = props["ro.samsung.fingerprint"]
        val meizu = props["ro.meizu.product.model"]
        return when {
            !miui.isNullOrBlank() && miui != "null" -> Vendor.XIAOMI
            !emui.isNullOrBlank() && emui != "null" -> Vendor.HUAWEI
            !oppo.isNullOrBlank() && oppo != "null" -> Vendor.OPPO
            !vivo.isNullOrBlank() && vivo != "null" -> Vendor.VIVO
            !samsung.isNullOrBlank() && samsung.contains("samsung", ignoreCase = true) -> Vendor.SAMSUNG
            !meizu.isNullOrBlank() && meizu != "null" -> Vendor.MEIZU
            else -> null
        }
    }

    /**
     * 读系统属性（反射 SystemProperties.get）。
     * Android 10+ 非 system 应用读 ro.* 会抛 SecurityException → 返回空 Map。
     */
    private fun readSystemProperties(): Map<String, String?> = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getMethod("get", String::class.java)
        listOf(
            "ro.miui.ui.version.name",
            "ro.build.version.emui",
            "ro.build.version.opporom",
            "ro.vivo.os.version",
            "ro.samsung.fingerprint",
            "ro.meizu.product.model",
        ).associateWith { key ->
            try {
                val v = method.invoke(clazz, key) as? String
                if (v.isNullOrBlank() || v == "null") null else v
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        Timber.tag(TAG).w(e, "System property read failed, fallback to OTHER")
        emptyMap()
    }

    companion object {
        private const val TAG = "VendorDetector"
    }
}
