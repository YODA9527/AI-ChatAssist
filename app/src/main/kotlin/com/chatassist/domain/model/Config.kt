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

    // ======================== 人设 ========================
    val persona: PersonaConfig = PersonaConfig(),

    // ======================== 目标语气 ========================
    val goal: ConversationGoal = ConversationGoal.WARM,

    // ======================== 模型链路（v2.2 新增）====================
    val chains: List<ModelChain> = listOf(
        ModelChain(
            id = 0,
            name = "经济模式",
            recognitionMode = RecognitionMode.OCR_ONLY,
            modelType = ModelType.TEXT,
            provider = "deepseek",
            model = "deepseek-chat",
            isDefault = true,
        )
    ),
    val activeChainId: Long = 0,
) {
    /**
     * 获取当前激活的链路。
     */
    fun activeChain(): ModelChain =
        chains.firstOrNull { it.id == activeChainId } ?: chains.first()
}

/**
 * API 协议格式。
 */
@Serializable
enum class ApiFormat { OPENAI, CLAUDE, CUSTOM }

/**
 * 触发方式。
 */
@Serializable
enum class TriggerMode { MANUAL, NOTIFICATION, EDGE_BAR, GESTURE }

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
 * 对话目标语气。
 */
@Serializable
enum class ConversationGoal {
    PROFESSIONAL,    // 专业
    CASUAL,          // 随意
    WARM,            // 温暖
    FLIRTY,          // 暧昧
    CONCISE,         // 简洁
    HUMOROUS,        // 幽默
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
 * 模型调用链路（v2.2 新增）。
 */
@Serializable
data class ModelChain(
    val id: Long,
    val name: String,                       // "经济模式" / "智能模式"
    val recognitionMode: RecognitionMode,
    val modelType: ModelType,
    val provider: String,
    val model: String,
    val isDefault: Boolean = false,
)

/**
 * 识别方式。
 */
@Serializable
enum class RecognitionMode { OCR_ONLY, VISION }

/**
 * 模型类型。
 */
@Serializable
enum class ModelType { TEXT, MULTIMODAL }
