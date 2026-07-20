package com.chatassist.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 单个模型的配置（v2.2 §13.4）。
 *
 * **API Key 不在此序列化**：真实 key 走 [SecureKeyStorage] 加密存储，这里只存
 * [apiKeyAlias]（加密存储中的寻址别名，如 `{schemeId}:text`），不含任何明文。
 * 价格内嵌（每百万 token 单价，元），配置模型时即可填，也可从内置预设目录带入。
 */
@Serializable
data class ModelConfig(
    val model: String = "",
    val apiUrl: String = "",
    val apiFormat: ApiFormat = ApiFormat.OPENAI,
    val inputPricePerMToken: Double = 0.0,
    val outputPricePerMToken: Double = 0.0,
    val cachedPricePerMToken: Double = 0.0,
    /** 加密存储中该模型 apiKey 的别名（不含真实 key）。 */
    val apiKeyAlias: String = "",
) {
    /** 由模型名推导服务商（与 [Pricing.keyOf] / [deriveProvider] 一致）。 */
    val provider: String get() = deriveProvider(model)

    /** 计费用主键 key。 */
    fun pricingKey(): String = Pricing.keyOf(provider, model)

    /** 是否看起来已配置完整（可用于 UI 提示）。 */
    fun isConfigured(): Boolean = model.isNotBlank() && apiUrl.isNotBlank()
}

/**
 * 模型配置方案（v2.2 §13.4）。
 *
 * 每套方案包含**文本模型** + **多模态模型**两个独立配置（各自 apiUrl/key/价格），
 * 可存储多套并随时切换。文本模型用于回复生成主链路；多模态模型预留（当前多模态
 * 推理尚未接入，先把配置结构建好）。
 */
@Serializable
data class ModelScheme(
    val id: String,
    val name: String,
    val textModel: ModelConfig,
    val multimodalModel: ModelConfig,
    val isActive: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    companion object {
        /** 出厂默认方案（首次启动 / 老用户迁移兜底）。 */
        fun default(): ModelScheme {
            val now = System.currentTimeMillis()
            return ModelScheme(
                id = "default",
                name = "默认方案",
                textModel = ModelConfig(
                    model = "deepseek-chat",
                    apiUrl = "https://api.deepseek.com/v1",
                    apiKeyAlias = "default:text",
                ),
                multimodalModel = ModelConfig(
                    model = "local",
                    apiUrl = "http://localhost:11434/v1",
                    apiKeyAlias = "default:multimodal",
                ),
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        }

        /** 生成新方案骨架（含 UUID 与独立加密别名）。 */
        fun newScheme(name: String): ModelScheme {
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            return ModelScheme(
                id = id,
                name = name.ifBlank { "新方案" },
                textModel = ModelConfig(apiKeyAlias = "$id:text"),
                multimodalModel = ModelConfig(apiKeyAlias = "$id:multimodal"),
                isActive = false,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}

/**
 * 取当前激活方案。
 *
 * 优先级：显式方案列表 > 旧版单模型字段兜底（迁移前 / 测试态，避免生成链路拿不到模型）
 * > 全空占位（运行时由 [ConfigRepository.migrateLegacySchemes] 覆盖为出厂默认方案）。
 */
fun AppConfig.activeScheme(): ModelScheme {
    schemes.firstOrNull { it.id == activeSchemeId }?.let { return it }
    schemes.firstOrNull()?.let { return it }
    if (apiUrl.isNotBlank() || model.isNotBlank()) {
        return ModelScheme(
            id = "legacy",
            name = "（旧版配置）",
            textModel = ModelConfig(model = model, apiUrl = apiUrl, apiFormat = apiFormat),
            multimodalModel = ModelConfig(),
            isActive = true,
        )
    }
    return ModelScheme(
        id = "",
        name = "",
        textModel = ModelConfig(),
        multimodalModel = ModelConfig(),
    )
}

/** 取当前生效的文本模型配置（聚合层统一入口）。 */
fun AppConfig.activeTextModel(): ModelConfig = activeScheme().textModel
