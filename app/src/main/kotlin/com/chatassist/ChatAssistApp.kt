package com.chatassist

import android.app.Application
import android.provider.Settings
import androidx.work.Configuration
import com.chatassist.data.local.security.SecureKeyStorage
import com.chatassist.data.permission.PermissionManager
import com.chatassist.domain.capture.CaptureStrategy
import com.chatassist.domain.error.ErrorPresenter
import com.chatassist.domain.usecase.KeepAliveManager
import com.chatassist.presentation.overlay.OverlayController
import com.chatassist.util.GlobalErrorHandler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application 入口。
 *
 * @HiltAndroidApp 触发 Hilt 编译期生成组件，所有 @AndroidEntryPoint 的 Activity/Fragment
 * 都能拿到全局依赖。
 *
 * v0.11.0 起接入 [GlobalErrorHandler]：所有未捕获异常（线程 + 协程）统一走 ErrorClassifier →
 * CrashLogger → ErrorPresenter，UI 回调通过 [GlobalErrorHandler.onError] 注入。
 *
 * v0.12 起实现 [Configuration.Provider]：Hilt 注入 WorkerFactory 给 WorkManager，
 * 配合 manifest 移除默认初始化器实现 on-demand 初始化。
 */
@HiltAndroidApp
class ChatAssistApp : Application(), Configuration.Provider {

    /** Hilt 注入的 WorkManager 配置（hilt-work 提供 HiltWorkerFactory）。 */
    @Inject lateinit var hiltWorkManagerConfiguration: Configuration

    @Inject lateinit var secureKey: SecureKeyStorage

    // 捕获策略状态机：App 级生命周期，注册权限监听 + 主通道实时流收集
    @Inject lateinit var captureStrategy: CaptureStrategy
    // 权限状态 facade：App 启动后刷新一次（Timber 已就绪，便于确认无障碍等检测）
    @Inject lateinit var permissionManager: PermissionManager
    // 悬浮窗：权限已授予则随 App 启动挂载（v0.6）
    @Inject lateinit var overlayController: OverlayController
    // 全局错误处理（v0.11.0 新增）：默认空回调，MainViewModel 启动时注入 onError
    @Inject lateinit var globalErrorHandler: GlobalErrorHandler
    // 错误展示器（v0.11.0 新增）：供 GlobalErrorHandler + UI 层使用
    @Inject lateinit var errorPresenter: ErrorPresenter
    // 后台保活协调器（v0.12 新增）：启动前台服务 + WorkManager 周期任务
    @Inject lateinit var keepAliveManager: KeepAliveManager

    override val workManagerConfiguration: Configuration
        get() = hiltWorkManagerConfiguration

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // v0.11.0：最早期注册全局错误处理（Timber 已 plant）
        globalErrorHandler.install()
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
        // v0.13：删除 ChatAssistApp.onCreate 的 keepAliveManager.start() 调用
        // 原因：Android 12+ 限制 cached app (uidState=CEM) 不能 startForegroundService（v0.12 mumu 实测）
        // 现改在 MainActivity.onCreate 调（用户进 App 时 uidState=FG，能正常拉 FGS）
    }
}
