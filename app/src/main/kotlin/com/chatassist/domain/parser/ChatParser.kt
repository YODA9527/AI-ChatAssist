package com.chatassist.domain.parser

import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import java.util.Calendar
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天内容解析器（v0.6 落地）。
 *
 * 把一条「原始文本 + 几何信息」解析成结构化 [ChatMessage]：
 * - [ChatMessage.isSelf]：按文本水平中心位置判定（微信/多数 IM 右侧=自己）。
 * - [ChatMessage.sender]：「我 / 对方 / 系统」。
 * - [ChatMessage.timestamp]：从文本里抠出 HH:MM（支持「上午/下午/晚上」前缀），落成当日毫秒时间戳。
 * - 系统消息（时间戳行、「撤回」「已添加」等）单独标记为「系统」，不参与回复建议。
 *
 * 纯 Kotlin（不持有 Android 框架状态），因此可 JUnit 单测 + 双通道（无障碍/OCR）复用同一套逻辑。
 * 文档 04 章的 PositionClassifier / AppPresetProvider 简化版：v0.6 先做位置分类 + 系统/时间识别，
 * 颜色聚类、多 App 预设库留 v0.6+ 迭代。
 */
@Singleton
class ChatParser @Inject constructor() {

    /**
     * @param text       原始文本
     * @param centerX    文本水平中心像素（无几何信息时传 -1）
     * @param screenWidth 屏幕宽度像素（无几何信息时传 0/负数）
     * @param pkg        目标 App 包名（v0.6 暂未区分预设，预留）
     * @param source     通道来源
     * @param index      序号（用于生成稳定 id）
     */
    fun parse(
        text: String,
        centerX: Int,
        screenWidth: Int,
        pkg: String,
        source: CaptureMode,
        index: Int,
    ): ChatMessage {
        val t = text.trim()
        val isSystem = isSystemLine(t)
        val ts = parseTime(t)
        val isSelf = if (isSystem) false else isSelfByPosition(centerX, screenWidth)
        val sender = when {
            isSystem -> "系统"
            isSelf -> "我"
            else -> "对方"
        }
        return ChatMessage(
            id = "${source.name.lowercase()}-$index",
            sender = sender,
            content = t,
            timestamp = ts,
            isSelf = isSelf,
            source = source,
        )
    }

    /** 是否为系统行（纯时间 / 撤回提示 / 好友添加等）。 */
    fun isSystemLine(text: String): Boolean {
        val t = text.trim()
        if (TIME_ONLY.matcher(t).matches()) return true
        return SYSTEM_KEYWORDS.any { it in t }
    }

    /** 按水平位置判定是否为「自己发的消息」（右侧=自己）。 */
    fun isSelfByPosition(centerX: Int, screenWidth: Int): Boolean {
        if (screenWidth <= 0) return false
        return centerX > screenWidth / 2
    }

    /**
     * 从文本中抠出时间，落成「今天的毫秒时间戳」。
     * 找不到返回 0（表示未知）。
     */
    fun parseTime(text: String): Long {
        val m = TIME_PATTERN.matcher(text)
        if (!m.find()) return 0L
        val cleaned = m.group(2)?.replace("：", ":").orEmpty()
        val parts = cleaned.split(":")
        val hour = parts.firstOrNull()?.toIntOrNull() ?: return 0L
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return 0L
        val period = PERIOD.matcher(text)
        var h = hour
        if (period.find()) {
            val p = period.group(1)
            if ((p == "下午" || p == "晚上") && h < 12) h += 12
        }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    companion object {
        // 纯时间行：上午 10:30 / 晚上 9:05 / 12:30
        private val TIME_ONLY = Pattern.compile("^\\s*(上午|下午|晚上)?\\s*\\d{1,2}[:：]\\d{2}\\s*$")
        // 任意位置出现的时间（捕获组 2 为 HH:MM）
        private val TIME_PATTERN = Pattern.compile("(上午|下午|晚上)?\\s*(\\d{1,2}[:：]\\d{2})")
        private val PERIOD = Pattern.compile("(上午|下午|晚上)")
        private val SYSTEM_KEYWORDS = listOf(
            "撤回", "已添加", "你添加", "对方正在输入", "已通过", "已拒绝",
            "你已", "对方已", "以下是打招呼", "新消息", "条新消息", "长按翻译",
            "按住说话", "语音通话", "视频通话",
        )
    }
}
