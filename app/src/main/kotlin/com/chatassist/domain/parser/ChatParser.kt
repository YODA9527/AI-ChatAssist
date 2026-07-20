package com.chatassist.domain.parser

import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import java.util.Calendar
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天内容解析器（v0.6 起逐步迭代）。
 *
 * 把一条「原始文本 + 几何信息」解析成结构化 [ChatMessage]：
 * - [ChatMessage.isSelf]：按文本水平中心位置 + App 预设判定（v0.10 起支持包名反转）。
 * - [ChatMessage.sender]：「我 / 对方 / 系统」。
 * - [ChatMessage.timestamp]：从文本里抠出 HH:MM（支持「上午/下午/晚上」前缀），落成当日毫秒时间戳。
 * - [ChatMessage.confidence]：v0.10 起按「来源+几何+长度+位置+App 预设」四维计算，UI 可据此提示
 *   「识别不确定，请核对」（< 0.7）。
 * - 系统消息（时间戳行、「撤回」「已添加」等）单独标记为「系统」，不参与回复建议。
 *
 * 纯 Kotlin（不持有 Android 框架状态），因此可 JUnit 单测 + 双通道（无障碍/OCR）复用同一套逻辑。
 * v0.10 起 [parse] 计算 confidence；颜色聚类、多 App 预设库已在 §12.2 规划、v0.10+ 迭代。
 */
@Singleton
class ChatParser @Inject constructor() {

    /**
     * @param text       原始文本
     * @param centerX    文本水平中心像素（无几何信息时传 -1）
     * @param screenWidth 屏幕宽度像素（无几何信息时传 0/负数）
     * @param pkg        目标 App 包名（v0.10 起按包名查预设表，反转 isSelf 判定）
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
        val isSelf = if (isSystem) false else isSelfByPosition(centerX, screenWidth, pkg)
        val sender = when {
            isSystem -> "系统"
            isSelf -> "我"
            else -> "对方"
        }
        val confidence = calcConfidence(
            text = t,
            isSystem = isSystem,
            centerX = centerX,
            screenWidth = screenWidth,
            pkg = pkg,
            source = source,
        )
        return ChatMessage(
            id = "${source.name.lowercase()}-$index",
            sender = sender,
            content = t,
            timestamp = ts,
            isSelf = isSelf,
            source = source,
            confidence = confidence,
        )
    }

    /** 是否为系统行（纯时间 / 撤回提示 / 好友添加等）。 */
    fun isSystemLine(text: String): Boolean {
        val t = text.trim()
        if (TIME_ONLY.matcher(t).matches()) return true
        return SYSTEM_KEYWORDS.any { it in t }
    }

    /**
     * 按水平位置 + App 预设判定是否为「自己发的消息」（右侧=自己 / 左侧=自己）。
     *
     * v0.10 起支持 AppPreset：微信等大部分 IM 右侧=自己；个别 App（如 Slack 英文版部分皮肤）左侧=自己。
     * 无屏幕宽度信息时按"未知"处理（默认右侧=自己），UI 应根据 confidence 字段给出核对提示。
     */
    fun isSelfByPosition(centerX: Int, screenWidth: Int, pkg: String = ""): Boolean {
        if (screenWidth <= 0) return false
        val preset = PRESETS[pkg]
        val rightIsSelf = preset?.alignLeftIsSelf?.not() ?: true
        return if (rightIsSelf) centerX > screenWidth / 2 else centerX < screenWidth / 2
    }

