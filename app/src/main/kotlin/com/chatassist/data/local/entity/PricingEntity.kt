package com.chatassist.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 模型定价表（v2.2 §13.3.3，用户可编辑）。
 *
 * 主键 `key = "$provider::$model"`，与 [UsageEntity] 的 provider/model 推导一致。
 *
 * v0.22 (C1 性能优化)：加 [Index("model")]，支撑 `WHERE model = :model LIMIT 1`
 * 自填模型时按模型名兜底匹配（v0.20 PricingDao.getByModel）。key 已是主键无需重复。
 * 数据量小（百行级）实际收益有限，但保持索引对齐规范、不留隐患。
 */
@Entity(
    tableName = "pricing",
    indices = [Index(value = ["model"])],
)
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
