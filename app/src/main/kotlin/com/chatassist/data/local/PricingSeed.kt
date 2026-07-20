package com.chatassist.data.local

import com.chatassist.data.local.entity.PricingEntity
import com.chatassist.domain.model.BUILTIN_PRESETS
import com.chatassist.domain.model.Pricing

/**
 * 内置定价表（v2.2 §13.3.3 / §13.4 / v0.21）。
 *
 * **v0.21 起改为由 [BUILTIN_PRESETS] 直接派生**，不再手写重复列表——
 * 预设目录即唯一数据源，种子价 = 每个预设模型按 `deriveProvider` 推导出的
 * `provider::model` 落库（与 [Pricing.keyOf] 一致）。这样预设/种子价永远对齐，
 * 杜绝 v0.20 时期「预设 12 条、种子价 7 条」的双源漂移雷。
 *
 * 仅作初始值；用户可在模型方案编辑页内联修改 / 覆盖（v0.20 起已无独立「定价管理」页）。
 * 保存方案时 [com.chatassist.data.local.ModelSchemeRepositoryImpl] 会把方案中两模型的价格 upsert 进此表。
 * 价格单位：元 / 每百万 token，与预设目录同源（初夏 2026-07-20 联网核对官方文档）。
 */
val BUILTIN_PRICING: List<PricingEntity> = BUILTIN_PRESETS.map { p ->
    PricingEntity(
        key = Pricing.keyOf(p.provider, p.model),
        provider = p.provider,
        model = p.model,
        inputPricePerMToken = p.inputPricePerMToken,
        outputPricePerMToken = p.outputPricePerMToken,
        cachedPricePerMToken = p.cachedPricePerMToken,
        updatedAt = 0,
        isBuiltin = true,
    )
}
