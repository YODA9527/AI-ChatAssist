package com.chatassist.util

import kotlinx.coroutines.CoroutineExceptionHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 协程异常处理工厂（v0.11.0：模块 12 A 项）。
 *
 * 唯一职责：提供 [CoroutineExceptionHandler] 工厂，让 ViewModel/UseCase 的
 * `viewModelScope.launch(handler) { ... }` 统一走 [GlobalErrorHandler]。
 *
 * 用法：
 * ```kotlin
 * @Inject lateinit var coroutineErrorHandlerFactory: CoroutineErrorHandler
 *
 * viewModelScope.launch(coroutineErrorHandlerFactory()) {
 *     // 任何异常都被全局处理（落盘 + UI 回调）
 * }
 * ```
 */
@Singleton
class CoroutineErrorHandler @Inject constructor(
    private val globalErrorHandler: GlobalErrorHandler,
) {
    operator fun invoke(): CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        globalErrorHandler.handle(throwable)
    }
}
