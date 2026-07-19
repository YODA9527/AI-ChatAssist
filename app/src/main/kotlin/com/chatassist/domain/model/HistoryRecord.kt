package com.chatassist.domain.model

/**
 * 一条历史记录（模块 09，UI 层消费）。
 *
 * 由 [com.chatassist.data.local.HistoryRepositoryImpl] 把 Room 实体映射而来，
 * 不包含任何 Room / 数据库注解，保证 domain 层不反向依赖 data 层。
 */
data class HistoryRecord(
    val id: Long,
    val capturedAt: Long,
    val targetPackage: String,
    val messages: List<ChatMessage>,
    val responseText: String,
    val usage: TokenUsage,
    val cost: Double,
)

/**
 * 落库输入（模块 09）。
 *
 * 由 [com.chatassist.domain.usecase.ReplySuggestionUseCase] 在生成完成时构造。
 * `cost` 暂由计费模块（v2.2）在落库前填入，当前默认 0.0。
 */
data class HistoryRecordInput(
    val targetPackage: String,
    val messages: List<ChatMessage>,
    val prompt: BuiltPrompt?,
    val responseText: String,
    val usage: TokenUsage,
    val cost: Double = 0.0,
)
