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
}
