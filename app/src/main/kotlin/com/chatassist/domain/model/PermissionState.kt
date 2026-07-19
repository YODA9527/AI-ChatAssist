package com.chatassist.domain.model

import android.content.Intent

/**
 * 权限状态机（4.1 启动与权限）。
 *
 * 四种权限走三种完全不同的获取机制，但对外统一成同一套状态：
 * - 通知 / 悬浮窗：运行时权限（checkSelfPermission / Settings.canDrawOverlays）
 * - MediaProjection：非权限，是 requestMediaProjection() 拿到的 token + 前台服务
 * - 无障碍：既非运行时权限也非 MediaProjection，是系统设置里服务是否启用
 */
sealed class PermissionState {
    data object NotRequested : PermissionState()
    data object Requesting : PermissionState()
    data class Granted(val grantedAt: Long) : PermissionState()
    data class Denied(val reason: DenyReason) : PermissionState()
    data class Revoked(val lastGrantedAt: Long) : PermissionState()
}

enum class DenyReason { USER_DENIED, SETTING_DISABLED, ROM_RESTRICTED }

data class MediaProjectionResult(
    val granted: Boolean,
    val resultCode: Int = 0,
    val data: Intent? = null,
)
