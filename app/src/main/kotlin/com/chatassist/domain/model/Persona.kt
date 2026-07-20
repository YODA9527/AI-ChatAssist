package com.chatassist.domain.model

import kotlinx.serialization.Serializable

/**
 * v2.2 双向人设（§13.1）—「我」这一侧的人设。
 *
 * 字段对齐指南 §13.1：
 * - [name] 名字/昵称
 * - [identity] 身份（我是谁）
 * - [speakingStyle] 说话风格
 * - [taboo] 禁忌/雷区（模型应避免触碰的话题或措辞）
 * - [signature] 口头禅/标志性用语
 */
@Serializable
data class SelfPersona(
    val name: String = "",
    val identity: String = "",
    val speakingStyle: String = "",
    val taboo: String = "",
    val signature: String = "",
)

/**
 * v2.2 双向人设（§13.1）—「对方」这一侧的人设。
 *
 * 字段对齐指南 §13.1：
 * - [name] 对方名字/昵称
 * - [identity] 对方身份
 * - [relationship] 与对方的关系
 * - [theirStyle] 对方说话风格
 * - [theirPersonality] 对方性格
 * - [dynamic] 当前互动动态/关系现状
 * - [goal] 本次聊天目标
 * - [likes] 喜好（兴趣爱好、喜欢聊的话题/事物）
 * - [dislikes] 讨厌的东西（反感、厌恶的人事物）
 * - [petPeeves] 雷区（忌讳、绝对别踩的点）
 * - [habits] 口头习惯（常用语气词、口头禅、小动作描写等）
 *
 * 新增字段（喜好/讨厌/雷区/习惯）均带默认值 ""，旧版持久化的双向方案 JSON 缺这些键时
 * 由 kotlinx.serialization 的默认值兜底为 ""，向后兼容，不抛反序列化异常。
 */
@Serializable
data class CounterpartPersona(
    val name: String = "",
    val identity: String = "",
    val relationship: String = "",
    val theirStyle: String = "",
    val theirPersonality: String = "",
    val dynamic: String = "",
    val goal: String = "",
    val likes: String = "",
    val dislikes: String = "",
    val petPeeves: String = "",
    val habits: String = "",
)

/**
 * v2.2 双向人设方案（§13.1）。
 *
 * 一套完整的双向人设（我 + 对方），可绑定到特定 App 包名 / 关键词，
 * 运行时由 [com.chatassist.domain.usecase.PersonaMatcher] 自动按场景切换。
 *
 * [id] 唯一标识（UUID），用于 [AppConfig.activePersonaId] 锁定与持久化。
 * [boundPackages] 绑定的 App 包名（精确匹配，包名全小写）。
 * [boundKeywords] 绑定的关键词（对方昵称 / 场景词，命中对话文本即匹配，大小写不敏感）。
 * [isDefault] 默认方案（无包名/关键词命中时的兜底）。
 * [lastUsedAt] 最近一次被选中/生效的时间戳（由手动选中维护，自动匹配命中不写回以避免频繁 DataStore 写入）。
 */
@Serializable
data class PersonaProfile(
    val id: String,
    val name: String,
    val self: SelfPersona = SelfPersona(),
    val counterpart: CounterpartPersona = CounterpartPersona(),
    val boundPackages: List<String> = emptyList(),
    val boundKeywords: List<String> = emptyList(),
    val isDefault: Boolean = false,
    val lastUsedAt: Long = 0L,
)

/**
 * 解析后的生效人设（已选定具体双向方案，或兼容旧 v2.1 单人设降级）。
 */
data class ResolvedPersona(
    val self: SelfPersona,
    val counterpart: CounterpartPersona,
)
