package com.chatassist.domain.repository

import com.chatassist.domain.model.AppConfig
import kotlinx.coroutines.flow.Flow

/**
 * 应用配置仓库接口。
 *
 * 任何业务层访问配置都通过此接口，UI 层不准直接访问 DataStore。
 * 实现位于 [com.chatassist.data.local.ConfigRepositoryImpl]。
 */
interface ConfigRepository {
    /** 全部配置（响应式）。 */
    fun observeAll(): Flow<AppConfig>

    /** 当前配置快照。 */
    suspend fun snapshot(): AppConfig

    /**
     * 更新配置（lambda 风格：原子修改）。
     *
     * 用法：
     * ```
     * configRepo.update { it.copy(apiKey = "xxx", model = "gpt-4o") }
     * ```
     */
    suspend fun update(transform: (AppConfig) -> AppConfig)

    /**
     * 测试 API 连接。
     *
     * @return 成功 Result.success(模型响应耗时ms)；失败 Result.failure(AppError)
     */
    suspend fun testConnection(): Result<Long>

    /**
     * 保存某模型别名的 API Key（加密存储，不进 DataStore）。
     * 别名形如 `{schemeId}:text` / `{schemeId}:multimodal`。
     */
    suspend fun saveModelApiKey(alias: String, key: String)

    /** 读取某模型别名的 API Key（空串表示未配置）。 */
    fun getModelApiKey(alias: String): String

    /**
     * 老用户迁移：若尚无方案，则用旧全局单模型配置（apiUrl/apiKey/model）合成默认方案。
     * 幂等——已存在方案则直接返回。应在 App 启动时调用一次。
     */
    suspend fun migrateLegacySchemes()
}
