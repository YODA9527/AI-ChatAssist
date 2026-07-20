package com.chatassist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatassist.data.local.entity.PricingEntity
import kotlinx.coroutines.flow.Flow

/**
 * 模型定价 DAO（v2.2 §13.3.3）。
 */
@Dao
interface PricingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: PricingEntity)

    @Query("SELECT * FROM pricing ORDER BY provider, model")
    fun observeAll(): Flow<List<PricingEntity>>

    @Query("SELECT * FROM pricing WHERE `key` = :key")
    suspend fun get(key: String): PricingEntity?

    /** 按模型名查找（provider 无关），用于用户自填模型时的兜底匹配。 */
    @Query("SELECT * FROM pricing WHERE model = :model LIMIT 1")
    suspend fun getByModel(model: String): PricingEntity?

    @Query("DELETE FROM pricing WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM pricing")
    suspend fun clearAll()
}
