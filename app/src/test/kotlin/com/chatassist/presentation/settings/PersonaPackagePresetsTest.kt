package com.chatassist.presentation.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PersonaPackagePresetsTest {

    private val all = PERSONA_PACKAGE_PRESET_GROUPS.flatMap { it.second }

    /** 人设绑定预设必须覆盖无障碍模式 [com.chatassist.domain.parser.ChatParser.PRESETS] 的 5 个主流 IM。 */
    @Test
    fun `覆盖无障碍模式原 5 个主流 IM 包名`() {
        val baseline = listOf(
            "com.tencent.mm", // 微信
            "com.tencent.mobileqq", // QQ
            "com.alibaba.dingtalk", // 钉钉
            "com.ss.android.lark", // 飞书
            "com.tencent.wework", // 企业微信
        )
        baseline.forEach { pkg ->
            assertTrue(all.any { it.pkg == pkg }, "人设预设应覆盖无障碍基线包名 $pkg")
        }
    }

    @Test
    fun `包名格式合法且无重复`() {
        all.forEach { p ->
            assertTrue(
                p.pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+$")),
                "非法包名 ${p.pkg}",
            )
        }
        assertEquals(all.size, all.map { it.pkg }.toSet().size, "预设包名存在重复")
    }
}
