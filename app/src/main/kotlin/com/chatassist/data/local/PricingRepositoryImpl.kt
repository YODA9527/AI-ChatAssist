package com.chatassist.data.local

import com.chatassist.data.local.dao.PricingDao
import com.chatassist.data.local.entity.PricingEntity
import com.chatassist.domain.model.Pricing
import com.chatassist.domain.model.TokenUsage
import com.chatassist.domain.model.deriveProvider
import com.chatassist.domain.repository.PricingRepository
import com.chatassist.domain.usecase.CostCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 模型定价仓库实现（v2.2 §13.3.3）。
 *
 * - 首次访问时惰性写入内置定价（[BUILTIN_PRICING]），仅补缺不覆盖用户改动。
 * - [calculateCost] 优先按 `$provider::$model` 取价（provider 由模型名推导），
 *   失败按模型名兜底，都查不到返回 0（不计费、不报错）。
 */
@Singleton
class PricingRepositoryImpl @Inject constructor(
    private val dao: PricingDao,
) : PricingRepository {

    private val seeded = AtomicBoolean(false)

    override fun observeAll(): Flow<List<Pricing>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(key: String): Pricing? = ensureSeeded().let { dao.get(key)?.toDomain() }

    override suspend fun getByModel(model: String): Pricing? =
        ensureSeeded().let { dao.getByModel(model)?.toDomain() }

    override suspend fun upsert(pricing: Pricing) {
        ensureSeeded()
        dao.upsert(pricing.toEntity())
    }

    override suspend fun delete(key: String) = dao.delete(key)

    override suspend fun resetBuiltin() {
        dao.clearAll()
        BUILTIN_PRICING.forEach { dao.upsert(it) }
        seeded.set(true)
    }

    override suspend fun calculateCost(usage: TokenUsage, model: String): Long {
        ensureSeeded()
        val pricing = runCatching {
            val provider = deriveProvider(model)
            dao.get(Pricing.keyOf(provider, model)) ?: dao.getByModel(model)
        }.getOrNull() ?: return 0L
        return CostCalculator.calculate(usage, pricing.toDomain())
    }

    /** 首次访问惰性补齐内置定价（仅当该 key 不存在）。 */
    private suspend fun ensureSeeded() {
        if (seeded.compareAndSet(false, true)) {
            runCatching {
                BUILTIN_PRICING.forEach { builtin ->
                    if (dao.get(builtin.key) == null) dao.upsert(builtin)
                }
            }.onFailure { Timber.w(it, "内置定价初始化失败（已忽略）") }
        }
    }

    private fun PricingEntity.toDomain() = Pricing(
        key = key,
        provider = provider,
        model = model,
        inputPricePerMToken = inputPricePerMToken,
        outputPricePerMToken = outputPricePerMToken,
        cachedPricePerMToken = cachedPricePerMToken,
        currency = currency,
        updatedAt = updatedAt,
        isBuiltin = isBuiltin,
    )

    private fun Pricing.toEntity() = PricingEntity(
        key = key,
        provider = provider,
        model = model,
        inputPricePerMToken = inputPricePerMToken,
        outputPricePerMToken = outputPricePerMToken,
        cachedPricePerMToken = cachedPricePerMToken,
        currency = currency,
        updatedAt = updatedAt,
        isBuiltin = isBuiltin,
    )
}
