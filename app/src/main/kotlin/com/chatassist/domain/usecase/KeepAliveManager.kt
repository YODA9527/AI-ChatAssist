package com.chatassist.domain.usecase

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.chatassist.domain.model.KeepAliveEvent
import com.chatassist.domain.model.Vendor
import com.chatassist.service.KeepAliveService
import com.chatassist.service.KeepAliveWorker
import com.chatassist.util.VendorGuide
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 后台保活协调器（v0.12：模块 10 协调层）。
 *
 * **职责**：
 * 1. 启动 / 停止 [KeepAliveService] 前台服务
 * 2. 注册 [KeepAliveWorker] 周期任务（兜底拉活）
 * 3. 提供自启动引导 Intent（委托 [VendorGuide]）
 * 4. 暴露状态 / 事件流供 UI 观察
 *
 * **状态机**（state）：
 * - [KeepAliveState.Stopped] — 服务未启动
 * - [KeepAliveState.Running] — 服务正常运行
 * - [KeepAliveState.Reviving] — Worker 检测到服务被杀了、正在拉活
 *
 * **事件流**（events）：见 [KeepAliveEvent]（服务启停 + 拉活 + 引导点击）
 *
 * **入口**：由 [com.chatassist.ChatAssistApp.onCreate] 启动（start 后异步生效）。
 */
@Singleton
class KeepAliveManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vendorGuide: VendorGuide,
) {
    private val _state = MutableStateFlow(KeepAliveState.Stopped)
    val state: StateFlow<KeepAliveState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<KeepAliveEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<KeepAliveEvent> = _events.asSharedFlow()

    /** 服务是否运行中（Running 或正在 Reviving 都算）。 */
    fun isRunning(): Boolean = _state.value != KeepAliveState.Stopped

    /**
     * 启动保活：前台服务 + Worker 周期任务。
     *
     * 幂等（多次调用仅首次生效）。
     */
    fun start() {
        if (isRunning()) {
            Timber.tag(TAG).d("Already running, skip start")
            return
        }
        Timber.tag(TAG).i("Starting keep-alive (service + worker)")
        runCatching { KeepAliveService.start(context) }
            .onFailure { Timber.tag(TAG).w(it, "Start service call failed (expected in test)") }
        runCatching { scheduleWorker() }
            .onFailure { Timber.tag(TAG).w(it, "Schedule worker failed (expected in test)") }
    }

    /**
     * 停止保活：前台服务 + 取消 Worker。
     */
    fun stop() {
        Timber.tag(TAG).i("Stopping keep-alive")
        runCatching { KeepAliveService.stop(context) }
            .onFailure { Timber.tag(TAG).w(it, "Stop service call failed (expected in test)") }
        cancelWorker()
        // Service.onDestroy 会回调 onServiceStopped() 更新状态；此处兜底
        _state.value = KeepAliveState.Stopped
    }

    /**
     * 注册 WorkManager 周期任务（15min 最小周期）。
     */
    private fun scheduleWorker() {
        val constraints = Constraints.Builder().build()
        val request = PeriodicWorkRequestBuilder<KeepAliveWorker>(
            WORKER_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            KeepAliveWorker.WORKER_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // 已存在则更新（应用重启时）
            request,
        )
    }

    /**
     * 取消 Worker 周期任务。
     */
    private fun cancelWorker() {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(KeepAliveWorker.WORKER_NAME)
        }.onFailure {
            Timber.tag(TAG).w(it, "Cancel worker failed (WorkManager 未初始化或测试环境)")
        }
    }

    /**
     * 构造自启动引导 Intent（供 UI 调用）。
     *
     * 同时 emit [KeepAliveEvent.VendorPushRequested] 事件（埋点用）。
     */
    fun requestVendorGuide(vendor: Vendor): android.content.Intent? {
        val intent = vendorGuide.buildIntent(vendor)
        if (intent != null) {
            _events.tryEmit(KeepAliveEvent.VendorPushRequested(vendor))
        }
        return intent
    }

    // --- 内部回调（由 Service / Worker 调用） ---

    /** Service.onCreate 回调。 */
    internal fun onServiceStarted() {
        _state.value = KeepAliveState.Running
        _events.tryEmit(KeepAliveEvent.ServiceStarted)
    }

    /** Service.onDestroy 回调。 */
    internal fun onServiceStopped() {
        _state.value = KeepAliveState.Stopped
        _events.tryEmit(KeepAliveEvent.ServiceStopped)
    }

    /** Worker 检测到服务被杀了、已拉活。 */
    internal fun onRevivedByWorker(workerName: String) {
        _state.value = KeepAliveState.Reviving
        _events.tryEmit(KeepAliveEvent.RevivedByWorker(workerName))
    }

    /** 保活状态（UI 观察用）。 */
    enum class KeepAliveState {
        Stopped,
        Running,
        Reviving,
    }

    companion object {
        private const val TAG = "KeepAliveManager"
        private const val WORKER_INTERVAL_MINUTES = 15L
    }
}
