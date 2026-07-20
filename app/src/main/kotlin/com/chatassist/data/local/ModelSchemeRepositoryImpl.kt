package com.chatassist.data.local

import com.chatassist.domain.model.ModelConfig
import com.chatassist.domain.model.ModelScheme
import com.chatassist.domain.model.Pricing
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.ModelSchemeRepository
import com.chatassist.domain.repository.PricingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 模型配置方案仓库实现（v2.2 §13.4）。
 *
 * - 列表：经 [ConfigRepository] 读写 AppConfig.schemes
 * - API Key：经 [ConfigRepository.saveModelApiKey]/[ConfigRepository.getModelApiKey] 走加密通道
 * - 价格：保存时同步 upsert 进 [PricingRepository]（key = provider::model）
 */
@Singleton
class ModelSchemeRepositoryImpl @Inject constructor(
    private val configRepo: ConfigRepository,
    private val pricingRepo: PricingRepository,
) : ModelSchemeRepository {

    override fun observeAll(): Flow<List<ModelScheme>> =
        configRepo.observeAll().map { it.schemes }

    override suspend fun snapshotAll(): List<ModelScheme> = configRepo.snapshot().schemes

    override suspend fun getActive(): ModelScheme = configRepo.snapshot().activeScheme()

    override suspend fun save(scheme: ModelScheme) {
        configRepo.update { cfg ->
            val others = cfg.schemes.filter { it.id != scheme.id }
            cfg.copy(schemes = others + scheme.copy(updatedAt = System.currentTimeMillis()))
        }
        syncPricing(scheme.textModel)
        syncPricing(scheme.multimodalModel)
    }

    override suspend fun delete(id: String) {
        configRepo.update { cfg ->
            val remaining = cfg.schemes.filter { it.id != id }
            val newActive = if (cfg.activeSchemeId == id) {
                remaining.firstOrNull()?.id ?: ""
            } else {
                cfg.activeSchemeId
            }
            cfg.copy(schemes = remaining, activeSchemeId = newActive)
        }
        configRepo.saveModelApiKey("$id:text", "")
        configRepo.saveModelApiKey("$id:multimodal", "")
    }

    override suspend fun setActive(id: String) {
        configRepo.update { cfg ->
            cfg.copy(
                activeSchemeId = id,
                schemes = cfg.schemes.map { it.copy(isActive = it.id == id) },
            )
        }
    }

    /** 把模型价格同步进定价表（模型名为空则跳过）。 */
    private suspend fun syncPricing(m: ModelConfig) {
        if (m.model.isBlank()) return
        pricingRepo.upsert(
            Pricing(
                key = m.pricingKey(),
                provider = m.provider,
                model = m.model,
                inputPricePerMToken = m.inputPricePerMToken,
                outputPricePerMToken = m.outputPricePerMToken,
                cachedPricePerMToken = m.cachedPricePerMToken,
                updatedAt = System.currentTimeMillis(),
                isBuiltin = false,
            ),
        )
    }
}