    /**
     * 置信度评分（v0.10 新增）。
     *
     * 五维加权（基准 0.7），最终 clamp 到 [0, 1]：
     * | 维度        | 条件                | 加分        |
     * |-------------|---------------------|-------------|
     * | 来源        | TEXT_ONLY           | +0.1        |
     * | 几何        | 有完整 centerX/screenWidth | +0.1 |
     * | 长度        | 1-2 字              | -0.4        |
     * | 长度        | 3-4 字              | -0.1        |
     * | 长度        | 5+ 字               | +0.05       |
     * | 位置贴边    | centerX < sw*0.08 或 > sw*0.92 | -0.2 |
     * | App 预设    | pkg 命中预设        | +0.05       |
     * | 系统消息    | 是                  | 强制 0.5    |
     *
     * < 0.7 时 UI 应提示「识别不确定，请核对」（v0.10+ UI 接入）。
     */
    fun calcConfidence(
        text: String,
        isSystem: Boolean,
        centerX: Int,
        screenWidth: Int,
        pkg: String,
        source: CaptureMode,
    ): Float {
        if (isSystem) return 0.5f
        var conf = 0.7f
        if (source == CaptureMode.TEXT_ONLY) conf += 0.1f
        val hasGeo = centerX > 0 && screenWidth > 0
        if (hasGeo) conf += 0.1f
        when (text.length) {
            0 -> conf -= 0.4f
            1, 2 -> conf -= 0.4f
            3, 4 -> conf -= 0.1f
            else -> conf += 0.05f
        }
        if (hasGeo) {
            val leftEdge = screenWidth * 0.08
            val rightEdge = screenWidth * 0.92
            if (centerX < leftEdge || centerX > rightEdge) conf -= 0.2f
        }
        if (pkg in PRESETS) conf += 0.05f
        return conf.coerceIn(0f, 1f)
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

        /**
         * 系统消息关键词（v0.10 扩充：F 项）。
         * 包括撤回、好友添加、对方正在输入、提示横幅、红包、通话、长按提示等。
         */
        private val SYSTEM_KEYWORDS = listOf(
            "撤回", "已添加", "你添加", "对方正在输入", "已通过", "已拒绝",
            "你已", "对方已", "以下是打招呼", "新消息", "条新消息", "长按翻译",
            "按住说话", "语音通话", "视频通话",
            // v0.10 扩充
            "红包已拆开", "已领取", "已收款", "已退还",
            "拍了拍", "你拍了拍",
            "已开启", "已关闭", "已设置",
            "撤回了一条消息",
            "撤回了一条",
            "消息已发送", "消息发送失败",
            "对方忙", "未接听", "已取消",
            "图片已过期", "链接已失效",
        )

        /**
         * 内置 App 预设（v0.10 起：B 项，v0.10.1 精简为 5 个国内主流办公/社交 IM）。
         * 包名 → [AppPreset]。所有内置 App 均右侧=自己（alignLeftIsSelf=false）。
         * 未命中预设的 App 走"右侧=自己"通用规则，confidence 略降。
         *
         * 覆盖范围：微信 / QQ / 钉钉 / 飞书 / 企业微信。
         * 不覆盖：TIM/Telegram/Slack/WhatsApp/LINE/短信等小众场景（用户可提 issue 增量添加）。
         */
        private val PRESETS: Map<String, AppPreset> = mapOf(
            "com.tencent.mm" to AppPreset("com.tencent.mm", "微信", alignLeftIsSelf = false),
            "com.tencent.mobileqq" to AppPreset("com.tencent.mobileqq", "QQ", alignLeftIsSelf = false),
            "com.alibaba.dingtalk" to AppPreset("com.alibaba.dingtalk", "钉钉", alignLeftIsSelf = false),
            "com.ss.android.lark" to AppPreset("com.ss.android.lark", "飞书", alignLeftIsSelf = false),
            "com.tencent.wework" to AppPreset("com.tencent.wework", "企业微信", alignLeftIsSelf = false),
        )
    }
}

/**
 * 单个 App 的解析预设（v0.10：B 项）。
 *
 * @param pkg             Android 包名
 * @param label           显示名（仅日志/调试用）
 * @param alignLeftIsSelf true=该 App 左侧=自己；false=该 App 右侧=自己（多数 IM 默认）
 */
data class AppPreset(
    val pkg: String,
    val label: String,
    val alignLeftIsSelf: Boolean,
)
