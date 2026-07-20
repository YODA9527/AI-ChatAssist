package com.chatassist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatassist.data.local.entity.UsageByApp
import com.chatassist.data.local.entity.UsageByModel
import com.chatassist.data.local.entity.UsageEntity
import com.chatassist.data.local.entity.UsageSummary
import kotlinx.coroutines.flow.Flow

/**
 * 计费记录 DAO（v2.2 §13.3.6）。
 */
@Dao
interface UsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: UsageEntity): Long

    @Query("SELECT * FROM usage ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun observePage(limit: Int, offset: Int): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UsageEntity>>

    /** 时间范围内汇总（成功/失败分别计数）。 */
    @Query(
        """
        SELECT
            COUNT(*) as callCount,
            COALESCE(SUM(promptTokens),0) as promptTokens,
            COALESCE(SUM(completionTokens),0) as completionTokens,
            COALESCE(SUM(totalTokens),0) as totalTokens,
            COALESCE(SUM(costMicro),0) as costMicro,
            COALESCE(SUM(CASE WHEN status='SUCCESS' THEN 1 ELSE 0 END),0) as successCount,
            COALESCE(SUM(CASE WHEN status='FAILED' THEN 1 ELSE 0 END),0) as failCount
        FROM usage WHERE createdAt >= :start AND createdAt < :end
        """,
    )
    fun observeSummary(start: Long, end: Long): Flow<UsageSummary>

    /** 按模型聚合。 */
    @Query(
        """
        SELECT provider, model,
            COUNT(*) as callCount,
            SUM(totalTokens) as totalTokens,
            SUM(costMicro) as costMicro
        FROM usage WHERE createdAt >= :start AND createdAt < :end
        GROUP BY provider, model ORDER BY costMicro DESC
        """,
    )
    fun observeByModel(start: Long, end: Long): Flow<List<UsageByModel>>

    /** 按 App 聚合。 */
    @Query(
        """
        SELECT targetPackage,
            COUNT(*) as callCount,
            SUM(totalTokens) as totalTokens,
            SUM(costMicro) as costMicro
        FROM usage WHERE createdAt >= :start AND createdAt < :end
        GROUP BY targetPackage ORDER BY costMicro DESC
        """,
    )
    fun observeByApp(start: Long, end: Long): Flow<List<UsageByApp>>

    @Query("SELECT COALESCE(SUM(costMicro),0) FROM usage WHERE createdAt >= :start AND createdAt < :end")
    suspend fun totalCost(start: Long, end: Long): Long

    @Query("DELETE FROM usage WHERE createdAt < :before")
    suspend fun deleteOlderThan(before: Long): Int

    @Query("DELETE FROM usage")
    suspend fun clearAll()
}
