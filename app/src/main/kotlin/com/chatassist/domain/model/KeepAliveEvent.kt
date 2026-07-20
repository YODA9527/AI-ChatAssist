package com.chatassist.domain.model

/**
 * 后台保活事件契约（v0.12：模块 10）。
 *
 * 由 [com.chatassist.domain.usecase.KeepAliveManager] 通过 StateFlow 暴露，
 * 供上层 UI（设置页、调试面板）观察保活状态变化。
 *
 * 事件类型：
 * - [ServiceStarted]：前台服务已拉起（通知可见）
 * - [ServiceStopped]：前台服务已停止（用户主动 / 系统杀进程）
 * - [VendorPushRequested]：用户点击「自启动引导」按钮、VendorGuide 构造了 Intent
 * - [RevivedByWorker]：WorkManager 周期任务发现服务被杀了，已尝试拉活
 */
sealed class KeepAliveEvent {
    /** 前台服务已拉起（startForeground 调用成功）。 */
    data object ServiceStarted : KeepAliveEvent()

    /** 前台服务已停止（onDestroy / stopSelf）。 */
    data object ServiceStopped : KeepAliveEvent()

    /** 用户请求自启动引导（VendorGuide 已构造跳转 Intent）。 */
    data class VendorPushRequested(val vendor: Vendor) : KeepAliveEvent()

    /** WorkManager 兜底拉活（服务被系统杀后通过 Worker 重启）。 */
    data class RevivedByWorker(val workerName: String) : KeepAliveEvent()
}
