package com.chatassist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chatassist.data.local.dao.HistoryDao
import com.chatassist.data.local.dao.PricingDao
import com.chatassist.data.local.dao.UsageDao
import com.chatassist.data.local.entity.HistoryEntity
import com.chatassist.data.local.entity.PricingEntity
import com.chatassist.data.local.entity.UsageEntity

/**
 * 历史记录数据库（模块 09），v2.2 计费模块接入后扩展为三表：
 * - history（历史对话）
 * - usage（计费记录，§13.3.2）
 * - pricing（模型定价，§13.3.3）
 *
 * 版本演进：
 * - v1 → v2：新增 usage / pricing 两张表（v0.19 计费统计）
 * - v2 → v3（v0.22 性能优化 C1）：加 4 个索引（详见各 Entity 注释）
 *   - history.capturedAt
 *   - usage.createdAt
 *   - usage.(createdAt, targetPackage)
 *   - usage.historyId
 *   - pricing.model
 *
 * v2 → v3 迁移用 [com.chatassist.data.local.Migrations.MIGRATION_2_3]，
 * 替换了 `fallbackToDestructiveMigration()`，升级不再清空老用户数据。
 */
@Database(
    entities = [
        HistoryEntity::class,
        UsageEntity::class,
        PricingEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun usageDao(): UsageDao
    abstract fun pricingDao(): PricingDao

    companion object {
        const val NAME = "chatassist_history"
    }
}
