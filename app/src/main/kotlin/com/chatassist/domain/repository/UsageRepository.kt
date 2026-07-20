package com.chatassist.domain.repository

import com.chatassist.domain.model.UsageByApp
import com.chatassist.domain.model.UsageByModel
import com.chatassist.domain.model.UsageRecord
import com.chatassist.domain.model.UsageSummary
import kotlinx.coroutines.flow.Flow

/**
 * 计费记录仓库接口（v2.2 §13.3）。
 *
 * UI / UseCase 通过此接口访问用量统计，不直接碰 Room。
 * 实现位于 [com.chatassist.data.local.UsageRepositoryImpl]。
 */
interface UsageRepository {
    /** 写入一条记录，返回自增 id。 */
    suspend fun record(record: UsageRecord): Long

    /** 全部记录（按时间倒序）。 */
    fun observeAll(): Flow<List<UsageRecord>>

    /** 分页记录。 */
    fun observePage(limit: Int, offset: Int): Flow<List<UsageRecord>>

    /** 时间范围内汇总（响应式）。 */
    fun observeSummary(start: Long, end: Long): Flow<UsageSummary>

    /** 按模型聚合（响应式）。 */
    fun observeByModel(start: Long, end: Long): Flow<List<UsageByModel>>

    /** 按 App 聚合（响应式）。 */
    fun observeByApp(start: Long, end: Long): Flow<List<UsageByApp>>

    /** 时间范围内总费用（微元，挂起）。 */
    suspend fun totalCost(start: Long, end: Long): Long

    /** 一键清空。 */
    suspend fun clearAll()

    /** 删除 before 之前的记录。 */
    suspend fun deleteOlderThan(before: Long): Int
}
