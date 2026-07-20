package com.chatassist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 模型定价表（v2.2 §13.3.3，用户可编辑）。
 *
 * 主键 `key = "$provider::$model"`，与 [UsageEntity] 的 provider/model 推导一致。
 */
@Entity(tableName = "pricing")
data class PricingEntity(
    @PrimaryKey val key: String,
    val provider: String,
    val model: String,
    val inputPricePerMToken: Double,       // 每百万输入 token 价格（元）
    val outputPricePerMToken: Double,      // 每百万输出 token 价格（元）
    val cachedPricePerMToken: Double = 0.0,
    val currency: String = "CNY",
    val updatedAt: Long,
    val isBuiltin: Boolean = false,         // 内置定价（用于「恢复默认」）
)
