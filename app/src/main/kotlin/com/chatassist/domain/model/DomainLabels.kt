package com.chatassist.domain.model

/**
 * 枚举 → 中文显示名（v0.16 落地：哥哥下旨"软件内所有语言及信息显示必须是中文"）。
 *
 * 适用范围：SettingsScreen 等所有用户可见处。系统日志、Retrofit role 字段保留英文。
 */
val ThemeMode.displayName: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
    }

val TriggerMode.displayName: String
    get() = when (this) {
        TriggerMode.MANUAL -> "悬浮窗生成按钮"
        TriggerMode.EDGE_BAR -> "侧边条滑动触发"
    }

val ConversationStyle.displayName: String
    get() = when (this) {
        ConversationStyle.PROFESSIONAL -> "专业得体"
        ConversationStyle.CASUAL -> "轻松随意"
        ConversationStyle.WARM -> "温暖亲切"
        ConversationStyle.FLIRTY -> "暧昧撩人"
        ConversationStyle.CONCISE -> "简洁直接"
        ConversationStyle.HUMOROUS -> "幽默风趣"
    }

val PowerMode.displayName: String
    get() = when (this) {
        PowerMode.LOW -> "省电"
        PowerMode.NORMAL -> "均衡"
        PowerMode.HIGH -> "性能"
    }
