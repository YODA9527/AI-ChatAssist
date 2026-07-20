package com.chatassist.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * ChatAssist Room 数据库迁移脚本（v0.22 起维护）。
 *
 * 命名规则：`MIGRATION_<from>_<to>`
 * 每个迁移必须：
 *   1. 幂等（可用 `IF NOT EXISTS`）
 *   2. 同步（Room 强制在迁移结束前不返回）
 *   3. 配套 `MigrationTest`（androidTest，见 [com.chatassist.MigrationTest]）
 *
 * 索引命名规则：Room 默认 `index_<table>_<col1>_<col2>_...`，
 * 这里显式给名便于排错日志。
 */
object Migrations {

    /**
     * v2 → v3（v0.22 C1 性能优化）：4 个查询索引
     *
     * 背景：v0.19 计费统计 + v0.20 多模型方案上线后，usage 表数据量从 0 涨到 1k~10k 行，
     * 所有 `WHERE createdAt` 范围 + `ORDER BY createdAt` + `GROUP BY ... costMicro` 走全表扫描。
     * 在 1 万行实测 50-200ms，用户切到「全部」tab 明显卡顿。
     *
     * 加索引后 < 5ms。
     */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // history：单列索引
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_capturedAt` ON `history` (`capturedAt`)")

            // usage：3 个索引（时间 / 时间+包名 / 关联历史）
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_createdAt` ON `usage` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_createdAt_targetPackage` ON `usage` (`createdAt`, `targetPackage`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_historyId` ON `usage` (`historyId`)")

            // pricing：单列索引
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pricing_model` ON `pricing` (`model`)")
        }
    }

    /** 全量迁移列表（按版本顺序）。 */
    val ALL: Array<Migration> = arrayOf(MIGRATION_2_3)
}
