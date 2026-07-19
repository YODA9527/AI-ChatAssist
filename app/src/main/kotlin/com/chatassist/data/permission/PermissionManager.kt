package com.chatassist.data.permission

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.chatassist.domain.model.DenyReason
import com.chatassist.domain.model.MediaProjectionResult
import com.chatassist.domain.model.PermissionState
import com.chatassist.service.ChatAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 权限状态机 facade（4.1 启动与权限）。
 *
 * 统一对外暴露 4 个 StateFlow，但内部三种机制完全不同：
 * - 通知：运行时权限
 * - 悬浮窗：Settings.canDrawOverlays
 * - MediaProjection：由 Activity 拿到 token 后 setProjectionResult 写入（非运行时权限）
 * - 无障碍：Settings.Secure 里服务是否启用（只能跳设置页引导开启）
 *
 * 只维护状态、不发起请求——实际申请由 UI 层用 ActivityResultLauncher 完成，
 * 完成后调 [refresh] 重算。这样 Manager 保持可单测、与 Activity 生命周期解耦。
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val _notificationState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    private val _overlayState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    private val _projectionState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    private val _accessibilityState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    private val _allGranted = MutableStateFlow(false)
    private val _projectionResult = MutableStateFlow<MediaProjectionResult?>(null)

    val notificationState: StateFlow<PermissionState> = _notificationState.asStateFlow()
    val overlayState: StateFlow<PermissionState> = _overlayState.asStateFlow()
    val projectionState: StateFlow<PermissionState> = _projectionState.asStateFlow()
    val accessibilityState: StateFlow<PermissionState> = _accessibilityState.asStateFlow()
    val allGranted: StateFlow<Boolean> = _allGranted.asStateFlow()

    init { refresh() }

    /** 重算全部状态（onResume 时调用）。 */
    fun refresh() {
        evaluateNotification()
        evaluateOverlay()
        evaluateProjection()
        evaluateAccessibility()
        recomputeAllGranted()
    }

    /** Activity 拿到 MediaProjection 结果后写入。 */
    fun setProjectionResult(result: MediaProjectionResult) {
        _projectionResult.value = result
        evaluateProjection()
        recomputeAllGranted()
    }

    private fun evaluateNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            _notificationState.value = PermissionState.Granted(System.currentTimeMillis())
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            appContext, android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        _notificationState.value = if (granted) {
            PermissionState.Granted(System.currentTimeMillis())
        } else {
            PermissionState.Denied(DenyReason.USER_DENIED)
        }
    }

    private fun evaluateOverlay() {
        val granted = Settings.canDrawOverlays(appContext)
        _overlayState.value = if (granted) {
            PermissionState.Granted(System.currentTimeMillis())
        } else {
            PermissionState.Denied(DenyReason.SETTING_DISABLED)
        }
    }

    private fun evaluateProjection() {
        val result = _projectionResult.value
        _projectionState.value = if (result?.granted == true) {
            PermissionState.Granted(System.currentTimeMillis())
        } else {
            PermissionState.NotRequested
        }
    }

    private fun evaluateAccessibility() {
        val enabled = isAccessibilityServiceEnabled()
        Timber.d("evaluateAccessibility enabled=$enabled")
        _accessibilityState.value = if (enabled) {
            PermissionState.Granted(System.currentTimeMillis())
        } else {
            PermissionState.Denied(DenyReason.SETTING_DISABLED)
        }
    }

    private fun recomputeAllGranted() {
        _allGranted.value = _notificationState.value is PermissionState.Granted &&
            _overlayState.value is PermissionState.Granted &&
            _projectionState.value is PermissionState.Granted &&
            _accessibilityState.value is PermissionState.Granted
    }

    /**
     * 通过 [AccessibilityManager] 查询系统当前已启用的无障碍服务列表，
     * 匹配本 App 的 [ChatAccessibilityService] 组件。
     * 相比读取 [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES] 字符串，
     * 该方式在部分 ROM/模拟器（如 MuMu）上对普通应用可读、更稳定。
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val ourComponent = ComponentName(appContext, ChatAccessibilityService::class.java)
        // 主检测：AccessibilityManager 查询系统已绑定的无障碍服务（标准、跨 ROM；模拟器若不支持则取不到）
        val am = appContext.getSystemService(AccessibilityManager::class.java)
        if (am != null && am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { info ->
                    val si = info.resolveInfo.serviceInfo
                    ComponentName(si.packageName, si.name) == ourComponent
                }
        ) {
            return true
        }
        // 回退检测：读 ENABLED_ACCESSIBILITY_SERVICES 字符串（部分 ROM 对普通应用可读，MuMu 上常受限返回 null）
        val raw = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (raw != null && raw.split(':').any { it.equals(ourComponent.flattenToString(), ignoreCase = true) }) {
            return true
        }
        return false
    }

    companion object {
        // flat name 改由 ComponentName(appContext, ChatAccessibilityService::class.java).flattenToString() 动态生成，
        // 兼容 debug(.debug)/release 不同 applicationId。
    }
}
