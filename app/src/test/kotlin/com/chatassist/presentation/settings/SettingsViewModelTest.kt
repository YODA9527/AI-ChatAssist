package com.chatassist.presentation.settings

import app.cash.turbine.test
import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.repository.ConfigRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * SettingsViewModel 单测（v0.14 全量测试，P2 补 ViewModel 覆盖）。
 *
 * 覆盖：state 加载 + API Key 更新 + 通用 update + testConnection 4 个分支 + dismissTestResult。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var configRepo: ConfigRepository
    private val configFlow = MutableStateFlow(AppConfig())

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        configRepo = mockk(relaxed = true)
        every { configRepo.observeAll() } returns configFlow
        coEvery { configRepo.update(any<(AppConfig) -> AppConfig>()) } returns Unit
        coEvery { configRepo.snapshot() } returns AppConfig()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始状态为 Loading config 流出后变 Loaded`() = runTest {
        val vm = SettingsViewModel(configRepo)
        assertEquals(SettingsUiState.Loading, vm.state.value)

        configFlow.value = AppConfig(apiUrl = "https://api.deepseek.com/v1", model = "deepseek-chat")
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is SettingsUiState.Loaded, "expected Loaded, got $state")
        assertEquals("https://api.deepseek.com/v1", (state as SettingsUiState.Loaded).config.apiUrl)
    }

    @Test
    fun `updateApiKey 调用 configRepo update`() = runTest {
        val vm = SettingsViewModel(configRepo)
        val transform = slot<(AppConfig) -> AppConfig>()

        coEvery { configRepo.update(capture(transform)) } returns Unit
        vm.updateApiKey("sk-test-1234567890abcdef")
        advanceUntilIdle()

        coVerify { configRepo.update(any()) }
        val cfg = transform.captured(AppConfig())
        assertEquals("sk-test-1234567890abcdef", cfg.apiKey)
    }

    @Test
    fun `update 通用转换器调用 configRepo update`() = runTest {
        val vm = SettingsViewModel(configRepo)
        val transform = slot<(AppConfig) -> AppConfig>()

        coEvery { configRepo.update(capture(transform)) } returns Unit
        vm.update { it.copy(privacyMode = true, themeMode = com.chatassist.domain.model.ThemeMode.DARK) }
        advanceUntilIdle()

        coVerify { configRepo.update(any()) }
        val cfg = transform.captured(AppConfig())
        assertTrue(cfg.privacyMode)
        assertEquals(com.chatassist.domain.model.ThemeMode.DARK, cfg.themeMode)
    }

    @Test
    fun `testConnection 成功 设置 Success 状态`() = runTest {
        coEvery { configRepo.testConnection() } returns Result.success(123L)
        val vm = SettingsViewModel(configRepo)

        vm.testResult.test {
            assertEquals(TestResult.Idle, awaitItem())
            vm.testConnection()
            assertEquals(TestResult.Testing, awaitItem())
            assertEquals(TestResult.Success(123L), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `testConnection 失败 AppError 提取 userMessage`() = runTest {
        val err = AppError.of(
            code = ErrorCode.AUTH_401,
            userMessage = "API Key 无效或已过期",
            technicalDetail = "401 Unauthorized",
        )
        coEvery { configRepo.testConnection() } returns Result.failure(err)
        val vm = SettingsViewModel(configRepo)

        vm.testResult.test {
            assertEquals(TestResult.Idle, awaitItem())
            vm.testConnection()
            assertEquals(TestResult.Testing, awaitItem())
            assertEquals(TestResult.Failed("API Key 无效或已过期"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `testConnection 失败非 AppError 用 message`() = runTest {
        coEvery { configRepo.testConnection() } returns Result.failure(RuntimeException("连接超时"))
        val vm = SettingsViewModel(configRepo)

        vm.testResult.test {
            assertEquals(TestResult.Idle, awaitItem())
            vm.testConnection()
            assertEquals(TestResult.Testing, awaitItem())
            assertEquals(TestResult.Failed("连接超时"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `testConnection 失败 message 为 null 走未知错误`() = runTest {
        coEvery { configRepo.testConnection() } returns Result.failure(RuntimeException())
        val vm = SettingsViewModel(configRepo)

        vm.testResult.test {
            assertEquals(TestResult.Idle, awaitItem())
            vm.testConnection()
            assertEquals(TestResult.Testing, awaitItem())
            assertEquals(TestResult.Failed("未知错误"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissTestResult 重置回 Idle`() = runTest {
        coEvery { configRepo.testConnection() } returns Result.success(50L)
        val vm = SettingsViewModel(configRepo)

        vm.testConnection()
        advanceUntilIdle()
        assertEquals(TestResult.Success(50L), vm.testResult.value)

        vm.dismissTestResult()
        assertEquals(TestResult.Idle, vm.testResult.value)
    }
}
