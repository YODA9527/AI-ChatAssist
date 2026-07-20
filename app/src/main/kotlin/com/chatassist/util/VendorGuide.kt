package com.chatassist.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.chatassist.domain.model.Vendor
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 厂商自启动引导描述符（v0.12：模块 10 后台保活）。
 *
 * 纯数据类（packageName + className），便于单测。
 * 不依赖 Android Framework 的 Intent/Uri，避免 JVM 单测的 not-mocked 问题。
 */
data class VendorDescriptor(
    val packageName: String,
    val className: String,
) {
    fun toComponentName(): ComponentName = ComponentName(packageName, className)
    fun toIntent(): Intent = Intent().apply { component = toComponentName() }
}

/**
 * 厂商自启动引导（v0.12：模块 10 后台保活）。
 *
 * 给上层（设置页「保活引导」卡片）一个 Intent 跳转到对应 ROM 的自启动白名单页面。
 *
 * **核心原则**：
 * - **不直接 startActivity**（保持工具类纯净），仅构造 Intent，由 UI 触发
 * - **resolveActivity 校验**（用户可能用国际版/精简版 ROM，ComponentName 不可用时回退到通用 Intent）
 * - **找不到任何入口** 返回 null（UI 隐藏「去设置」按钮即可）
 *
 * **Intent 矩阵**（v0.12，按 v2 指南 §4.10 + 实测经验整理）：
 *
 * | 厂商 | 主 Intent | 回退 Intent |
 * |------|-----------|-------------|
 * | [Vendor.XIAOMI] | `com.miui.securitycenter/.permcenter.autostart.AutoStartManagementActivity` | 设置首页 |
 * | [Vendor.HUAWEI] | `com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity` | 华为手机管家首页 |
 * | [Vendor.OPPO] | `com.coloros.safecenter/.permission.startup.StartupAppListActivity` | 设置首页 |
 * | [Vendor.VIVO] | `com.iqoo.secure/.ui.phoneoptimize.AddWhiteListActivity` | i管家首页 |
 * | [Vendor.SAMSUNG] | `com.samsung.android.lool/.SettingsActivity` | 电池设置 |
 * | [Vendor.MEIZU] | `com.meizu.safe/.security.SHOW_APPSEC` | 权限管理 |
 * | [Vendor.OTHER] | `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` | - |
 *
 * 注：实际跳转路径以用户 ROM 版本为准（v0.13 真机回归时实测调整）。
 */
@Singleton
class VendorGuide @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * 构造跳转 Intent（不启动）。
     *
     * @param vendor 目标厂商（由调用方经 [VendorDetector] 检测后传入，便于单测）
     * @return 可启动的 Intent；返回 null 表示该 ROM 无可用入口（UI 隐藏按钮）
     */
    fun buildIntent(vendor: Vendor): Intent? {
        val primary = buildPrimaryDescriptor(vendor) ?: return null
        val primaryIntent = primary.toIntent()
        if (canResolve(primaryIntent)) {
            return primaryIntent
        }
        Timber.tag(TAG).w("Primary intent for $vendor not resolvable, trying fallback")
        val fallback = buildFallbackIntent(vendor) ?: return null
        return if (canResolve(fallback)) fallback else null
    }

    /**
     * 主入口描述符（厂商专属 ComponentName）。
     *
     * 纯函数版（接受 vendor 返回 VendorDescriptor?），便于单测。
     * 测试只覆盖 descriptor，Intent 构造在运行期生效。
     */
    fun buildPrimaryDescriptor(vendor: Vendor): VendorDescriptor? = when (vendor) {
        Vendor.XIAOMI -> VendorDescriptor(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        )
        Vendor.HUAWEI -> VendorDescriptor(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        )
        Vendor.OPPO -> VendorDescriptor(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        )
        Vendor.VIVO -> VendorDescriptor(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        )
        Vendor.SAMSUNG -> VendorDescriptor(
            "com.samsung.android.lool",
            "com.samsung.android.sm.ui.battery.BatteryActivity",
        )
        Vendor.MEIZU -> VendorDescriptor(
            "com.meizu.safe",
            "com.meizu.safe.security.SHOW_APPSEC",
        )
        Vendor.OTHER -> null
    }

    /** 回退 Intent（系统级设置）。 */
    private fun buildFallbackIntent(vendor: Vendor): Intent? = when (vendor) {
        // 厂商专属路径失败时，回退到设置首页（可让用户手工找自启动）
        Vendor.XIAOMI, Vendor.HUAWEI, Vendor.OPPO, Vendor.VIVO, Vendor.SAMSUNG, Vendor.MEIZU -> Intent(
            Settings.ACTION_SETTINGS,
        )
        Vendor.OTHER -> null
    }

    /** 检测 Intent 能否被系统解析（应用未安装 / ComponentName 错误时返回 false）。 */
    private fun canResolve(intent: Intent): Boolean {
        // 必须设置 FLAG_ACTIVITY_NEW_TASK（从非 Activity context 启动）
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.resolveActivity(context.packageManager) != null ||
                context.packageManager.queryIntentActivities(
                    Intent(intent).addFlags(flags),
                    0,
                ).isNotEmpty()
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(
                Intent(intent).addFlags(flags),
                0,
            ).isNotEmpty()
        }
    }

    companion object {
        private const val TAG = "VendorGuide"

        /**
         * 构造「应用详情」通用 Intent（终极兜底，Android 8+ 都可用）。
         *
         * 给设置页一个「去设置」按钮保底使用。
         */
        fun buildAppDetailsIntent(context: Context): Intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}
