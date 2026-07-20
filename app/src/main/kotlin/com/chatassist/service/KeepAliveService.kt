package com.chatassist.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chatassist.domain.usecase.KeepAliveManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * 后台保活前台服务（v0.12：模块 10）。
 *
 * **职责**：在 App 处于后台时保持进程不被回收，确保用户从悬浮窗触发识别时无障碍 + 截图服务可用。
 *
 * **Android 14+ 适配**：
 * - `foregroundServiceType="specialUse"`（保活不属于 mediaProjection / locationSync 等已定义类型）
 * - 必须在 manifest 加 `FOREGROUND_SERVICE_SPECIAL_USE` 权限
 * - 必须在 startForeground 时传 type（[ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE]）
 *
 * **生命周期**：
 * - `onCreate` → `startForeground(NOTIF_ID, buildNotification(), type)` → emit `ServiceStarted`
 * - `onStartCommand` → `START_STICKY`（被系统杀后会重启）
 * - `onDestroy` → emit `ServiceStopped`
 *
 * **与 WorkManager 协作**：
 * - Worker 周期 15min 检查服务是否存活
 * - 死了就 `startService(ACTION_START)` 拉活
 * - 拉活成功通过 `RevivedByWorker` 事件上抛
 */
@AndroidEntryPoint
class KeepAliveService : Service() {

    @Inject lateinit var keepAliveManager: KeepAliveManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        keepAliveManager.onServiceStarted()
        Timber.tag(TAG).i("KeepAliveService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY：被系统杀后系统会尝试重启服务（Intent 为 null）
        return START_STICKY
    }

    override fun onDestroy() {
        keepAliveManager.onServiceStopped()
        Timber.tag(TAG).i("KeepAliveService stopped")
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 必须传 type
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "后台保活",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "保持 ChatAssist 后台可用，便于快速触发识别" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ChatAssist 后台保活中")
            .setContentText("保持识别服务可用，随时可触发回复建议")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val TAG = "KeepAliveService"
        private const val CHANNEL_ID = "keep_alive_channel"
        private const val NOTIF_ID = 1002

        const val ACTION_START = "com.chatassist.action.KEEP_ALIVE_START"
        const val ACTION_STOP = "com.chatassist.action.KEEP_ALIVE_STOP"

        /** 启动保活服务（幂等，已运行则忽略）。 */
        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** 停止保活服务。 */
        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }
}
