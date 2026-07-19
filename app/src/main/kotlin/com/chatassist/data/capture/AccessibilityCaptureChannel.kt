package com.chatassist.data.capture

import com.chatassist.domain.capture.CaptureChannel
import com.chatassist.domain.capture.CaptureResult
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.CaptureMode
import com.chatassist.service.ChatAccessibilityService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 无障碍主通道（B 的实现）。
 *
 * 持有最新消息的 SharedFlow（replay=1），由 [com.chatassist.service.ChatAccessibilityService]
 * 在窗口内容变化时 push（实时流已不再被消费，仅作兜底）。
 *
 * [service] 由无障碍服务在 onServiceConnected 反写，供 [captureOnce] 走
 * rootInActiveWindow 实时遍历当前窗口——这才是「点生成时抓一帧」的真实快照，
 * 而不是读实时流缓存。
 */
@Singleton
class AccessibilityCaptureChannel @Inject constructor() : CaptureChannel {
    private val _messages = MutableSharedFlow<List<ChatMessage>>(
        replay = 1, extraBufferCapacity = 64,
    )

    override val mode: CaptureMode = CaptureMode.TEXT_ONLY
    override val stream: Flow<List<ChatMessage>> = _messages.asSharedFlow()

    /** 由无障碍服务在 onServiceConnected 时反写，供 captureOnce 真实快照。 */
    @Volatile var service: ChatAccessibilityService? = null

    /** 由无障碍服务调用，推送最新一帧消息（实时流保留作兜底，但已不再驱动生成）。 */
    fun push(messages: List<ChatMessage>) {
        _messages.tryEmit(messages)
    }

    override suspend fun captureOnce(): Result<CaptureResult> = runCatching {
        val svc = service
        val msgs = svc?.snapshotMessages().orEmpty()
        val pkg = svc?.lastPackage.orEmpty()
        CaptureResult(mode = mode, messages = msgs, targetPackage = pkg)
    }
}
