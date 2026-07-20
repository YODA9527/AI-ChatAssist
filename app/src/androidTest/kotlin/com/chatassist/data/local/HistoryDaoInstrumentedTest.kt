/*
 * v0.15 模块 13：HistoryDao Room 集成测试
 *
 * 目标：验证 v0.7 接入的 Room 历史持久化在真机上的读写
 * 工具：HiltAndroidRule + inMemoryDatabaseBuilder
 *
 * 单元测试已覆盖 ChatParser/HistoryRecord 映射（v0.10 52 单测），这里补 IO 部分。
 */
package com.chatassist.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chatassist.data.local.dao.HistoryDao
import com.chatassist.data.local.entity.HistoryEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HistoryDaoInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var db: HistoryDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, HistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.historyDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndObserve_roundTrip() = runTest {
        val entity = HistoryEntity(
            id = 0, // autoGenerate
            capturedAt = 1_700_000_000L,
            targetPackage = "com.example.chat",
            messagesJson = "[]",
            promptJson = "{}",
            responseText = "建议：好的收到",
            promptTokens = 10,
            completionTokens = 5,
            totalTokens = 15,
            cost = 0.0001,
        )
        val newId = dao.insert(entity)
        assertTrue("id must be auto-generated > 0", newId > 0)

        val all = dao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("com.example.chat", all.first().targetPackage)
        assertEquals(15, all.first().totalTokens)
    }

    @Test
    fun deleteById_removesRecord() = runTest {
        val entity = HistoryEntity(
            id = 0,
            capturedAt = 1_700_000_000L,
            targetPackage = "com.example.chat",
            messagesJson = "[]",
            promptJson = "{}",
            responseText = "x",
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            cost = 0.0,
        )
        val newId = dao.insert(entity)
        dao.deleteById(newId)
        val all = dao.observeAll().first()
        assertEquals(0, all.size)
    }

    @Test
    fun clearAll_emptiesTable() = runTest {
        repeat(3) { i ->
            dao.insert(
                HistoryEntity(
                    id = 0,
                    capturedAt = 1_700_000_000L + i,
                    targetPackage = "com.example.chat",
                    messagesJson = "[]",
                    promptJson = "{}",
                    responseText = "x$i",
                    promptTokens = 0,
                    completionTokens = 0,
                    totalTokens = 0,
                    cost = 0.0,
                ),
            )
        }
        assertEquals(3, dao.observeAll().first().size)
        dao.clearAll()
        assertEquals(0, dao.observeAll().first().size)
    }
}
