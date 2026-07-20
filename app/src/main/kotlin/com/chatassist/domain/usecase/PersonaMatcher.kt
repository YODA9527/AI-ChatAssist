package com.chatassist.domain.usecase

import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.PersonaProfile

/**
 * 双向人设选择器（v0.18，v2.2 §13.1）。
 *
 * 匹配优先级（从高到低）：
 * ① 用户手动锁定的 [AppConfig.activePersonaId]（设置页「设为当前」优先于一切自动匹配）
 * ② 当前 App 包名精确匹配（[PersonaProfile.boundPackages] 含 [targetPackage]）
 * ③ 对话文本关键词匹配（[PersonaProfile.boundKeywords] 任意命中，大小写不敏感）
 * ④ 默认方案（[PersonaProfile.isDefault]）/ 列表第一个
 * ⑤ 无任何方案（[AppConfig.personas] 为空）→ 返回 null，交由调用方降级到旧 v2.1 单人设
 *
 * 纯函数、无副作用，便于单测。
 */
object PersonaMatcher {

    fun resolve(
        cfg: AppConfig,
        targetPackage: String,
        recentText: String,
    ): PersonaProfile? {
        val personas = cfg.personas
        if (personas.isEmpty()) return null

        // ① 用户手动锁定优先
        cfg.personas.firstOrNull { it.id == cfg.activePersonaId }?.let { return it }

        // ② 包名精确匹配（包名全小写，直接 contains 即可）
        if (targetPackage.isNotBlank()) {
            personas.firstOrNull { it.boundPackages.contains(targetPackage) }?.let { return it }
        }

        // ③ 关键词匹配（对方昵称 / 场景词命中对话文本，大小写不敏感）
        val text = recentText.lowercase()
        personas.firstOrNull { p ->
            p.boundKeywords.any { kw -> kw.isNotBlank() && text.contains(kw.lowercase()) }
        }?.let { return it }

        // ④ 默认方案 / 列表第一个兜底
        return personas.firstOrNull { it.isDefault } ?: personas.first()
    }
}
