package com.chatassist.domain.usecase

import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.CounterpartPersona
import com.chatassist.domain.model.PersonaProfile
import com.chatassist.domain.model.SelfPersona
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * PersonaMatcher 三级匹配单测（v0.18）。
 *
 * 覆盖：空列表降级 / activePersonaId 锁定优先 / 包名精确匹配 / 关键词匹配（大小写不敏感）/ 默认方案 / 首个兜底。
 */
class PersonaMatcherTest {

    private fun profile(
        id: String,
        packages: List<String> = emptyList(),
        keywords: List<String> = emptyList(),
        isDefault: Boolean = false,
    ) = PersonaProfile(
        id = id,
        name = id,
        self = SelfPersona(),
        counterpart = CounterpartPersona(),
        boundPackages = packages,
        boundKeywords = keywords,
        isDefault = isDefault,
    )

    @Test
    fun `无方案返回 null 交由调用方降级旧版单人设`() {
        val cfg = AppConfig()
        assertNull(PersonaMatcher.resolve(cfg, "com.tencent.mm", "你好宝贝"))
    }

    @Test
    fun `activePersonaId 锁定优先于包名和关键词`() {
        val cfg = AppConfig(
            personas = listOf(
                profile("wx", packages = listOf("com.tencent.mm")),
                profile("lock", keywords = listOf("锁定")),
            ),
            activePersonaId = "lock",
        )
        assertEquals("lock", PersonaMatcher.resolve(cfg, "com.tencent.mm", "任意文本")?.id)
    }

    @Test
    fun `包名精确匹配优先于关键词`() {
        val cfg = AppConfig(
            personas = listOf(
                profile("wx", packages = listOf("com.tencent.mm")),
                profile("kw", keywords = listOf("宝贝")),
            ),
        )
        assertEquals("wx", PersonaMatcher.resolve(cfg, "com.tencent.mm", "宝贝在吗")?.id)
    }

    @Test
    fun `关键词匹配命中对话文本 大小写不敏感`() {
        val cfg = AppConfig(
            personas = listOf(
                profile("kw", keywords = listOf("BaBy")),
            ),
        )
        assertEquals("kw", PersonaMatcher.resolve(cfg, "", "你好 baby 在吗")?.id)
    }

    @Test
    fun `无包名无关键词命中走默认方案`() {
        val cfg = AppConfig(
            personas = listOf(
                profile("a"),
                profile("def", isDefault = true),
            ),
        )
        assertEquals("def", PersonaMatcher.resolve(cfg, "com.unknown.app", "无关文本")?.id)
    }

    @Test
    fun `无默认方案时走第一个兜底`() {
        val cfg = AppConfig(
            personas = listOf(
                profile("first"),
                profile("second"),
            ),
        )
        assertEquals("first", PersonaMatcher.resolve(cfg, "com.unknown.app", "无关文本")?.id)
    }
}
