package com.chatassist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatassist.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

/** 历史记录 DAO（模块 09）。 */
@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity): Long

    @Query("SELECT * FROM history ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    /** 保留最近 keepLast 条，其余删除。 */
    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY capturedAt DESC LIMIT :keepLast)")
    suspend fun deleteExceptLatest(keepLast: Int)

    /** 删除 olderThan 之前、且不在最近 keepLast 条内的记录（保留策略兜底）。 */
    @Query(
        "DELETE FROM history WHERE capturedAt < :olderThan " +
            "AND id NOT IN (SELECT id FROM history ORDER BY capturedAt DESC LIMIT :keepLast)",
    )
    suspend fun deleteOlderThan(keepLast: Int, olderThan: Long)
}
