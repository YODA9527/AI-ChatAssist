package com.chatassist.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chatassist.data.capture.ScreenCaptureManager
import com.chatassist.data.permission.PermissionManager
import com.chatassist.domain.model.MediaProjectionResult
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * 截图前台服务（C 的会话持有者）。
 *
 * MediaProjection 必须挂在一个活的前台服务上（Android 10+ 后台限制，14 必须声明
 * foregroundServiceType="mediaProjection"）。Activity 经 MediaProjection 授权后，
 * 把 resultCode/data 通过 Intent 启动本服务，本服务持 MediaProjection + 驱动截图管线。
 */
@AndroidEntryPoint
class CaptureForegroundService : Service() {

    @Inject lateinit var screenCaptureManager: ScreenCaptureManager
    @Inject lateinit var permissionManager: PermissionManager

    private var mediaProjection: MediaProjection? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATA)
        }
        val granted = resultCode != 0 && data != null
        permissionManager.setProjectionResult(MediaProjectionResult(granted, resultCode, data))
        if (!granted) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification())
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(resultCode, data!!)
        mediaProjection?.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    Timber.d("MediaProjection stopped by system")
                    permissionManager.setProjectionResult(MediaProjectionResult(false))
                    stopSelf()
                }
            },
            null,
        )
        screenCaptureManager.startCapture(mediaProjection!!)
        return START_STICKY
    }

    override fun onDestroy() {
        screenCaptureManager.stopCapture()
        mediaProjection?.stop()
        mediaProjection = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, "屏幕识别", NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "保持聊天识别服务运行" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("聊天助手识别中")
            .setContentText("正在通过无障碍 / 截图提取聊天内容")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        private const val CHANNEL_ID = "capture_channel"
        private const val NOTIF_ID = 1001
    }
}
