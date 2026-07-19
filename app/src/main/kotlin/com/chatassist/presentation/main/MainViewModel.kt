package com.chatassist.presentation.main

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatassist.data.permission.PermissionManager
import com.chatassist.domain.capture.CaptureResult
import com.chatassist.domain.capture.CaptureStrategy
import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.PermissionState
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.usecase.CaptureChatUseCase
import com.chatassist.domain.usecase.ReplySuggestionUseCase
import com.chatassist.presentation.overlay.OverlayController
import com.chatassist.presentation.overlay.OverlayState
import com.chatassist.service.ChatAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页 ViewModel（E 收口）。
 *
 * 暴露：配置摘要、四权限状态、实时捕获消息、当前捕获模式、回复建议状态、悬浮窗状态。
 * 不直接依赖 data 层 Manager——权限走 [PermissionManager]（同层 facade），捕获走 [CaptureChatUseCase]，
 * 回复走 [ReplySuggestionUseCase]，悬浮窗走 [OverlayController]。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val configRepo: ConfigRepository,
    private val permissionManager: PermissionManager,
    private val captureUseCase: CaptureChatUseCase,
    private val captureStrategy: CaptureStrategy,
    private val replyUseCase: ReplySuggestionUseCase,
    private val overlay: OverlayController,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val state: StateFlow<MainUiState> = configRepo.observeAll()
        .map { cfg ->
            MainUiState(
                isApiConfigured = cfg.apiKey.isNotBlank(),
                apiUrl = cfg.apiUrl,
                model = cfg.model,
                privacyMode = cfg.privacyMode,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState(),
        )

    val permissionSummary: StateFlow<PermissionSummary> =
        combine(
            permissionManager.notificationState,
            permissionManager.overlayState,
            permissionManager.projectionState,
            permissionManager.accessibilityState,
        ) { notif, overlay, proj, a11y ->
            PermissionSummary(notif, overlay, proj, a11y)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PermissionSummary(),
        )

    val capturedMessages: StateFlow<List<ChatMessage>> = captureUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val captureMode: StateFlow<CaptureMode> = captureStrategy.currentMode

    val replyState: StateFlow<com.chatassist.domain.usecase.ReplyUiState> = replyUseCase.state
    val overlayState: StateFlow<OverlayState> = overlay.state

    init {
        // 悬浮窗复制走系统剪贴板
        overlay.copyHandler = { text, _ -> copySuggestion(text) }
        // 悬浮窗「生成」按钮触发生成回复建议（按需抓屏，不再读实时流）
        overlay.generateHandler = { generateReply() }
        // 悬浮窗点一条回复 → 填充输入框（复制兜底 + 无障碍 setText）
        overlay.fillHandler = { fillInput(it) }
        // 把回复状态镜像到悬浮窗（权限足够时悬浮窗会显示）
        viewModelScope.launch {
            replyUseCase.state.collect { s ->
                when (s) {
                    is com.chatassist.domain.usecase.ReplyUiState.Streaming -> overlay.showStreaming(s.text)
                    is com.chatassist.domain.usecase.ReplyUiState.Suggestions -> overlay.showSuggestions(s.items)
                    is com.chatassist.domain.usecase.ReplyUiState.Error -> overlay.showError(s.message)
                    com.chatassist.domain.usecase.ReplyUiState.Idle -> {}
                }
            }
        }
    }

    /** 生成回复建议：点生成时按需抓一帧屏幕，再喂给模型（不再依赖实时流）。 */
    fun generateReply() {
        viewModelScope.launch {
            val res = captureUseCase.captureOnce()
            if (res.isFailure) {
                overlay.showError("屏幕提取失败：${res.exceptionOrNull()?.message}")
                return@launch
            }
            val capture: CaptureResult = res.getOrThrow()
            val msgs = capture.messages
            if (msgs.isEmpty()) {
                val hint = if (permissionManager.projectionState.value !is PermissionState.Granted) {
                    "无障碍读不到图片里的字。请在首页「设置」开启「屏幕录制（兜底）」后重试（用于识别聊天截图）"
                } else {
                    capture.note ?: "未能从屏幕读到对话，请确认聊天界面在前台"
                }
                overlay.showError(hint)
                return@launch
            }
            replyUseCase.generate(msgs, capture.targetPackage)
        }
    }

    /** 复制一条建议到系统剪贴板。 */
    fun copySuggestion(text: String) {
        val cm = appContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("reply", text))
    }

    /**
     * 点击一条回复：填充到当前聚焦的输入框。
     * 先复制到剪贴板兜底，再尝试无障碍 setText 直接填入；用 Toast 告知结果。
     */
    /**
     * 点击一条回复：填充到当前聚焦的输入框。
     * 先复制到剪贴板兜底；autoPaste 开启且无障碍可用时走自动粘贴（ACTION_PASTE），
     * 否则走无障碍 setText 直接填入。
     */
    fun fillInput(text: String) {
        viewModelScope.launch {
            val cfg = configRepo.snapshot()
            copySuggestion(text)
            val svc = ChatAccessibilityService.instance
            val pasted = if (cfg.autoPaste && svc != null) {
                svc.autoPaste(text)
            } else {
                svc?.fillFocusedInput(text) == true
            }
            overlay.toast(if (pasted) "已填入输入框" else "已复制，请到聊天框粘贴")
        }
    }

    /** 悬浮窗开/关切换。 */
    fun toggleOverlay() {
        if (overlay.isShowing()) overlay.hide() else overlay.show()
    }

    /** 申请/设置变更后由 UI 调用，重算权限状态。 */
    fun refreshPermissions() = permissionManager.refresh()
}

data class MainUiState(
    val isApiConfigured: Boolean = false,
    val apiUrl: String = "",
    val model: String = "",
    val privacyMode: Boolean = false,
)

data class PermissionSummary(
    val notification: PermissionState = PermissionState.NotRequested,
    val overlay: PermissionState = PermissionState.NotRequested,
    val projection: PermissionState = PermissionState.NotRequested,
    val accessibility: PermissionState = PermissionState.NotRequested,
)
