package com.chatassist.domain.usecase

import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.ConversationStyle
import com.chatassist.domain.model.CounterpartPersona
import com.chatassist.domain.model.MessageRole
import com.chatassist.domain.model.PersonaConfig
import com.chatassist.domain.model.PersonaProfile
import com.chatassist.domain.model.SelfPersona
import com.chatassist.domain.repository.ConfigRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * BuildPromptUseCase 单测（v0.14 全量测试，P2 补 UseCase 覆盖；v0.18 扩双向人设）。
 *
 * 覆盖：空消息 + 正常消息 + system 含 OCR 提示 + contextRounds 截断 + Style 6 种
 *       + 旧版单人设兼容占位 + 双向人设注入 + PersonaMatcher 锁定/包名匹配。
 */
class BuildPromptUseCaseTest {

    private val configRepo: ConfigRepository = mockk(relaxed = true)

    @Test
    fun `空消息 transcript 走暂无历史对话`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5)
        val useCase = BuildPromptUseCase(configRepo)

        val result = useCase(emptyList())
        val prompt = result.getOrThrow()

        assertTrue(prompt.systemPrompt.contains("# 任务"))
        assertTrue(prompt.messages.first().content.contains("（暂无历史对话）"))
        assertEquals(MessageRole.USER, prompt.messages.first().role)
    }

    @Test
    fun `正常消息渲染为 transcript 我与对方前缀`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5)
        val useCase = BuildPromptUseCase(configRepo)
        val messages = listOf(
            ChatMessage(id = "1", content = "你好", isSelf = true, sender = "我"),
            ChatMessage(id = "1", content = "在吗", isSelf = false, sender = "对方"),
        )

        val prompt = useCase(messages).getOrThrow()
        val userContent = prompt.messages.first().content

        assertTrue(userContent.contains("我：你好"), "应包含「我：你好」")
        assertTrue(userContent.contains("对方：在吗"), "应包含「对方：在吗」")
    }

    @Test
    fun `isSelf 为 true 显示我前缀`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5)
        val useCase = BuildPromptUseCase(configRepo)
        val messages = listOf(ChatMessage(id = "1", content = "abc", isSelf = true, sender = "我"))

        val prompt = useCase(messages).getOrThrow()
        assertTrue(prompt.messages.first().content.contains("我：abc"))
    }

    @Test
    fun `isSelf 为 false 显示对方前缀`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5)
        val useCase = BuildPromptUseCase(configRepo)
        val messages = listOf(ChatMessage(id = "1", content = "abc", isSelf = false, sender = "对方"))

        val prompt = useCase(messages).getOrThrow()
        assertTrue(prompt.messages.first().content.contains("对方：abc"))
    }

    @Test
    fun `系统消息被过滤不进入 transcript`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5)
        val useCase = BuildPromptUseCase(configRepo)
        val messages = listOf(
            ChatMessage(id = "1", content = "系统提示", isSelf = false, sender = "系统"),
            ChatMessage(id = "1", content = "真的消息", isSelf = true, sender = "我"),
        )

        val prompt = useCase(messages).getOrThrow()
        val userContent = prompt.messages.first().content
        assertTrue(userContent.contains("我：真的消息"))
        assertTrue(!userContent.contains("系统提示"))
    }

    @Test
    fun `空 content 消息被过滤`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5)
        val useCase = BuildPromptUseCase(configRepo)
        val messages = listOf(
            ChatMessage(id = "1", content = "", isSelf = true, sender = "我"),
            ChatMessage(id = "1", content = "有效", isSelf = true, sender = "我"),
        )

        val prompt = useCase(messages).getOrThrow()
        val userContent = prompt.messages.first().content
        assertTrue(!userContent.contains("我：\n"))
        assertTrue(userContent.contains("我：有效"))
    }

    @Test
    fun `OCR_ONLY 消息触发了 user 的 OCR 提示段 v0_16 缓存命中优化`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5)
        val useCase = BuildPromptUseCase(configRepo)
        val messages = listOf(
            ChatMessage(id = "1", content = "正常消息", isSelf = true, sender = "我", source = CaptureMode.OCR_ONLY),
        )

        val prompt = useCase(messages).getOrThrow()
        val userContent = prompt.messages.first().content
        // OCR 提示在 user 块（保证 system 块 100% 稳定以命中 prompt cache）
        assertTrue(userContent.contains("屏幕 OCR 识别"), "应包含 OCR 提示前缀")
        assertTrue(userContent.contains("形近字"))
        // system 块绝对不包含 OCR 提示（缓存命中前提）
        assertTrue(!prompt.systemPrompt.contains("OCR"), "system 块必须不含 OCR 提示以保稳定")
    }

    @Test
    fun `TEXT_ONLY 消息不触发 OCR 提示`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5)
        val useCase = BuildPromptUseCase(configRepo)
        val messages = listOf(
            ChatMessage(id = "1", content = "正常", isSelf = true, sender = "我", source = CaptureMode.TEXT_ONLY),
        )

        val prompt = useCase(messages).getOrThrow()
        val userContent = prompt.messages.first().content
        assertTrue(!userContent.contains("屏幕 OCR 识别"), "TEXT_ONLY 不应触发 OCR 提示")
        assertTrue(!prompt.systemPrompt.contains("OCR"))
    }

    @Test
    fun `contextRounds 等于 1 截断只保留最后 2 条`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 1)
        val useCase = BuildPromptUseCase(configRepo)
        val messages = listOf(
            ChatMessage(id = "1", content = "msg1", isSelf = true, sender = "我"),
            ChatMessage(id = "1", content = "msg2", isSelf = false, sender = "对方"),
            ChatMessage(id = "1", content = "msg3", isSelf = true, sender = "我"),
            ChatMessage(id = "1", content = "msg4", isSelf = false, sender = "对方"),
        )

        val prompt = useCase(messages).getOrThrow()
        val userContent = prompt.messages.first().content
        assertTrue(userContent.contains("msg3"))
        assertTrue(userContent.contains("msg4"))
        assertTrue(!userContent.contains("msg1"))
        assertTrue(!userContent.contains("msg2"))
    }

    @Test
    fun `旧版单人设空字段显示未填写占位 v0_18 兼容降级`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(
            contextRounds = 5,
            persona = PersonaConfig(whoAmI = "", speakingStyle = "", relationship = ""),
        )
        val useCase = BuildPromptUseCase(configRepo)
        val prompt = useCase(listOf(ChatMessage(id = "1", content = "hi", isSelf = true, sender = "我"))).getOrThrow()

        assertTrue(prompt.systemPrompt.contains("名字：（未填写）"))
        assertTrue(prompt.systemPrompt.contains("说话风格：（未填写）"))
        assertTrue(prompt.systemPrompt.contains("与我的关系：（未填写）"))
    }

    @Test
    fun `Style 6 种值都映射到中文描述`() = runTest {
        val styles = listOf(
            ConversationStyle.PROFESSIONAL to "专业",
            ConversationStyle.CASUAL to "轻松",
            ConversationStyle.WARM to "温暖",
            ConversationStyle.FLIRTY to "暧昧",
            ConversationStyle.CONCISE to "简洁",
            ConversationStyle.HUMOROUS to "幽默",
        )
        styles.forEach { (style, expectedKeyword) ->
            coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5, style = style)
            val useCase = BuildPromptUseCase(configRepo)
            val prompt = useCase(listOf(ChatMessage(id = "1", content = "x", isSelf = true, sender = "我"))).getOrThrow()
            assertTrue(
                prompt.systemPrompt.contains(expectedKeyword),
                "Style=$style 应包含「$expectedKeyword」",
            )
        }
    }

    /**
     * v0.17 哥哥下旨「确认预设的对话风格 prompt 是否正确在被切换之后注入到回复生成过程」。
     */
    @Test
    fun `style 切换后下一次 invoke 立即用新风格且不叠加旧风格`() = runTest {
        val useCase = BuildPromptUseCase(configRepo)
        val messages = listOf(ChatMessage(id = "1", content = "hi", isSelf = true, sender = "我"))

        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5, style = ConversationStyle.WARM)
        val warmPrompt = useCase(messages).getOrThrow()
        assertTrue(warmPrompt.systemPrompt.contains("温暖"), "WARM 应注入「温暖」")
        assertTrue(!warmPrompt.systemPrompt.contains("专业、得体"), "WARM 不应包含 PROFESSIONAL 描述")

        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5, style = ConversationStyle.FLIRTY)
        val flirtyPrompt = useCase(messages).getOrThrow()
        assertTrue(flirtyPrompt.systemPrompt.contains("暧昧"), "FLIRTY 应注入「暧昧」")
        assertTrue(!flirtyPrompt.systemPrompt.contains("温暖、亲切"), "FLIRTY 不应再包含 WARM 描述")
    }

    @Test
    fun `双向人设 SelfPersona 与 CounterpartPersona 注入 system 块`() = runTest {
        val profile = PersonaProfile(
            id = "p1",
            name = "和女朋友",
            self = SelfPersona(
                name = "我",
                identity = "男友",
                speakingStyle = "温柔",
                taboo = "别提前任",
                signature = "么么",
            ),
            counterpart = CounterpartPersona(
                name = "她",
                identity = "女友",
                relationship = "恋人",
                theirStyle = "撒娇",
                theirPersonality = "活泼",
                dynamic = "热恋",
                goal = "增进感情",
            ),
        )
        coEvery { configRepo.snapshot() } returns AppConfig(
            contextRounds = 5,
            personas = listOf(profile),
            activePersonaId = "p1",
        )
        val useCase = BuildPromptUseCase(configRepo)
        val sys = useCase(listOf(ChatMessage(id = "1", content = "hi", isSelf = true, sender = "我"))).getOrThrow().systemPrompt

        assertTrue(sys.contains("# 我的人设"))
        assertTrue(sys.contains("名字：我"))
        assertTrue(sys.contains("身份：男友"))
        assertTrue(sys.contains("说话风格：温柔"))
        assertTrue(sys.contains("禁忌：别提前任"))
        assertTrue(sys.contains("口头禅：么么"))
        assertTrue(sys.contains("# 对方的人设"))
        assertTrue(sys.contains("名字：她"))
        assertTrue(sys.contains("与我的关系：恋人"))
        assertTrue(sys.contains("聊天目标：增进感情"))
    }

    /**
     * v0.18.1 哥哥下旨「确认每一项人设信息都有正确的注入回复生成过程」。
     *
     * 把对方人设全部 11 项字段逐一填入不同哨兵值，断言每一项都以 `<段名>：<值>` 形式出现在
     * system 块（即真实进入回复生成的 prompt），杜绝任何一项漏注入。
     */
    @Test
    fun `对方人设全部字段都注入 system 块`() = runTest {
        val profile = PersonaProfile(
            id = "p1",
            name = "和女朋友",
            counterpart = CounterpartPersona(
                name = "SENTINEL_NAME",
                identity = "SENTINEL_IDENTITY",
                relationship = "SENTINEL_REL",
                theirStyle = "SENTINEL_STYLE",
                theirPersonality = "SENTINEL_PERSONALITY",
                dynamic = "SENTINEL_DYNAMIC",
                goal = "SENTINEL_GOAL",
                likes = "SENTINEL_LIKES",
                dislikes = "SENTINEL_DISLIKES",
                petPeeves = "SENTINEL_PEEVES",
                habits = "SENTINEL_HABITS",
            ),
        )
        coEvery { configRepo.snapshot() } returns AppConfig(
            contextRounds = 5,
            personas = listOf(profile),
            activePersonaId = "p1",
        )
        val useCase = BuildPromptUseCase(configRepo)
        val sys = useCase(listOf(ChatMessage(id = "1", content = "hi", isSelf = true, sender = "我"))).getOrThrow().systemPrompt

        val expected = listOf(
            "名字：SENTINEL_NAME",
            "身份：SENTINEL_IDENTITY",
            "与我的关系：SENTINEL_REL",
            "说话风格：SENTINEL_STYLE",
            "性格：SENTINEL_PERSONALITY",
            "互动动态：SENTINEL_DYNAMIC",
            "聊天目标：SENTINEL_GOAL",
            "喜好：SENTINEL_LIKES",
            "讨厌的东西：SENTINEL_DISLIKES",
            "雷区：SENTINEL_PEEVES",
            "口头习惯：SENTINEL_HABITS",
        )
        for (line in expected) {
            assertTrue(sys.contains(line), "system 块应注入「$line」")
        }
    }

    @Test
    fun `PersonaMatcher 锁定 activePersonaId 优先于包名匹配`() = runTest {
        val wx = PersonaProfile(id = "wx", name = "微信", counterpart = CounterpartPersona(relationship = "微信好友"))
        val qq = PersonaProfile(id = "qq", name = "QQ", counterpart = CounterpartPersona(relationship = "QQ好友"))
        coEvery { configRepo.snapshot() } returns AppConfig(
            contextRounds = 5,
            personas = listOf(wx, qq),
            activePersonaId = "qq",
        )
        val useCase = BuildPromptUseCase(configRepo)
        val prompt = useCase(
            listOf(ChatMessage(id = "1", content = "hi", isSelf = true, sender = "我")),
            targetPackage = "com.tencent.mm",
        ).getOrThrow()

        assertTrue(prompt.systemPrompt.contains("与我的关系：QQ好友"))
        assertTrue(!prompt.systemPrompt.contains("与我的关系：微信好友"))
    }

    @Test
    fun `PersonaMatcher 包名匹配命中不同人设`() = runTest {
        val wx = PersonaProfile(
            id = "wx",
            name = "微信",
            boundPackages = listOf("com.tencent.mm"),
            counterpart = CounterpartPersona(relationship = "微信好友"),
        )
        val qq = PersonaProfile(
            id = "qq",
            name = "QQ",
            boundPackages = listOf("com.tencent.mobileqq"),
            counterpart = CounterpartPersona(relationship = "QQ好友"),
        )
        coEvery { configRepo.snapshot() } returns AppConfig(contextRounds = 5, personas = listOf(wx, qq))
        val useCase = BuildPromptUseCase(configRepo)
        val prompt = useCase(
            listOf(ChatMessage(id = "1", content = "hi", isSelf = true, sender = "我")),
            targetPackage = "com.tencent.mobileqq",
        ).getOrThrow()

        assertTrue(prompt.systemPrompt.contains("与我的关系：QQ好友"))
        assertTrue(!prompt.systemPrompt.contains("与我的关系：微信好友"))
    }

    @Test
    fun `temperature maxTokens model 从 config 透传`() = runTest {
        coEvery { configRepo.snapshot() } returns AppConfig(
            contextRounds = 5,
            temperature = 0.7,
            maxTokens = 1024,
            model = "deepseek-chat",
        )
        val useCase = BuildPromptUseCase(configRepo)
        val prompt = useCase(listOf(ChatMessage(id = "1", content = "x", isSelf = true, sender = "我"))).getOrThrow()

        assertEquals(0.7, prompt.temperature)
        assertEquals(1024, prompt.maxTokens)
        assertEquals("deepseek-chat", prompt.model)
    }
}
