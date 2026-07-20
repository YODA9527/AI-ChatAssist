package com.chatassist.presentation.main

import android.content.ClipboardManager
import android.content.Context
import com.chatassist.data.permission.PermissionManager
import com.chatassist.domain.capture.CaptureResult
import com.chatassist.domain.capture.CaptureStrategy
import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.PermissionState
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.usecase.CaptureChatUseCase
import com.chatassist.domain.usecase.ReplySuggestionUseCase
import com.chatassist.domain.usecase.ReplyUiState
import com.chatassist.presentation.overlay.OverlayController
import com.chatassist.presentation.overlay.OverlayState
import com.chatassist.util.CoroutineErrorHandler
import com.chatassist.util.GlobalErrorHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
 * MainViewModel 单测（v0.14 全量测试，P2 补 ViewModel 覆盖）。
 *
 * 覆盖：state 加载、permissionSummary、capturedMessages、generateReply 4 个分支（OK / 失败 / 空消息 / 提示）、
 * copySuggestion、refreshPermissions、toggleOverlay。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var configRepo: ConfigRepository
    private lateinit var permissionManager: PermissionManager
    private lateinit var captureUseCase: CaptureChatUseCase
    private lateinit var captureStrategy: CaptureStrategy
    private lateinit var replyUseCase: ReplySuggestionUseCase
    private lateinit var overlay: OverlayController
    private lateinit var appContext: Context
    private lateinit var coroutineErrorHandler: CoroutineErrorHandler
    private lateinit var clipboardManager: ClipboardManager

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        configRepo = mockk(relaxed = true)
        permissionManager = mockk(relaxed = true)
        captureUseCase = mockk(relaxed = true)
        captureStrategy = mockk(relaxed = true)
        replyUseCase = mockk(relaxed = true)
        overlay = mockk(relaxed = true)
        appContext = mockk(relaxed = true)
        coroutineErrorHandler = CoroutineErrorHandler(mockk<GlobalErrorHandler>(relaxed = true))
        clipboardManager = mockk(relaxed = true)

        every { configRepo.observeAll() } returns flowOf(com.chatassist.domain.model.AppConfig())
        coEvery { configRepo.snapshot() } returns com.chatassist.domain.model.AppConfig()
        every { permissionManager.notificationState } returns MutableStateFlow(PermissionState.NotRequested)
        every { permissionManager.overlayState } returns MutableStateFlow(PermissionState.NotRequested)
        every { permissionManager.projectionState } returns MutableStateFlow(PermissionState.NotRequested)
        every { permissionManager.accessibilityState } returns MutableStateFlow(PermissionState.NotRequested)
        every { permissionManager.refresh() } returns Unit
        every { captureUseCase() } returns MutableStateFlow(emptyList())
        every { captureStrategy.currentMode } returns MutableStateFlow(CaptureMode.TEXT_ONLY)
        every { replyUseCase.state } returns MutableStateFlow(ReplyUiState.Idle)
        every { overlay.state } returns MutableStateFlow(OverlayState.Collapsed)
        every { overlay.isShowing() } returns false
        every { overlay.show() } returns Unit
        every { overlay.hide() } returns Unit
        every { overlay.showStreaming(any()) } returns Unit
        every { overlay.showSuggestions(any()) } returns Unit
        every { overlay.showError(any()) } returns Unit
        every { overlay.toast(any()) } returns Unit
        every { appContext.getSystemService(Context.CLIPBOARD_SERVICE) } returns clipboardManager
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state 默认 isApiConfigured false apiUrl 空 model 空 privacyMode false`() = runTest {
        // 显式空配置（无方案、无旧版字段）→ 未配置
        every { configRepo.observeAll() } returns flowOf(
            com.chatassist.domain.model.AppConfig(apiUrl = "", apiKey = "", model = ""),
        )
        val vm = makeVm()
        advanceUntilIdle()
        // 触发 state collect 完成后才检查（stateIn WhileSubscribed 行为）
        val s = vm.state.value
        assertEquals(false, s.isApiConfigured)
        assertEquals("", s.apiUrl)
        assertEquals("", s.model)
        assertEquals(false, s.privacyMode)
    }

    @Test
    fun `state apiKey 非空时 isApiConfigured true`() = runTest {
        val cfg = com.chatassist.domain.model.AppConfig(apiKey = "sk-1234567890")
        every { configRepo.observeAll() } returns flowOf(cfg)
        val vm = makeVm()
        advanceUntilIdle()
        // stateIn WhileSubscribed：第一次 awaitItem 拿 initialValue（默认 state），第二次拿 cfg 转换后的
        vm.state.test {
            skipItems(1)
            val s = awaitItem()
            assertTrue(s.isApiConfigured, "expected isApiConfigured=true, got $s")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `permissionSummary 初始 4 项 NotRequested`() = runTest {
        val vm = makeVm()
        vm.permissionSummary.test {
            val s = awaitItem()
            assertEquals(PermissionState.NotRequested, s.notification)
            assertEquals(PermissionState.NotRequested, s.overlay)
            assertEquals(PermissionState.NotRequested, s.projection)
            assertEquals(PermissionState.NotRequested, s.accessibility)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `capturedMessages 透传 captureUseCase state`() = runTest {
        val msgs = listOf(ChatMessage(id = "1", content = "x", isSelf = true, sender = "我"))
        every { captureUseCase() } returns MutableStateFlow(msgs)
        val vm = makeVm()
        advanceUntilIdle()
        vm.capturedMessages.test {
            // 第一次 awaitItem 拿 initialValue (emptyList)，第二次拿 MutableStateFlow 的 msgs
            skipItems(1)
            val first = awaitItem()
            assertEquals(msgs, first)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `captureMode 透传 captureStrategy currentMode`() = runTest {
        val mode = CaptureMode.OCR_ONLY
        every { captureStrategy.currentMode } returns MutableStateFlow(mode)
        val vm = makeVm()
        vm.captureMode.test {
            val first = awaitItem()
            assertEquals(mode, first)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `generateReply 成功 调 replyUseCase generate`() = runTest {
        val captureResult = CaptureResult(
            messages = listOf(ChatMessage(id = "1", content = "hi", isSelf = false, sender = "对方")),
            mode = CaptureMode.TEXT_ONLY,
            targetPackage = "com.tencent.mm",
        )
        coEvery { captureUseCase.captureOnce() } returns Result.success(captureResult)
        val vm = makeVm()

        vm.generateReply()
        advanceUntilIdle()

        coVerify { replyUseCase.generate(any(), "com.tencent.mm") }
    }

    @Test
    fun `generateReply 捕获失败 调 overlay showError 不调 replyUseCase`() = runTest {
        coEvery { captureUseCase.captureOnce() } returns Result.failure(RuntimeException("no perm"))
        val vm = makeVm()

        vm.generateReply()
        advanceUntilIdle()

        verify { overlay.showError(match { it.contains("no perm") }) }
        coVerify(exactly = 0) { replyUseCase.generate(any<List<ChatMessage>>(), any()) }
    }

    @Test
    fun `generateReply 空消息且无障碍未授权 提示开屏幕录制`() = runTest {
        val emptyResult = CaptureResult(messages = emptyList(), mode = CaptureMode.TEXT_ONLY)
        coEvery { captureUseCase.captureOnce() } returns Result.success(emptyResult)
        every { permissionManager.projectionState } returns MutableStateFlow(PermissionState.NotRequested)
        val vm = makeVm()

        vm.generateReply()
        advanceUntilIdle()

        verify { overlay.showError(match { it.contains("屏幕录制") || it.contains("聊天界面") }) }
        coVerify(exactly = 0) { replyUseCase.generate(any<List<ChatMessage>>(), any()) }
    }

    @Test
    fun `copySuggestion 调 ClipboardManager setPrimaryClip`() {
        val vm = makeVm()
        vm.copySuggestion("要复制的文本")
        verify { clipboardManager.setPrimaryClip(any()) }
    }

    @Test
    fun `refreshPermissions 调 permissionManager refresh`() {
        val vm = makeVm()
        vm.refreshPermissions()
        verify { permissionManager.refresh() }
    }

    @Test
    fun `toggleOverlay 当前显示则 hide 否则 show`() {
        val vm = makeVm()
        every { overlay.isShowing() } returns true
        vm.toggleOverlay()
        verify { overlay.hide() }

        every { overlay.isShowing() } returns false
        vm.toggleOverlay()
        verify { overlay.show() }
    }

    private fun makeVm() = MainViewModel(
        configRepo,
        permissionManager,
        captureUseCase,
        captureStrategy,
        replyUseCase,
        overlay,
        appContext,
        coroutineErrorHandler,
    )
}
