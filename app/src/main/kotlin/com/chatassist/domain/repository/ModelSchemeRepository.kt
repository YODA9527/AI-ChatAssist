package com.chatassist.domain.repository

import com.chatassist.domain.model.ModelScheme
import kotlinx.coroutines.flow.Flow

/**
 * 模型配置方案仓库（v2.2 §13.4）。
 *
 * 方案列表存于 [com.chatassist.domain.model.AppConfig]（DataStore），
 * 每个模型的 API Key 走 [com.chatassist.data.local.security.SecureKeyStorage] 加密，
 * 模型价格同步进 [PricingRepository]（运行时计费按 provider::model 取价）。
 *
 * UI / ViewModel 只依赖此接口，不直接碰配置/加密/定价三套存储。
 */
interface ModelSchemeRepository {
    /** 全部方案（响应式）。 */
    fun observeAll(): Flow<List<ModelScheme>>

    /** 全部方案快照。 */
    suspend fun snapshotAll(): List<ModelScheme>

    /** 当前激活方案（无方案返回出厂默认）。 */
    suspend fun getActive(): ModelScheme

    /**
     * 保存方案（新增或更新）。同时把文本/多模态两个模型的价格同步进定价表，
     * 供计费统计使用。
     */
    suspend fun save(scheme: ModelScheme)

    /** 删除方案（并清空其两个模型的加密 key）。若删除的是当前方案，自动激活下一个。 */
    suspend fun delete(id: String)

    /** 设为当前激活方案。 */
    suspend fun setActive(id: String)
}
