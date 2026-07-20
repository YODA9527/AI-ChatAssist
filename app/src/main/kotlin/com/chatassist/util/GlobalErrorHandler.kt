package com.chatassist.util

import com.chatassist.domain.error.ErrorClassifier
import com.chatassist.domain.error.ErrorPresenter
import com.chatassist.domain.error.ErrorUiModel
import com.chatassist.domain.model.AppError
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局错误处理（v0.11.0：模块 12 A 项）。
 *
 * 唯一入口：所有未捕获异常（线程 + 协程）走这里：
 * 1. [ErrorClassifier] 分类 → [AppError]
 * 2. [CrashLogger] 落本地日志（脱敏）
 * 3. [ErrorPresenter] 转 [ErrorUiModel]（给 UI 层用）
 * 4. UI 回调 [onError] 触发 Snackbar/Dialog
 *
 * 集成：
 * - [com.chatassist.ChatAssistApp.onCreate] 调 [install] 注册 defaultUncaughtExceptionHandler
 * - 协程用 [com.chatassist.util.CoroutineErrorHandler] 调 [handle]
 * - UI 用 [onError] 接收 [ErrorUiModel]（ChatAssistApp 持有回调，MainViewModel 启动时注入）
 */
@Singleton
class GlobalErrorHandler @Inject constructor(
    private val crashLogger: CrashLogger,
    private val errorClassifier: ErrorClassifier,
    private val errorPresenter: ErrorPresenter,
) {

    /** UI 回调：App.onCreate 注入，UI 层弹 Snackbar/Dialog。 */
    var onError: (ErrorUiModel) -> Unit = {}

    /**
     * 注册全局线程异常处理（应用启动时调一次）。
     *
     * 链式调用：保留旧 handler（可能是 Android 系统或 Hilt/Jetpack 注入的处理器），
     * 本处理器先落本地日志、再让旧 handler 决定是否 kill 进程。
     */
    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { handle(throwable) }
                .onFailure { Timber.tag(TAG).e(it, "GlobalErrorHandler failed") }
            previous?.uncaughtException(thread, throwable)
        }
        Timber.tag(TAG).d("GlobalErrorHandler installed")
    }

    /**
     * 统一入口：分类 + 落盘 + 转 UI model + 触发回调。
     * 供 [CoroutineErrorHandler] 调用。
     */
    fun handle(throwable: Throwable): ErrorUiModel {
        val appError = errorClassifier.classify(throwable)
        // 致命错误必落盘，可恢复错误按需
        when (appError) {
            is AppError.Fatal -> crashLogger.logAppError(appError)
            is AppError.Recoverable -> {
                // 仅 retryable 的可恢复错误落盘（避免日志被无意义 NETWORK 刷屏）
                if (appError.retryable) crashLogger.logAppError(appError)
            }
        }
        val ui = errorPresenter.present(appError)
        runCatching { onError(ui) }.onFailure { Timber.tag(TAG).e(it, "onError callback failed") }
        return ui
    }

    companion object {
        private const val TAG = "GlobalErrorHandler"
    }
}
