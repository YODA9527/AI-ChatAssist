package com.chatassist.domain.usecase

import com.chatassist.data.capture.CaptureStrategyManager
import com.chatassist.domain.capture.CaptureResult
import com.chatassist.domain.capture.CaptureStrategy
import com.chatassist.domain.model.ChatMessage
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 捕获用例（E 的 Clean 边界）。
 *
 * ViewModel 只依赖本用例，不直接碰 Manager/Channel；保证 UI 层不反向依赖 data。
 */
class CaptureChatUseCase @Inject constructor(
    private val strategy: CaptureStrategyManager,
) {
    /** 实时消息流。 */
    operator fun invoke(): StateFlow<List<ChatMessage>> = strategy.latestMessages

    /** 按需拉取一帧。 */
    suspend fun captureOnce(): Result<CaptureResult> = strategy.captureOnce()
}
