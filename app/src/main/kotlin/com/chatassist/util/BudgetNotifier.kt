package com.chatassist.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.chatassist.domain.model.BudgetStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预算提醒通知（v2.2 §13.3.7）。
 *
 * 达 80% / 100% 时发本地通知。Android 13+ 需要 [Manifest.permission.POST_NOTIFICATIONS]，
 * 未授权时静默跳过（用量统计页始终有可见的预算进度条作为兜底）。
 * 写通知失败一律兜底忽略，绝不阻塞主链路。
 */
@Singleton
class BudgetNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun notify(status: BudgetStatus, usedYuan: Double, budgetYuan: Double) {
        if (status == BudgetStatus.OK) return
        val text = when (status) {
            BudgetStatus.NEAR_LIMIT ->
                "本月用量已达预算的 80%（已用 ${usedYuan.format()} 元 / 预算 ${budgetYuan.format()} 元），注意控制"
            BudgetStatus.EXCEEDED_WARN ->
                "本月用量已超预算（已用 ${usedYuan.format()} 元 / 预算 ${budgetYuan.format()} 元），建议检查或上调预算"
            BudgetStatus.EXCEEDED_BLOCK ->
                "本月预算已用完，生成已拦截。请在设置中调整预算或关闭超额拦截"
            BudgetStatus.OK -> return
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "预算提醒",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "用量达到预算阈值时提醒" }
            manager.createNotificationChannel(channel)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("ChatAssist 用量提醒")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            manager.notify(NOTIF_ID, notification)
        }.onFailure { Timber.w(it, "预算提醒通知发送失败（已忽略）") }
    }

    private fun Double.format(): String = "%.2f".format(this)

    companion object {
        private const val CHANNEL_ID = "budget_channel"
        private const val NOTIF_ID = 1003
    }
}
