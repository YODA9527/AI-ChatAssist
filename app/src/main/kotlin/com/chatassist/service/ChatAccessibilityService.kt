package com.chatassist.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.chatassist.data.capture.AccessibilityCaptureChannel
import com.chatassist.data.capture.AccessibilityChatNode
import com.chatassist.data.capture.extractMessages
import com.chatassist.domain.model.ChatMessage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * 无障碍主通道服务（B 的入口）。
 *
 * 监听 [AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED] / [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED]，
 * 取当前窗口根节点 → 经 [extractMessages]（纯函数遍历节点树）→ 推给 [AccessibilityCaptureChannel]。
 * 由系统在无障碍服务启用后拉起，不依赖 App 进程存活，是 Phase 1 主捕获通道。
 *
 * 新增能力（v0.7）：
 * - [snapshotMessages]：供 [com.chatassist.data.capture.AccessibilityCaptureChannel.captureOnce]
 *   走 rootInActiveWindow 实时遍历当前窗口，实现「点生成时抓一帧」，而非读实时流缓存。
 * - [fillFocusedInput]：供悬浮窗「填充」把回复文本写入当前聚焦的输入框（跨 App 无障碍 setText）。
 */
@AndroidEntryPoint
class ChatAccessibilityService : AccessibilityService() {

    @Inject lateinit var channel: AccessibilityCaptureChannel

    /** 最近一次捕获到的前台包名（供 CaptureResult.targetPackage 落历史记录）。 */
    @Volatile var lastPackage: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        channel.service = this
        Timber.d("ChatAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }
        val root = rootInActiveWindow ?: return
        try {
            val pkg = event.packageName?.toString().orEmpty()
            lastPackage = pkg
            val messages = extractMessages(AccessibilityChatNode(root), pkg)
            if (messages.isNotEmpty()) channel.push(messages)
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() {
        // 服务被系统中断时无需处理；捕获恢复由策略机监听无障碍状态自动接管
    }

    override fun onDestroy() {
        if (channel.service === this) channel.service = null
        instance = null
        super.onDestroy()
    }

    /**
     * 抓一次当前前台窗口的聊天气泡（供 captureOnce 使用，不依赖实时流）。
     * 返回空集表示当前无可见聊天内容或无障碍拿不到根节点。
     */
    fun snapshotMessages(): List<ChatMessage> {
        val root = rootInActiveWindow ?: return emptyList()
        return try {
            val pkg = root.packageName?.toString().orEmpty()
            lastPackage = pkg
            extractMessages(AccessibilityChatNode(root), pkg)
        } finally {
            root.recycle()
        }
    }

    /**
     * 把文本填入当前聚焦的输入框（跨 App 无障碍 setText）。
     * 返回是否成功（无焦点输入框或权限不足时为 false）。
     */
    fun fillFocusedInput(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text,
                )
            }
            val ok = focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            focus.recycle()
            ok
        } finally {
            root.recycle()
        }
    }

    /**
     * 自动粘贴（v0.8，12.1.4）：仅精准模式 / 无障碍可用时生效。
     * 把文本写入系统剪贴板后，对当前聚焦的 EditText 执行 [AccessibilityNodeInfo.ACTION_PASTE]。
     * 目标输入框若为自绘控件（无障碍找不到焦点）则失败返回 false。
     */
    fun autoPaste(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("reply", text))
            val ok = focus.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            focus.recycle()
            ok
        } finally {
            root.recycle()
        }
    }

    companion object {
        /** 当前存活的服务实例（由系统实例化，onServiceConnected 时赋值）。 */
        @Volatile var instance: ChatAccessibilityService? = null
    }
}
