package com.chatassist.domain.repository

import com.chatassist.domain.model.Pricing
import com.chatassist.domain.model.TokenUsage
import kotlinx.coroutines.flow.Flow

/**
 * 模型定价仓库接口（v2.2 §13.3.3）。
 *
 * UI / UseCase 通过此接口读写定价与计算费用，不直接碰 Room。
 * 实现位于 [com.chatassist.data.local.PricingRepositoryImpl]。
 */
interface PricingRepository {
    /** 全部定价（响应式）。 */
    fun observeAll(): Flow<List<Pricing>>

    /** 按主键取。 */
    suspend fun get(key: String): Pricing?

    /** 按模型名取（provider 无关兜底）。 */
    suspend fun getByModel(model: String): Pricing?

    /** 新增 / 更新。 */
    suspend fun upsert(pricing: Pricing)

    /** 删除（按主键）。 */
    suspend fun delete(key: String)

    /** 恢复内置定价（清表后重写 BUILTIN_PRICING）。 */
    suspend fun resetBuiltin()

    /**
     * 计算单次调用费用（微元）。
     *
     * 优先 `$provider::$model`（provider 由模型名推导），
     * 失败则按模型名兜底；都查不到返回 0（不计费，不报错）。
     */
    suspend fun calculateCost(usage: TokenUsage, model: String): Long
}
