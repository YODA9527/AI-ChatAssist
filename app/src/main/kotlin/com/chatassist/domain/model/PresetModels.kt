package com.chatassist.domain.model

/**
 * 内置国内模型预设目录（v0.21 · 品牌分组）。
 *
 * 设计（用户需求 2026-07-20）：
 * - 国外模型（OpenAI / Anthropic / Google 等）**全部移出预设**，仅保留「自定义配置」能力；
 *   用户仍可在模型方案编辑页手动填任意接口地址 + 模型名 + 价格（[deriveProvider] 对这些前缀仍兜底）。
 * - 国内模型按**品牌**分组，每个品牌下二级菜单预设该品牌的最新若干模型（[PresetBrand.models]）。
 * - 各模型的 [PresetModel.defaultApiUrl] 统一取所属品牌的 OpenAI 兼容基址。
 *
 * 价格来源：初夏于 2026-07-20 逐一联网核对各厂商官方 API 文档 / 计费页得出，单位 元/百万 tokens。
 * 官方价随时间调整，落库前请按需核对；标注「≈」者为官方页未直接给出、按同代已知价估算（已逐行注明）。
 *
 * [provider] 不手填，由 [deriveProvider] 推导，与计费侧 [Pricing.keyOf] 严格一致（v0.20.1 已焊死）。
 */
data class PresetModel(
    val model: String,
    val defaultApiUrl: String,
    val apiFormat: ApiFormat = ApiFormat.OPENAI,
    val inputPricePerMToken: Double,
    val outputPricePerMToken: Double,
    val cachedPricePerMToken: Double = 0.0,
    val multimodal: Boolean = false,
) {
    /** 服务商由模型名推导，与 [deriveProvider] / [Pricing.keyOf] 严格一致。 */
    val provider: String get() = deriveProvider(model)
}

/** 同一厂商的最新模型集合（预设选择时的「品牌 → 二级菜单」分组单位）。 */
data class PresetBrand(
    val brand: String,
    val brandKey: String,
    val apiBaseUrl: String,
    val models: List<PresetModel>,
)

private const val QWEN_BASE = "https://dashscope.aliyuncs.com/compatible-mode/v1"
private const val DEEPSEEK_BASE = "https://api.deepseek.com/v1"
private const val ZHIPU_BASE = "https://open.bigmodel.cn/api/paas/v4"
private const val KIMI_BASE = "https://api.moonshot.cn/v1"
private const val DOUBAO_BASE = "https://ark.cn-beijing.volces.com/api/v3"
private const val BAIDU_BASE = "https://qianfan.baidubce.com/v2"
private const val TENCENT_BASE = "https://api.hunyuan.cloud.tencent.com/v1"
private const val MINIMAX_BASE = "https://api.minimax.chat/v1"
private const val LOCAL_BASE = "http://localhost:11434/v1"

/**
 * 品牌分组预设目录（国内模型）。国外模型不在其中——如需使用，编辑页手动自定义即可。
 * 价格均为官方文档「元/百万 tokens」；带「≈」为估算值（官方页未直接给价）。
 */
