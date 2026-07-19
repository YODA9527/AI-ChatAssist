package com.chatassist.presentation.overlay

/**
 * 悬浮窗状态（文档 07 章契约）。
 */
sealed interface OverlayState {
    data object Collapsed : OverlayState
    data class Expanding(val progress: Float) : OverlayState
    data class Showing(
        val suggestions: List<String>,
        val isStreaming: Boolean,
    ) : OverlayState
    data class Error(val message: String) : OverlayState

    /**
     * 连续复制小气泡（v0.8，12.1.3）：复制第 currentIndex 条后，
     * 悬浮窗收起为「下一条」小气泡，点气泡复制 suggestions[currentIndex+1]，
     * 全部复制完自动回到 [Collapsed]。
     */
    data class HistoryBubble(
        val suggestions: List<String>,
        val currentIndex: Int,
    ) : OverlayState
}
