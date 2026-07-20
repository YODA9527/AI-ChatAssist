package com.chatassist.presentation.history

import app.cash.turbine.test
import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.HistoryRecord
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.HistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * HistoryViewModel 单测（v0.14 全量测试，P2 补 ViewModel 覆盖）。
 *
 * 覆盖：state 合并（records + config）+ delete + clearAll + runCleanup（含 maxAgeDays<=0 边界）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var configRepo: ConfigRepository
    private lateinit var historyRepo: HistoryRepository
    private val configFlow = MutableStateFlow(AppConfig())
    private val record1 = HistoryRecord(
        id = 1L,
        capturedAt = 1000L,
        targetPackage = "com.tencent.mm",
        messages = listOf(ChatMessage(id = "1", content = "hi", isSelf = false, sender = "对方")),
        responseText = "你好",
        usage = com.chatassist.domain.model.TokenUsage.of(prompt = 100, completion = 50),
        cost = 0.0,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        configRepo = mockk(relaxed = true)
        historyRepo = mockk(relaxed = true)
        every { configRepo.observeAll() } returns configFlow
        coEvery { configRepo.snapshot() } returns AppConfig()
        coEvery { historyRepo.deleteById(any()) } returns Unit
        coEvery { historyRepo.clearAll() } returns Unit
        coEvery { historyRepo.cleanup(any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始 records 为空 isEmpty true`() = runTest {
        every { historyRepo.observeAll() } returns flowOf(emptyList())
        val vm = HistoryViewModel(configRepo, historyRepo)
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue(s.isEmpty)
        assertEquals(emptyList<HistoryRecord>(), s.records)
    }

    @Test
    fun `records 变化触发 state 更新`() = runTest {
        val recordsFlow = MutableStateFlow(emptyList<HistoryRecord>())
        every { historyRepo.observeAll() } returns recordsFlow
        val vm = HistoryViewModel(configRepo, historyRepo)
        advanceUntilIdle()

        recordsFlow.value = listOf(record1)
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(1, s.records.size)
        assertEquals(1L, s.records[0].id)
        assertEquals(false, s.isEmpty)
    }

    @Test
    fun `config 变化同步到 state privacyMode maxAgeDays maxCount`() = runTest {
        every { historyRepo.observeAll() } returns flowOf(listOf(record1))
        val vm = HistoryViewModel(configRepo, historyRepo)
        advanceUntilIdle()

        configFlow.value = AppConfig(
            privacyMode = true,
            historyMaxAgeDays = 7,
            historyMaxCount = 100,
        )
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue(s.privacyMode)
        assertEquals(7, s.maxAgeDays)
        assertEquals(100, s.maxCount)
    }

    @Test
    fun `delete 调用 historyRepo deleteById`() = runTest {
        every { historyRepo.observeAll() } returns flowOf(emptyList())
        val vm = HistoryViewModel(configRepo, historyRepo)
        advanceUntilIdle()

        vm.delete(42L)
        advanceUntilIdle()

        coVerify { historyRepo.deleteById(42L) }
    }

    @Test
    fun `clearAll 调用 historyRepo clearAll`() = runTest {
        every { historyRepo.observeAll() } returns flowOf(emptyList())
        val vm = HistoryViewModel(configRepo, historyRepo)
        advanceUntilIdle()

        vm.clearAll()
        advanceUntilIdle()

        coVerify { historyRepo.clearAll() }
    }

    @Test
    fun `runCleanup maxAgeDays 大于 0 用 currentTimeMillis 减天数`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(historyMaxAgeDays = 30, historyMaxCount = 1000)
        every { historyRepo.observeAll() } returns flowOf(emptyList())
        val vm = HistoryViewModel(configRepo, historyRepo)
        advanceUntilIdle()

        val before = System.currentTimeMillis()
        vm.runCleanup()
        advanceUntilIdle()
        val after = System.currentTimeMillis()

        // olderThan 应该在 (before - 30 * 86_400_000) ~ (after - 30 * 86_400_000) 之间
        coVerify {
            historyRepo.cleanup(
                match { it in (before - 30L * 86_400_000L - 1)..(after - 30L * 86_400_000L + 1) },
                1000,
            )
        }
    }

    @Test
    fun `runCleanup maxAgeDays 小于等于 0 olderThan 为 0`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(historyMaxAgeDays = 0, historyMaxCount = 500)
        every { historyRepo.observeAll() } returns flowOf(emptyList())
        val vm = HistoryViewModel(configRepo, historyRepo)
        advanceUntilIdle()

        vm.runCleanup()
        advanceUntilIdle()

        coVerify { historyRepo.cleanup(0L, 500) }
    }
}
