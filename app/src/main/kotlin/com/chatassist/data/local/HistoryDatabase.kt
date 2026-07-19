package com.chatassist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chatassist.data.local.dao.HistoryDao
import com.chatassist.data.local.entity.HistoryEntity

/** 历史记录数据库（模块 09），当前仅含 history 单表，version = 1。 */
@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "chatassist_history"
    }
}
