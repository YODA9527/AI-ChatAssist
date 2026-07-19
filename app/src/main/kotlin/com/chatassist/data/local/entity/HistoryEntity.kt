package com.chatassist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 历史记录 Room 实体（模块 09）。
 *
 * 参考 docs/modules/09-历史记录.md：消息列表与 Prompt 以 JSON 字符串存储，
 * 避免引入多表关系；计费字段（cost / 各 token 数）一并落库，供后续 v2.2 统计。
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val capturedAt: Long,
    val targetPackage: String,
    val messagesJson: String,
    val promptJson: String,
    val responseText: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val cost: Double,
)
