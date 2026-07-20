package com.chatassist.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chatassist.domain.model.KeepAliveEvent
import com.chatassist.domain.usecase.KeepAliveManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * 后台保活兜底 Worker（v0.12：模块 10）。
 *
 * **职责**：WorkManager 周期任务，检查 [KeepAliveService] 是否还活着，死了就拉活。
 *
 * **触发策略**：
 * - 默认周期 15 分钟（Android WorkManager 最小周期）
 * - 通过 [KeepAliveManager.scheduleWorker] 入队，约束充电中 + WiFi 不强制
 *
 * **拉活逻辑**：
 * 1. 读 [KeepAliveManager.isRunning] 状态
 * 2. false → 调 [KeepAliveService.start] 重启服务
 * 3. 抛 [KeepAliveEvent.RevivedByWorker] 事件供 UI 观察
 *
 * **失败处理**：
 * - Worker 内部异常不抛（[Result.failure] 会让 WorkManager 退避重试，污染调度）
 * - 真正不可恢复时返 [Result.retry]（指数退避）
 */
@HiltWorker
class KeepAliveWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val keepAliveManager: KeepAliveManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (keepAliveManager.isRunning()) {
                Timber.tag(TAG).d("Service is running, skip revive")
                Result.success()
            } else {
                Timber.tag(TAG).w("Service is down, attempting revive")
                KeepAliveService.start(applicationContext)
                keepAliveManager.onRevivedByWorker(WORKER_NAME)
                // 给服务一点启动时间，下一次 Worker 周期再验证
                Result.success()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "KeepAliveWorker failed")
            // 失败时退避重试（WorkManager 默认指数退避）
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "KeepAliveWorker"
        const val WORKER_NAME = "chatassist_keep_alive_worker"
    }
}