val PRESET_BRANDS: List<PresetBrand> = listOf(
    PresetBrand("阿里云百炼 · 通义千问", "qwen", QWEN_BASE, listOf(
        PresetModel("qwen-max", QWEN_BASE, inputPricePerMToken = 2.4, outputPricePerMToken = 9.6, multimodal = true),
        PresetModel("qwen-plus", QWEN_BASE, inputPricePerMToken = 0.8, outputPricePerMToken = 2.0),
        PresetModel("qwen-turbo", QWEN_BASE, inputPricePerMToken = 0.3, outputPricePerMToken = 0.6),
        PresetModel("qwen-long", QWEN_BASE, inputPricePerMToken = 0.5, outputPricePerMToken = 2.0),
        PresetModel("qwen-coder", QWEN_BASE, inputPricePerMToken = 0.8, outputPricePerMToken = 2.0),
        // 注：qwen3.8-max-preview 等需 Token Plan 抢先体验，未列入；上述为官方长期在售稳定线。
    )),
    PresetBrand("DeepSeek", "deepseek", DEEPSEEK_BASE, listOf(
        PresetModel("deepseek-v4-flash", DEEPSEEK_BASE, inputPricePerMToken = 1.0, outputPricePerMToken = 2.0, cachedPricePerMToken = 0.02),
        PresetModel("deepseek-v4-pro", DEEPSEEK_BASE, inputPricePerMToken = 3.0, outputPricePerMToken = 6.0, cachedPricePerMToken = 0.025),
        PresetModel("deepseek-chat", DEEPSEEK_BASE, inputPricePerMToken = 1.0, outputPricePerMToken = 2.0, cachedPricePerMToken = 0.1), // 旧名，2026-07-24 弃用但仍可调用
        PresetModel("deepseek-reasoner", DEEPSEEK_BASE, inputPricePerMToken = 4.0, outputPricePerMToken = 16.0, cachedPricePerMToken = 1.0), // 旧名，2026-07-24 弃用
    )),
    PresetBrand("智谱 AI · GLM", "zhipu", ZHIPU_BASE, listOf(
        PresetModel("glm-5.2", ZHIPU_BASE, inputPricePerMToken = 8.0, outputPricePerMToken = 28.0),
        PresetModel("glm-5.1", ZHIPU_BASE, inputPricePerMToken = 6.0, outputPricePerMToken = 24.0), // 取 ≤32k 基础档
        PresetModel("glm-5-turbo", ZHIPU_BASE, inputPricePerMToken = 5.0, outputPricePerMToken = 22.0),
        PresetModel("glm-5", ZHIPU_BASE, inputPricePerMToken = 4.0, outputPricePerMToken = 18.0),
        PresetModel("glm-4.7", ZHIPU_BASE, inputPricePerMToken = 2.0, outputPricePerMToken = 8.0), // 取基础档
    )),
    PresetBrand("月之暗面 · Kimi", "moonshot", KIMI_BASE, listOf(
        PresetModel("kimi-k3", KIMI_BASE, inputPricePerMToken = 20.0, outputPricePerMToken = 100.0, cachedPricePerMToken = 2.0), // 官方：缓存命中2 / 未命中20，输出100
        PresetModel("kimi-k2", KIMI_BASE, inputPricePerMToken = 2.0, outputPricePerMToken = 10.0), // ≈ 2026-04 降价后 2/10，建议核对
        PresetModel("kimi-k2-thinking", KIMI_BASE, inputPricePerMToken = 2.0, outputPricePerMToken = 10.0), // ≈ 同 k2
        PresetModel("moonshot-v1-8k", KIMI_BASE, inputPricePerMToken = 2.0, outputPricePerMToken = 10.0),
        PresetModel("kimi-latest-8k", KIMI_BASE, inputPricePerMToken = 2.0, outputPricePerMToken = 10.0), // ≈ 同降价后档
    )),
    PresetBrand("火山引擎 · 豆包", "doubao", DOUBAO_BASE, listOf(
        PresetModel("doubao-seed-2.1-pro", DOUBAO_BASE, inputPricePerMToken = 6.0, outputPricePerMToken = 30.0, cachedPricePerMToken = 1.2),
        PresetModel("doubao-seed-2.1-turbo", DOUBAO_BASE, inputPricePerMToken = 3.0, outputPricePerMToken = 15.0, cachedPricePerMToken = 0.6), // Pro 一半
        PresetModel("doubao-pro-32k", DOUBAO_BASE, inputPricePerMToken = 0.8, outputPricePerMToken = 2.0), // ≈ 官方旧档价
        PresetModel("doubao-lite-32k", DOUBAO_BASE, inputPricePerMToken = 0.3, outputPricePerMToken = 0.6), // ≈
        PresetModel("doubao-1.5-pro", DOUBAO_BASE, inputPricePerMToken = 3.0, outputPricePerMToken = 9.0), // ≈
    )),
    PresetBrand("百度智能云 · 文心", "baidu", BAIDU_BASE, listOf(
        PresetModel("ERNIE-5.1", BAIDU_BASE, inputPricePerMToken = 4.0, outputPricePerMToken = 18.0), // ≤32k 基础档
        PresetModel("ERNIE-5.0", BAIDU_BASE, inputPricePerMToken = 6.0, outputPricePerMToken = 24.0),
        PresetModel("ERNIE-4.5-Turbo-128K", BAIDU_BASE, inputPricePerMToken = 0.8, outputPricePerMToken = 3.2, cachedPricePerMToken = 0.2),
        PresetModel("ERNIE-X1.1", BAIDU_BASE, inputPricePerMToken = 1.0, outputPricePerMToken = 4.0),
        PresetModel("ERNIE-4.5-Turbo-32K", BAIDU_BASE, inputPricePerMToken = 0.8, outputPricePerMToken = 3.2, cachedPricePerMToken = 0.2),
    )),
    PresetBrand("腾讯云 · 混元", "tencent", TENCENT_BASE, listOf(
        PresetModel("Hunyuan-2.0-Thinking", TENCENT_BASE, inputPricePerMToken = 3.98, outputPricePerMToken = 15.90), // ≈ 第三方 llmabacus 报价
        PresetModel("Hunyuan-T1", TENCENT_BASE, inputPricePerMToken = 1.0, outputPricePerMToken = 4.0), // ≈ 第三方 mofcloud 报价
        PresetModel("Hunyuan-TurboS", TENCENT_BASE, inputPricePerMToken = 0.8, outputPricePerMToken = 2.0), // ≈ 公开报道
        PresetModel("Hunyuan-a13b", TENCENT_BASE, inputPricePerMToken = 0.5, outputPricePerMToken = 2.0),
        PresetModel("Hunyuan-role-latest", TENCENT_BASE, inputPricePerMToken = 2.4, outputPricePerMToken = 9.6),
    )),
    PresetBrand("MiniMax", "minimax", MINIMAX_BASE, listOf(
        PresetModel("MiniMax-M3", MINIMAX_BASE, inputPricePerMToken = 2.1, outputPricePerMToken = 8.4), // 永久五折后
        PresetModel("MiniMax-M2.7", MINIMAX_BASE, inputPricePerMToken = 2.1, outputPricePerMToken = 8.4),
        PresetModel("MiniMax-M2.5", MINIMAX_BASE, inputPricePerMToken = 2.1, outputPricePerMToken = 8.4),
        PresetModel("MiniMax-M2.5-highspeed", MINIMAX_BASE, inputPricePerMToken = 4.2, outputPricePerMToken = 16.8),
        PresetModel("MiniMax-M2.1", MINIMAX_BASE, inputPricePerMToken = 2.1, outputPricePerMToken = 8.4),
    )),
    PresetBrand("本地部署（Ollama 等）", "local", LOCAL_BASE, listOf(
        PresetModel("local", LOCAL_BASE, inputPricePerMToken = 0.0, outputPricePerMToken = 0.0, cachedPricePerMToken = 0.0, multimodal = true),
    )),
)

/** 平铺的全部预设模型（= 各品牌模型之和），供 [findPreset] / 种子价派生 / 一致性测试使用。 */
val BUILTIN_PRESETS: List<PresetModel> = PRESET_BRANDS.flatMap { it.models }

/** 按模型名在预设目录中查找（provider 精确优先，否则按模型名匹配）。 */
fun findPreset(model: String): PresetModel? =
    BUILTIN_PRESETS.firstOrNull { it.model.equals(model, ignoreCase = true) }
        ?: BUILTIN_PRESETS.firstOrNull { model.equals(it.model, ignoreCase = true) }
