package com.chatassist.domain.model

import kotlinx.serialization.Serializable

/**
 * 模型定价（v2.2 §13.3.3）。
 *
 * 价格单位：每百万 token 的单价（元）。用户可在「定价管理」页编辑或新增。
 * [key] 为 `{provider}::{model}`，与 [RecordUsageUseCase] 写入的 `UsageEntity.key` 对齐。
 */
@Serializable
data class Pricing(
    val key: String,
    val provider: String,
    val model: String,
    val inputPricePerMToken: Double,
    val outputPricePerMToken: Double,
    val cachedPricePerMToken: Double = 0.0,
    val currency: String = "CNY",
    val updatedAt: Long = 0,
    val isBuiltin: Boolean = false,
) {
    companion object {
        fun keyOf(provider: String, model: String): String = "$provider::$model"
    }
}

/** 由模型名推导服务商（用于落库时未显式带 provider 的兜底）。 */
fun deriveProvider(model: String): String {
    val m = model.lowercase()
    return when {
        m.startsWith("deepseek") -> "deepseek"
        m.startsWith("gpt") || m.startsWith("o1") || m.startsWith("o3") || m.startsWith("chatgpt") -> "openai"
        m.startsWith("claude") -> "anthropic"
        m.startsWith("moonshot") || m.startsWith("kimi") -> "moonshot"
        m.startsWith("qwen") -> "qwen"
        m.startsWith("glm") -> "zhipu"
        m.startsWith("doubao") -> "doubao"
        m.startsWith("ernie") -> "baidu"
        m.startsWith("hunyuan") -> "tencent"
        m.startsWith("minimax") -> "minimax"
        m.startsWith("gemini") -> "google"
        m.startsWith("local") -> "local"
        else -> "custom"
    }
}
