package com.chatassist.data.capture

import com.chatassist.domain.capture.CaptureChannel
import com.chatassist.domain.capture.CaptureResult
import com.chatassist.domain.capture.CaptureStrategy
import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.PermissionState
import com.chatassist.data.permission.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

/**
 * 捕获策略状态机（D）。
 *
 * 监听权限状态，在 TEXT_ONLY ↔ OCR_ONLY 间降级/恢复：
 * - 无障碍启用 → TEXT_ONLY（主通道，无障碍实时读屏）
 * - 无障碍被撤销 + MediaProjection 已授权 → OCR_ONLY（v0.5 真实：MediaProjection 截图 + ML Kit 中文识别）
 * - 两者皆无 → 停在 TEXT_ONLY（无源， latestMessages 为空）
 *
 * 依赖 [CaptureChannel] 抽象，不耦合 B/C 具体实现；OCR 通道升级为真实实现后状态机逻辑不变，
 * 因此仍可纯单测降级/恢复全路径（用 fake channel 即可）。
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureStrategyManager @Inject constructor(
    private val accessibilityChannel: AccessibilityCaptureChannel,
    private val ocrChannel: OcrCaptureChannel,
    private val permissionManager: PermissionManager,
) : CaptureStrategy {

    private var scope = newScope()
    private val _currentMode = MutableStateFlow(CaptureMode.TEXT_ONLY)
    override val currentMode: StateFlow<CaptureMode> = _currentMode.asStateFlow()
    private val _latestMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val latestMessages: StateFlow<List<ChatMessage>> = _latestMessages.asStateFlow()
    private var started = false

    override fun start() {
        if (started) return
        started = true
        recomputeMode()
        // 仅跟踪权限 → 切换主/兜底通道；不再订阅 channel.stream，
        // 因此实时提取停止，latestMessages 保持空。生成时改走 captureOnce() 抓一帧。
        permissionManager.accessibilityState
            .onEach { recomputeMode() }
            .launchIn(scope)
        permissionManager.projectionState
            .onEach { recomputeMode() }
            .launchIn(scope)
    }

    override fun stop() {
        if (!started) return
        started = false
        scope.cancel()
        scope = newScope()
    }

    private fun recomputeMode() {
        val acc = permissionManager.accessibilityState.value is PermissionState.Granted
        val proj = permissionManager.projectionState.value is PermissionState.Granted
        _currentMode.value = when {
            acc && proj -> CaptureMode.HYBRID
            acc -> CaptureMode.TEXT_ONLY
            proj -> CaptureMode.OCR_ONLY
            else -> CaptureMode.TEXT_ONLY
        }
    }

    private fun activeChannel(mode: CaptureMode): CaptureChannel =
        if (mode == CaptureMode.OCR_ONLY) ocrChannel else accessibilityChannel

    /**
     * 双通道融合：按内容去重，OCR 先入、无障碍后入（无障碍文本更准，冲突时覆盖）。
     */
    private fun mergeMessages(
        a11y: List<ChatMessage>,
        ocr: List<ChatMessage>,
    ): List<ChatMessage> {
        val map = LinkedHashMap<String, ChatMessage>()
        ocr.forEach { if (it.content.isNotBlank()) map.putIfAbsent(it.content, it) }
        a11y.forEach { if (it.content.isNotBlank()) map.putIfAbsent(it.content, it) }
        return map.values.toList()
    }

    override suspend fun captureOnce(): Result<CaptureResult> {
        return when (val mode = currentMode.value) {
            CaptureMode.HYBRID -> {
                val a = accessibilityChannel.captureOnce().getOrElse { return Result.failure(it) }
                val o = ocrChannel.captureOnce().getOrElse { return Result.failure(it) }
                Result.success(
                    CaptureResult(
                        mode = mode,
                        messages = mergeMessages(a.messages, o.messages),
                    ),
                )
            }
            else -> activeChannel(mode).captureOnce()
        }
    }

    private companion object {
        fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
