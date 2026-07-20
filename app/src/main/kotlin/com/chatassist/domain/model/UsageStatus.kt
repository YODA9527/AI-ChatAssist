package com.chatassist.domain.model

/**
 * 单次模型调用的计费记录状态（v2.2 §13.3 计费模块）。
 *
 * 即使调用失败也落库（[FAILED] / [CANCELLED]），便于统计成功率与失败成本。
 */
enum class UsageStatus { SUCCESS, FAILED, CANCELLED }
