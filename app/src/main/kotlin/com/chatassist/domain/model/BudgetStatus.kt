package com.chatassist.domain.model

/**
 * 预算状态（v2.2 §13.3.7）。
 *
 * - [OK]          预算内
 * - [NEAR_LIMIT]  达 80%（警告，发通知）
 * - [EXCEEDED_WARN]  超 100% 但未开启拦截（警告，发通知）
 * - [EXCEEDED_BLOCK] 超 100% 且开启拦截（阻止生成，用户可见）
 */
enum class BudgetStatus { OK, NEAR_LIMIT, EXCEEDED_WARN, EXCEEDED_BLOCK }
