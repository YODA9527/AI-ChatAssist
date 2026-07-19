package com.chatassist.domain.repository

import com.chatassist.domain.model.HistoryRecord
import com.chatassist.domain.model.HistoryRecordInput
import kotlinx.coroutines.flow.Flow

/**
 * 历史记录仓库接口（模块 09）。
 *
 * 任何业务层访问历史都通过此接口，UI 层不准直接访问 Room。
 * 实现位于 [com.chatassist.data.local.HistoryRepositoryImpl]。
 */
interface HistoryRepository {
    /** 写入一条记录，返回自增 id。 */
    suspend fun record(input: HistoryRecordInput): Long

    /** 全部记录（按时间倒序，响应式）。 */
    fun observeAll(): Flow<List<HistoryRecord>>

    /** 按 id 删除。 */
    suspend fun deleteById(id: Long)

    /** 一键清空。 */
    suspend fun clearAll()

    /** 保留策略：保留最近 keepLast 条 + 删除 olderThan 之前的（毫秒时间戳）。 */
    suspend fun cleanup(olderThan: Long, keepLast: Int)
}
