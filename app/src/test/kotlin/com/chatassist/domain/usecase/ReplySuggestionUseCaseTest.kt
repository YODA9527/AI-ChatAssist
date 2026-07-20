package com.chatassist.domain.usecase

import app.cash.turbine.test
import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.BudgetStatus
import com.chatassist.domain.model.BuiltPrompt
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.Message
import com.chatassist.domain.model.MessageRole
import com.chatassist.domain.model.ModelCallEvent
import com.chatassist.domain.model.TokenUsage
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.HistoryRepository
import com.chatassist.domain.repository.ModelApiRepository
import com.chatassist.domain.usecase.BudgetManager
import com.chatassist.domain.usecase.BudgetState
import com.chatassist.domain.usecase.RecordUsageUseCase
import com.chatassist.util.BudgetNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ReplySuggestionUseCase 单测（v0.14 全量测试，P2 补 UseCase 覆盖）。
 *
 * 覆盖：空消息 Error + 正常流式累积 + StreamDone 转 Suggestions + 隐私模式不写历史 +
 * 解析 3 种候选格式 + 最多 5 条。
 */
class ReplySuggestionUseCaseTest {

    private val buildPrompt: BuildPromptUseCase = mockk(relaxed = true)
    private val modelApi: ModelApiRepository = mockk(relaxed = true)
    private val historyRepo: HistoryRepository = mockk(relaxed = true)
    private val configRepo: ConfigRepository = mockk(relaxed = true)
    private val recordUsage: RecordUsageUseCase = mockk(relaxed = true)
    private val budgetManager: BudgetManager = mockk(relaxed = true)
    private val budgetNotifier: BudgetNotifier = mockk(relaxed = true)

    private fun makeUseCase(): ReplySuggestionUseCase {
        // 默认预算状态 OK，避免生成前被「超额拦截」影响既有用例
        coEvery { budgetManager.currentState() } returns BudgetState(BudgetStatus.OK, 0.0, 0.0)
        return ReplySuggestionUseCase(
            buildPrompt,
            modelApi,
            historyRepo,
            configRepo,
            recordUsage,
            budgetManager,
            budgetNotifier,
        )
    }

    private val sampleMessages = listOf(
        ChatMessage(id = "1", content = "你好", isSelf = true, sender = "我"),
    )

    private val samplePrompt = BuiltPrompt(
        systemPrompt = "sys",
        messages = listOf(Message(MessageRole.USER, "user")),
        temperature = 0.7,
        maxTokens = 1024,
        model = "deepseek-chat",
    )

