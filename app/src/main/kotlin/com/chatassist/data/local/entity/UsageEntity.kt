package com.chatassist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单次模型调用计费记录（v2.2 §13.3.2）。
 *
 * 与历史记录关联（[historyId]），但单独成表便于聚合统计。
 * 历史可清空、用量保留（二者解耦）。
 *
 * `status` 以字符串存储（枚举名），避免 Room 枚举转换依赖。
 */
@Entity(tableName = "usage")
data class UsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val historyId: Long = 0,
    val createdAt: Long,
    val provider: String,           // "deepseek" / "openai"
    val model: String,              // "deepseek-chat" / "gpt-4o"
    val promptTokens: Int,          // 输入 token
    val completionTokens: Int,      // 输出 token
    val totalTokens: Int,           // 总 token
    val cachedTokens: Int = 0,      // 命中缓存的 token
    val costMicro: Long,            // 费用（微元，1 元 = 1_000_000 微元）
    val targetPackage: String,      // 目标 App
    val status: String,             // UsageStatus 枚举名
)
