package com.chatassist.domain.model

/**
 * 单条计费记录（v2.2 §13.3.2，domain 层消费模型）。
 *
 * 由 [com.chatassist.data.local.UsageRepositoryImpl] 把 Room 实体映射而来，
 * 不含任何 Room 注解，保证 domain 层不反向依赖 data 层。
 *
 * 与历史记录关联（[historyId]），但单独存表便于聚合统计（历史可清空、用量保留）。
 */
data class UsageRecord(
    val id: Long = 0,
    val historyId: Long = 0,
    val createdAt: Long,
    val provider: String,
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val cachedTokens: Int = 0,
    val costMicro: Long,     // 费用（微元，1 元 = 1_000_000 微元，避免浮点）
    val targetPackage: String,
    val status: UsageStatus,
)

/**
 * 时间范围内用量汇总（v2.2 §13.3.6）。
 */
data class UsageSummary(
    val callCount: Int = 0,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val costMicro: Long = 0,
    val successCount: Int = 0,
    val failCount: Int = 0,
) {
    /** 平均每次费用（元）。 */
    val avgCostYuan: Double
        get() = if (callCount == 0) 0.0 else costMicro / 1_000_000.0 / callCount
}

/**
 * 按模型聚合（v2.2 §13.3.6）。
 */
data class UsageByModel(
    val provider: String,
    val model: String,
    val callCount: Int,
    val totalTokens: Int,
    val costMicro: Long,
)

/**
 * 按 App 聚合（v2.2 §13.3.6）。
 */
data class UsageByApp(
    val targetPackage: String,
    val callCount: Int,
    val totalTokens: Int,
    val costMicro: Long,
)
