package com.chatassist.domain.capture

import com.chatassist.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 捕获通道抽象（D 依赖的接口，B/C 各自实现）。
 *
 * 把无障碍通道与 OCR 通道统一成同一契约，使 [CaptureStrategyManager] 可纯状态机驱动、
 * 不耦合具体实现，OCR 通道在 v0.4 用桩替代也不影响降级/恢复逻辑。
 */
interface CaptureChannel {
    /** 本通道模式。 */
    val mode: com.chatassist.domain.model.CaptureMode

    /** 持续流出的最新消息（主通道实时推送；兜底通道可为 emptyFlow）。 */
    val stream: Flow<List<ChatMessage>>

    /** 按需拉取一帧。 */
    suspend fun captureOnce(): Result<CaptureResult>
}
