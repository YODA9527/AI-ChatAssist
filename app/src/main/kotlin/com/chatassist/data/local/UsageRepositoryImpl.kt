package com.chatassist.data.local

import com.chatassist.data.local.dao.UsageDao
import com.chatassist.data.local.entity.UsageEntity
import com.chatassist.domain.model.UsageByApp
import com.chatassist.domain.model.UsageByModel
import com.chatassist.domain.model.UsageRecord
import com.chatassist.domain.model.UsageStatus
import com.chatassist.domain.model.UsageSummary
import com.chatassist.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 计费记录仓库实现（v2.2 §13.3）。
 *
 * 实体 ↔ domain 映射在此完成；status 以字符串存库（枚举名）。
 */
@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val dao: UsageDao,
) : UsageRepository {

    override suspend fun record(record: UsageRecord): Long = dao.insert(record.toEntity())

    override fun observeAll(): Flow<List<UsageRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observePage(limit: Int, offset: Int): Flow<List<UsageRecord>> =
        dao.observePage(limit, offset).map { list -> list.map { it.toDomain() } }

    override fun observeSummary(start: Long, end: Long): Flow<UsageSummary> =
        dao.observeSummary(start, end).map { it.toDomain() }

    override fun observeByModel(start: Long, end: Long): Flow<List<UsageByModel>> =
        dao.observeByModel(start, end).map { list -> list.map { it.toDomain() } }

    override fun observeByApp(start: Long, end: Long): Flow<List<UsageByApp>> =
        dao.observeByApp(start, end).map { list -> list.map { it.toDomain() } }

    override suspend fun totalCost(start: Long, end: Long): Long = dao.totalCost(start, end)

    override suspend fun clearAll() = dao.clearAll()

    override suspend fun deleteOlderThan(before: Long): Int = dao.deleteOlderThan(before)

    // ====================== 映射 ======================

    private fun UsageRecord.toEntity() = UsageEntity(
        id = id,
        historyId = historyId,
        createdAt = createdAt,
        provider = provider,
        model = model,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        cachedTokens = cachedTokens,
        costMicro = costMicro,
        targetPackage = targetPackage,
        status = status.name,
    )

    private fun UsageEntity.toDomain() = UsageRecord(
        id = id,
        historyId = historyId,
        createdAt = createdAt,
        provider = provider,
        model = model,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        cachedTokens = cachedTokens,
        costMicro = costMicro,
        targetPackage = targetPackage,
        status = runCatching { UsageStatus.valueOf(status) }.getOrDefault(UsageStatus.SUCCESS),
    )

    private fun com.chatassist.data.local.entity.UsageSummary.toDomain() = UsageSummary(
        callCount = callCount,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        costMicro = costMicro,
        successCount = successCount,
        failCount = failCount,
    )

    private fun com.chatassist.data.local.entity.UsageByModel.toDomain() = UsageByModel(
        provider = provider,
        model = model,
        callCount = callCount,
        totalTokens = totalTokens,
        costMicro = costMicro,
    )

    private fun com.chatassist.data.local.entity.UsageByApp.toDomain() = UsageByApp(
        targetPackage = targetPackage,
        callCount = callCount,
        totalTokens = totalTokens,
        costMicro = costMicro,
    )
}
