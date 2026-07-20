package com.chatassist.domain.model

import com.chatassist.data.local.BUILTIN_PRICING
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 价格数据源一致性（v0.20 质量闭环 → v0.21 品牌分组）：
 * 预设目录 / 种子价 / deriveProvider 三处 provider 命名必须统一，
 * 否则品牌模型会落库成 `custom::...` 而与展示 / 种子价对不上，计费静默失效。
 */
class PricingConsistencyTest {

    @Test
    fun geminiDerivesToGoogle() {
        assertEquals("google", deriveProvider("gemini-1.5-pro"))
        assertEquals("google", deriveProvider("gemini-2.0-flash"))
    }

    @Test
    fun newDomesticProvidersDeriveCorrectly() {
        assertEquals("zhipu", deriveProvider("glm-5.2"))
        assertEquals("doubao", deriveProvider("doubao-seed-2.1-pro"))
        assertEquals("baidu", deriveProvider("ERNIE-5.1"))
        assertEquals("tencent", deriveProvider("Hunyuan-TurboS"))
        assertEquals("minimax", deriveProvider("MiniMax-M3"))
        assertEquals("moonshot", deriveProvider("kimi-k3"))
        assertEquals("deepseek", deriveProvider("deepseek-v4-flash"))
        assertEquals("qwen", deriveProvider("qwen-max"))
    }

    @Test
    fun allPresetProvidersAreKnown() {
        for (p in BUILTIN_PRESETS) {
            assertFalse(deriveProvider(p.model) == "custom", "预设 ${p.model} 不应落到 custom provider")
        }
    }

    @Test
    fun presetProvidersMatchDeriveProvider() {
        for (p in BUILTIN_PRESETS) {
            assertEquals(deriveProvider(p.model), p.provider, "预设 ${p.model} 的 provider 必须 == deriveProvider")
        }
    }

    @Test
    fun brandsFlatMatchesBuiltinPresets() {
        val flat = PRESET_BRANDS.flatMap { it.models }
        assertEquals(BUILTIN_PRESETS.size, flat.size, "品牌平铺数必须 == BUILTIN_PRESETS")
        for (p in BUILTIN_PRESETS) {
            assertTrue(flat.any { it.model.equals(p.model, ignoreCase = true) }, "品牌平铺应含 ${p.model}")
        }
    }

    @Test
    fun seedProvidersMatchDeriveProviderAndKey() {
        for (s in BUILTIN_PRICING) {
            assertEquals(deriveProvider(s.model), s.provider, "种子价 ${s.model} provider 必须 == deriveProvider")
            assertEquals("${s.provider}::${s.model}", s.key, "种子价 key 必须 == provider::model")
        }
    }

    @Test
    fun seedCoversAllPresets() {
        val seedModels = BUILTIN_PRICING.map { it.model.lowercase() }.toSet()
        for (p in BUILTIN_PRESETS) {
            assertTrue(seedModels.contains(p.model.lowercase()), "种子价必须覆盖预设模型 ${p.model}")
        }
        assertEquals(BUILTIN_PRESETS.size, BUILTIN_PRICING.size, "预设数与种子价数应一致")
    }
}
