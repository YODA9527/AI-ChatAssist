package com.chatassist.domain.model

import kotlinx.serialization.Serializable

/**
 * 应用全部配置项。
 *
 * 设计原则：
 * 1. 所有字段都有默认值（与附录 C 默认配置表一致）
 * 2. 不可变（val），更新走 `ConfigRepository.update { ... }` 拷贝方式
 * 3. 嵌套结构用 @Serializable 持久化
 */
@Serializable
data class AppConfig(
    // ======================== API 配置 ========================
    val apiUrl: String = "https://api.deepseek.com/v1",
    val apiKey: String = "",                                 // 用户必填，初始为空
    val model: String = "deepseek-chat",
    val apiFormat: ApiFormat = ApiFormat.OPENAI,
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024,
    val maxContextTokens: Int = 8000,
    val contextRounds: Int = 10,
    val streamMode: Boolean = true,

    // ======================== OCR 配置 ========================
    val ocrAutoCorrect: Boolean = true,

    // ======================== 触发配置 ========================
    // v0.17：精简为两种触发方式（哥哥下旨）
    //  - MANUAL：悬浮窗「生成」按钮（默认）
    //  - EDGE_BAR：屏幕右侧 16dp 边缘触发条（双击触发主面板）
    // 已移除 v0.16 的 NOTIFICATION（通知触发）和 GESTURE（手势触发）——见 CHANGELOG v0.17
    val triggerMode: TriggerMode = TriggerMode.MANUAL,

    // ======================== 悬浮窗体验（v0.8）====================
    val smartSnap: Boolean = true,          // 智能贴边半透明吸附
    val typewriter: Boolean = true,         // 打字机效果（流式逐字显示）
    val autoPaste: Boolean = false,         // 自动粘贴（仅精准模式/无障碍可用时生效）

    // ======================== 主题与无障碍 ========================
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,

    // ======================== 省电 ========================
    val powerMode: PowerMode = PowerMode.NORMAL,

    // ======================== 历史记录 ========================
    val historyMaxAgeDays: Int = 30,
    val historyMaxCount: Int = 1000,

    // ======================== 隐私 ========================
    val privacyMode: Boolean = false,

    // ======================== 人设（v2.1 单人设，兼容保留）========================
    // v0.18 升级为 v2.2 双向多套人设（见 [personas]）。
    // 旧版本用户 DataStore 里的 [persona] 字段继续生效：当 [personas] 为空时，
    // BuildPromptUseCase 自动降级使用本字段，老用户人设不丢。
    val persona: PersonaConfig = PersonaConfig(),

    // ======================== 双向人设方案（v0.18，v2.2 §13.1）========================
    // 多套完整双向人设（我 + 对方），可绑定 App 包名 / 关键词自动切换。
    // [activePersonaId] 为用户手动锁定的当前方案；为空时由 PersonaMatcher 按场景自动选。
    // 向后兼容：旧 JSON 无 [personas]/[activePersonaId] → 强容错 Json 用默认值
    // （emptyList() / ""），不会因未知字段抛 SerializationException。
    val personas: List<PersonaProfile> = emptyList(),
    val activePersonaId: String = "",

    // ======================== 计费与预算（v2.2 §13.3）========================
    // 月度预算：0 = 不设（不限）。超额拦截不可静默，由 ReplySuggestionUseCase 在生成前校验。
    val budget: BudgetConfig = BudgetConfig(),

    // ======================== 对话风格（原"目标语气"，v0.17 重命名）========================
    // v0.17：枚举名 ConversationGoal → ConversationStyle，AppConfig 字段名 goal → style
    // 含义从「目标语气」调整为「对话风格」，更贴合用户语义
    // 显示文案「对话目标」→「对话风格」
    val style: ConversationStyle = ConversationStyle.WARM,

    // ======================== 模型配置方案（v2.2 §13.4）====================
    // 多套可切换方案，每套含文本模型 + 多模态模型（价格内嵌）。
    // 旧版单模型字段 apiUrl/apiKey/model/apiFormat 保留作迁移源，首次启动迁移进 [schemes]。
    val schemes: List<ModelScheme> = emptyList(),
    val activeSchemeId: String = "",
) {
    /**
     * 获取当前激活的方案；无方案时返回内置默认方案（不修改持久化状态）。
     */
    fun activeScheme(): ModelScheme =
        schemes.firstOrNull { it.id == activeSchemeId } ?: schemes.firstOrNull() ?: ModelScheme.default()
}

/**
 * API 协议格式。
 */
@Serializable
enum class ApiFormat { OPENAI, CLAUDE, CUSTOM }

/**
 * 触发方式（v0.17 精简）。
 *
 * 哥哥下旨（2026-07-19）：只保留两种最直观的触发
 *  - [MANUAL]：悬浮窗常驻「生成」按钮（默认）
 *  - [EDGE_BAR]：屏幕右侧 16dp 边缘触发条（双击触发主面板）
 *
 * 已移除 v0.16 的 NOTIFICATION / GESTURE（不够直观，且 NOTIFICATION 需要监听系统通知权限
 * 在 Android 13+ 收得更紧，GESTURE 在国产 ROM 上和全面屏手势冲突 → 实际可用性差）。
 *
 * 旧 DataStore JSON 兼容：见 [com.chatassist.data.local.ConfigRepositoryImpl] 的强容错 Json 配置
 * （`coerceInputValues = true`）—— 旧 JSON 里的 "NOTIFICATION" / "GESTURE" 字符串会
 * 自动回退到默认值 [MANUAL] 而非抛 [kotlinx.serialization.SerializationException] 闪退。
 */
@Serializable
enum class TriggerMode { MANUAL, EDGE_BAR }

/**
 * 主题模式。
 */
@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * 省电模式。
 */
@Serializable
enum class PowerMode { LOW, NORMAL, HIGH }

/**
 * 对话风格（v0.17 重命名，原 [ConversationGoal]「对话目标语气」）。
 *
 * 哥哥下旨（2026-07-19）：「对话目标」措辞改为「对话风格」，更贴切实际效果。
 * 中文 displayName 已对齐（DomainLabels.kt）。
 *
 * 6 种风格：专业得体 / 轻松随意 / 温暖亲切 / 暧昧撩人 / 简洁直接 / 幽默风趣
 * 实际 prompt 注入见 [com.chatassist.domain.usecase.BuildPromptUseCase.styleText]。
 */
@Serializable
enum class ConversationStyle {
    PROFESSIONAL,    // 专业得体
    CASUAL,          // 轻松随意
    WARM,            // 温暖亲切
    FLIRTY,          // 暧昧撩人
    CONCISE,         // 简洁直接
    HUMOROUS,        // 幽默风趣
}

/**
 * v2.1 简化版人设（v2.2 升级为 DualPersona，先用 v2.1 版本）。
 */
@Serializable
data class PersonaConfig(
    val whoAmI: String = "",                // 我是谁
    val speakingStyle: String = "",          // 说话风格
    val relationship: String = "",           // 关系
)

/**
 * 模型配置方案（v2.2 §13.4）相关定义见 [ModelScheme]，避免循环依赖单独置于 ModelScheme.kt。
 */