    @Test
    fun `空消息直接置 Error 不调 buildPrompt`() = runTest {
        val useCase = makeUseCase()

        useCase.state.test {
            assertEquals(ReplyUiState.Idle, awaitItem())
            useCase.generate(emptyList())
            val err = awaitItem()
            assertTrue(err is ReplyUiState.Error)
            assertEquals("暂无可分析的对话，先去捕获一些消息吧～", (err as ReplyUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { buildPrompt.invoke(any()) }
    }

    @Test
    fun `buildPrompt 失败时置 Error 不调 modelApi`() = runTest {
        coEvery { buildPrompt.invoke(any(), any()) } returns Result.failure(RuntimeException("build error"))
        val useCase = makeUseCase()

        useCase.state.test {
            assertEquals(ReplyUiState.Idle, awaitItem())
            useCase.generate(sampleMessages)
            val err = awaitItem()
            assertTrue(err is ReplyUiState.Error)
            assertEquals("build error", (err as ReplyUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { modelApi.call(any()) }
    }

    @Test
    fun `流式累积 多个 StreamDelta 拼接为完整文本`() = runTest {
        coEvery { buildPrompt.invoke(any(), any()) } returns Result.success(samplePrompt)
        coEvery { modelApi.call(any()) } returns flowOf(
            ModelCallEvent.StreamDelta("[1] 回复1\n[2] "),
            ModelCallEvent.StreamDelta("回复2\n[3] 回复3"),
            ModelCallEvent.StreamDone(TokenUsage.of(10, 20), 1000L),
        )
        val useCase = makeUseCase()

        useCase.state.test {
            assertEquals(ReplyUiState.Idle, awaitItem())
            useCase.generate(sampleMessages)
            // 第一次：Streaming("")
            assertEquals(ReplyUiState.Streaming(""), awaitItem())
            // 第二次：Streaming("[1] 回复1\n[2] ")
            assertEquals(ReplyUiState.Streaming("[1] 回复1\n[2] "), awaitItem())
            // 第三次：Streaming("[1] 回复1\n[2] 回复2\n[3] 回复3")
            assertEquals(
                ReplyUiState.Streaming("[1] 回复1\n[2] 回复2\n[3] 回复3"),
                awaitItem(),
            )
            // 第四次：Suggestions 解析（v0.6 正则只认 [.、] 不认 [N] 前缀，所以保留全文）
            val sug = awaitItem()
            assertTrue(sug is ReplyUiState.Suggestions, "expected Suggestions got $sug")
            assertEquals(
                listOf("[1] 回复1", "[2] 回复2", "[3] 回复3"),
                (sug as ReplyUiState.Suggestions).items,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `隐私模式开启不写历史`() = runTest {
        coEvery { buildPrompt.invoke(any(), any()) } returns Result.success(samplePrompt)
        coEvery { modelApi.call(any()) } returns flowOf(
            ModelCallEvent.StreamDone(TokenUsage.of(10, 20), 1000L),
        )
        coEvery { configRepo.snapshot() } returns AppConfig(privacyMode = true)
        val useCase = makeUseCase()

        useCase.generate(sampleMessages)

        coVerify(exactly = 0) { historyRepo.record(any()) }
    }

    @Test
    fun `非隐私模式写历史含 targetPackage`() = runTest {
        coEvery { buildPrompt.invoke(any(), any()) } returns Result.success(samplePrompt)
        coEvery { modelApi.call(any()) } returns flowOf(
            ModelCallEvent.StreamDone(TokenUsage.of(10, 20), 1000L),
        )
        coEvery { configRepo.snapshot() } returns AppConfig(privacyMode = false)
        val useCase = makeUseCase()

        useCase.generate(sampleMessages, targetPackage = "com.tencent.mm")

        coVerifyOrder {
            historyRepo.record(match { it.targetPackage == "com.tencent.mm" })
        }
    }

    @Test
    fun `parseSuggestions 兼容 1 xxx 格式`() = runTest {
        coEvery { buildPrompt.invoke(any(), any()) } returns Result.success(samplePrompt)
        coEvery { modelApi.call(any()) } returns flowOf(
            ModelCallEvent.StreamDone(TokenUsage.of(0, 0), 0L),
        )
        coEvery { configRepo.snapshot() } returns AppConfig(privacyMode = true)
        val useCase = makeUseCase()
        useCase.state.test {
            assertEquals(ReplyUiState.Idle, awaitItem())
            // 用反射测 parseSuggestions 比较麻烦，改用 StreamDelta 喂入测试格式
            coEvery { modelApi.call(any()) } returns flowOf(
                ModelCallEvent.StreamDelta("1. 第一条\n2. 第二条\n3. 第三条"),
                ModelCallEvent.StreamDone(TokenUsage.of(0, 0), 0L),
            )
            useCase.generate(sampleMessages)
            // skip initial Streaming("") + Streaming(accumulated)
            skipItems(2)
            val sug = awaitItem()
            assertTrue(sug is ReplyUiState.Suggestions)
            assertEquals(
                listOf("第一条", "第二条", "第三条"),
                (sug as ReplyUiState.Suggestions).items,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `parseSuggestions 候选最多 5 条`() = runTest {
        coEvery { buildPrompt.invoke(any(), any()) } returns Result.success(samplePrompt)
        coEvery { configRepo.snapshot() } returns AppConfig(privacyMode = true)
        val useCase = makeUseCase()
        coEvery { modelApi.call(any()) } returns flowOf(
            ModelCallEvent.StreamDelta(
                "[1] 一\n[2] 二\n[3] 三\n[4] 四\n[5] 五\n[6] 六\n[7] 七",
            ),
            ModelCallEvent.StreamDone(TokenUsage.of(0, 0), 0L),
        )

        useCase.state.test {
            assertEquals(ReplyUiState.Idle, awaitItem())
            useCase.generate(sampleMessages)
            skipItems(2)
            val sug = awaitItem()
            assertTrue(sug is ReplyUiState.Suggestions)
            assertEquals(5, (sug as ReplyUiState.Suggestions).items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ModelCallEvent Error 置 Error 状态`() = runTest {
        coEvery { buildPrompt.invoke(any(), any()) } returns Result.success(samplePrompt)
        coEvery { modelApi.call(any()) } returns flowOf(
            ModelCallEvent.Error(com.chatassist.domain.model.AppError.of(
                com.chatassist.domain.model.ErrorCode.NETWORK,
                "网络不通",
                "UnknownHost",
            )),
        )
        val useCase = makeUseCase()

        useCase.state.test {
            assertEquals(ReplyUiState.Idle, awaitItem())
            useCase.generate(sampleMessages)
            skipItems(1)  // Streaming("")
            val err = awaitItem()
            assertTrue(err is ReplyUiState.Error)
            assertEquals("网络不通", (err as ReplyUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
