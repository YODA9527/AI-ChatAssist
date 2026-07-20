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
 * version 1 → 2：新增 usage / pricing 两张表。沿用 `fallbackToDestructiveMigration()`，
 * 升级时开发期数据清空可接受（不写迁移脚本）。
 */
@Database(
    entities = [
        HistoryEntity::class,
        UsageEntity::class,
        PricingEntity::class,
    ],
    version = 2,
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
