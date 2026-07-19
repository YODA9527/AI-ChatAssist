package com.chatassist

import android.app.Application
import android.provider.Settings
import com.chatassist.data.local.security.SecureKeyStorage
import com.chatassist.data.permission.PermissionManager
import com.chatassist.domain.capture.CaptureStrategy
import com.chatassist.presentation.overlay.OverlayController
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application 入口。
 *
 * @HiltAndroidApp 触发 Hilt 编译期生成组件，所有 @AndroidEntryPoint 的 Activity/Fragment
 * 都能拿到全局依赖。
 */
@HiltAndroidApp
class ChatAssistApp : Application() {
    @Inject lateinit var secureKey: SecureKeyStorage

    // 捕获策略状态机：App 级生命周期，注册权限监听 + 主通道实时流收集
    @Inject lateinit var captureStrategy: CaptureStrategy
    // 权限状态 facade：App 启动后刷新一次（Timber 已就绪，便于确认无障碍等检测）
    @Inject lateinit var permissionManager: PermissionManager
    // 悬浮窗：权限已授予则随 App 启动挂载（v0.6）
    @Inject lateinit var overlayController: OverlayController

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // 触发 SecureKeyStorage 注入（暖启动 EncryptedSharedPreferences，避免首次使用卡顿）
        Timber.d("SecureKeyStorage ready, hasKey=${secureKey.getApiKey().isNotEmpty()}")
        // 启动捕获策略状态机（v0.4）
        captureStrategy.start()
        // 重新评估权限（Timber 已就绪，便于在日志确认无障碍等状态）
        permissionManager.refresh()
        // 悬浮窗随 App 启动挂载（权限不足时静默不显示，由首页按钮引导授权）
        if (Settings.canDrawOverlays(this)) {
            overlayController.show()
        }
    }
}
