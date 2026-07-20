package com.chatassist.domain.model

import kotlinx.serialization.Serializable

/**
 * 月度预算配置（v2.2 §13.3.7 预算提醒）。
 *
 * 存入 [AppConfig]，随 DataStore 持久化。默认值与指南 §13.4 配置表一致：
 * - 预算 0 = 不设预算（不限）
 * - 80% / 100% 默认提醒
 * - 超额默认不拦截（仅警告）
 */
@Serializable
data class BudgetConfig(
    val monthlyBudgetYuan: Double = 0.0,    // 月度预算（元），0 = 不设
    val alertAt80Percent: Boolean = true,   // 达 80% 提醒
    val alertAt100Percent: Boolean = true,  // 达 100% 提醒
    val blockAtExceed: Boolean = false,     // 超额是否拦截生成
)
