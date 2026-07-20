package com.chatassist.data.local.entity

/**
 * 用量聚合结果载体（v2.2 §13.3.6），由 @Query 的 SELECT 列别名直接实例化。
 *
 * 非 @Entity，仅作 DAO 查询返回类型。字段名必须与 SQL 列别名一致。
 */

/** 时间范围内用量汇总。 */
data class UsageSummary(
    val callCount: Int,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val costMicro: Long,
    val successCount: Int,
    val failCount: Int,
)

/** 按模型聚合。 */
data class UsageByModel(
    val provider: String,
    val model: String,
    val callCount: Int,
    val totalTokens: Int,
    val costMicro: Long,
)

/** 按 App 聚合。 */
data class UsageByApp(
    val targetPackage: String,
    val callCount: Int,
    val totalTokens: Int,
    val costMicro: Long,
)
