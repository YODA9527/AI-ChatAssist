package com.chatassist.domain.capture

import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import kotlinx.coroutines.flow.StateFlow
import kotlin.Result

/**
 * 捕获策略契约（D 的 domain 接口）。
 *
 * 把「当前走哪条通道 / 实时消息流 / 启停 / 按需拉帧」收敛成 VM/UseCase 可依赖的抽象，
 * 使 UI 层不反向依赖 data 层具体 Manager/Channel（满足 Clean 分层铁律）。
 * 实现见 [com.chatassist.data.capture.CaptureStrategyManager]。
 */
interface CaptureStrategy {
    /** 当前实际通道（TEXT_ONLY / OCR_ONLY）。 */
    val currentMode: StateFlow<CaptureMode>

    /** 实时提取到的聊天消息。 */
    val latestMessages: StateFlow<List<ChatMessage>>

    /** 启动策略机（监听权限、驱动当前通道）。 */
    fun start()

    /** 停止并释放。 */
    fun stop()

    /** 按需拉取一帧（走当前通道）。 */
    suspend fun captureOnce(): Result<CaptureResult>
}
