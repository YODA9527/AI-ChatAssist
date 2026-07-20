package com.chatassist.domain.usecase

import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.BuiltPrompt
import com.chatassist.domain.model.activeScheme
import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.CounterpartPersona
import com.chatassist.domain.model.Message
import com.chatassist.domain.model.MessageRole
import com.chatassist.domain.model.ResolvedPersona
import com.chatassist.domain.model.SelfPersona
import com.chatassist.domain.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 构造大模型 Prompt（v0.6 落地，v0.16 缓存命中优化，v0.18 双向人设）。
 *
 * 缓存命中设计（哥哥 2026-07-19 下旨）：
 *  - System 块【完全稳定】—— 100% 字节级一致，与当前对话内容/OCR 状态解耦
 *  - 任何条件性内容（OCR 提示等）放 User 块最前面
 *  - messages 数组结构固定：[System, User] 不变
 *  - 这样 OpenAI/Anthropic/DeepSeek 的 prompt cache（前缀匹配）能稳定命中 System 块
 *
 * 双向人设（v0.18，v2.2 §13.1）：
 *  - 生效人设由 [PersonaMatcher] 解析：手动锁定(activePersonaId) > 包名 > 关键词 > 默认
 *  - 无双向方案时降级到旧 v2.1 单人设（[AppConfig.persona]），老用户人设不丢
 *  - 人设字段写入 System 块；切换人设会让 System 块变化 → prompt cache 短暂 miss 一次
 *    （与 [styleText] 同理，是不可避免且合理的取舍）
 *
 * 结构：
 *  - System：任务定义 + 我的人设 + 对方的人设 + 目标语气 + 回复要求
 *  - User：OCR 提示（条件性，置顶）+ 最近对话转写 + 生成指令
 */
@Singleton
class BuildPromptUseCase @Inject constructor(
    private val configRepo: ConfigRepository,
) {

    suspend operator fun invoke(messages: List<ChatMessage>, targetPackage: String = ""): Result<BuiltPrompt> = runCatching {
        val cfg = configRepo.snapshot()
        val recent = messages
            .takeLast(cfg.contextRounds * 2)
            .filter { it.sender != "系统" && it.content.isNotBlank() }

        val transcript = if (recent.isEmpty()) {
            "（暂无历史对话）"
        } else {
            recent.joinToString("\n") { "${if (it.isSelf) "我" else "对方"}：${it.content}" }
        }

        // 供 PersonaMatcher 做关键词匹配的纯文本（已过滤系统消息/空内容）
        val transcriptText = recent.joinToString(" ") { it.content }

        val hasOcr = messages.any { it.source == CaptureMode.OCR_ONLY }

        // 解析生效人设：双向多套（锁定/包名/关键词/默认）→ 兼容旧 v2.1 单人设
        val resolved = resolvePersona(cfg, targetPackage, transcriptText)

        // System 块：完全稳定，不含任何条件性内容，便于 prompt cache 命中
        val system = buildSystemPrompt(cfg, resolved)

        // User 块：OCR 提示（条件性）置顶 → 聊天内容 → 生成指令
        val user = buildString {
            if (hasOcr) {
                appendLine(OCR_HINT)
                appendLine()
            }
            appendLine("最近对话：")
            appendLine(transcript)
            appendLine()
            appendLine("请根据以上对话，站在「我」的视角，给出 3 条合适的回复建议。")
            appendLine("要求：每条独立一行，前面加 [1] [2] [3]；风格与对方匹配、符合我的人设；长度 1-3 句，不要长篇大论。")
        }

        BuiltPrompt(
            systemPrompt = system,
            messages = listOf(Message(MessageRole.USER, user)),
            temperature = cfg.temperature,
            maxTokens = cfg.maxTokens,
            model = cfg.activeScheme().textModel.model,
        )
    }

    /**
     * 解析当前生效人设。
     *
     * 优先取 [PersonaMatcher] 选中的双向方案；若双向方案列表为空（老用户），
     * 降级使用旧 v2.1 单人设 [AppConfig.persona]（whoAmI→名字/身份，speakingStyle→说话风格，
     * relationship→关系），保证行为向后兼容。
     */
    private fun resolvePersona(cfg: AppConfig, targetPackage: String, transcriptText: String): ResolvedPersona {
        val profile = PersonaMatcher.resolve(cfg, targetPackage, transcriptText)
        if (profile != null) return ResolvedPersona(profile.self, profile.counterpart)

        // 兼容旧 v2.1 单人设
        val legacy = cfg.persona
        return ResolvedPersona(
            self = SelfPersona(
                name = legacy.whoAmI,
                identity = legacy.whoAmI,
                speakingStyle = legacy.speakingStyle,
            ),
            counterpart = CounterpartPersona(relationship = legacy.relationship),
        )
    }

    private fun buildSystemPrompt(cfg: AppConfig, resolved: ResolvedPersona): String = buildString {
        appendLine("# 任务")
        appendLine("你是聊天回复建议生成器，根据上下文为用户（「我」）生成合适的回复候选。")
        appendLine()
        appendLine("# 我的人设")
        val s = resolved.self
        appendLine("- 名字：${s.name.ifBlank { "（未填写）" }}")
        appendLine("- 身份：${s.identity.ifBlank { "（未填写）" }}")
        appendLine("- 说话风格：${s.speakingStyle.ifBlank { "（未填写）" }}")
        appendLine("- 禁忌：${s.taboo.ifBlank { "（未填写）" }}")
        appendLine("- 口头禅：${s.signature.ifBlank { "（未填写）" }}")
        appendLine()
        appendLine("# 对方的人设")
        val c = resolved.counterpart
        appendLine("- 名字：${c.name.ifBlank { "（未填写）" }}")
        appendLine("- 身份：${c.identity.ifBlank { "（未填写）" }}")
        appendLine("- 与我的关系：${c.relationship.ifBlank { "（未填写）" }}")
        appendLine("- 说话风格：${c.theirStyle.ifBlank { "（未填写）" }}")
        appendLine("- 性格：${c.theirPersonality.ifBlank { "（未填写）" }}")
        appendLine("- 互动动态：${c.dynamic.ifBlank { "（未填写）" }}")
        appendLine("- 聊天目标：${c.goal.ifBlank { "（未填写）" }}")
        appendLine("- 喜好：${c.likes.ifBlank { "（未填写）" }}")
        appendLine("- 讨厌的东西：${c.dislikes.ifBlank { "（未填写）" }}")
        appendLine("- 雷区：${c.petPeeves.ifBlank { "（未填写）" }}")
        appendLine("- 口头习惯：${c.habits.ifBlank { "（未填写）" }}")
        appendLine()
        appendLine("# 目标语气")
        appendLine(styleText(cfg.style))
        appendLine()
        appendLine("# 回复要求")
        appendLine("- 候选数：3 条")
        appendLine("- 风格：与对方匹配，与「我」的人设一致")
        appendLine("- 长度：1-3 句，不要长篇大论")
        appendLine("- 格式：每条独立一行，前面加 [1] [2] [3]")
    }

    /**
     * 对话风格 → 中文描述（注入到 system 块的「# 目标语气」段）。
     *
     * v0.17 改名：goalText → styleText，参数类型 [com.chatassist.domain.model.ConversationGoal]
     * → [com.chatassist.domain.model.ConversationStyle]，枚举值不变。
     *
     * **缓存命中说明**（哥哥 2026-07-19 确认）：
     *  - 每次 invoke 都 [configRepo.snapshot] 拿**最新** cfg，切换 [ConversationStyle] 后**下一次生成
     *    会立即注入新风格**（本函数在 invoke 路径上被实时调用）
     *  - 代价：style 切换会让 system 块的「# 目标语气」段文字变化 → OpenAI/Anthropic/DeepSeek
     *    的 prompt cache（前缀匹配）**短暂 miss 一次**，下次开始重新稳定
     *  - 双向人设同理：切换人设方案 / 自动匹配到不同方案，也会让 system 块人设段变化 → cache
     *    短暂 miss 一次；这是用户主动或场景驱动的变更，本就该反映到 system prompt
     *  - 若未来想"切换不影响 cache"，可把人设/风格段挪到 user 块（user 本来就每次变），但会
     *    损失 system 块的语义位置（模型对 system 指令的权重更高）
     */
    private fun styleText(style: com.chatassist.domain.model.ConversationStyle): String = when (style) {
        com.chatassist.domain.model.ConversationStyle.PROFESSIONAL -> "专业、得体、克制"
        com.chatassist.domain.model.ConversationStyle.CASUAL -> "轻松、随意、自然"
        com.chatassist.domain.model.ConversationStyle.WARM -> "温暖、亲切、有温度"
        com.chatassist.domain.model.ConversationStyle.FLIRTY -> "暧昧、撩人、带点小调皮"
        com.chatassist.domain.model.ConversationStyle.CONCISE -> "简洁、直接、不废话"
        com.chatassist.domain.model.ConversationStyle.HUMOROUS -> "幽默、有趣、爱开玩笑"
    }

    companion object {
        /**
         * OCR 形近字纠错提示（v0.9.0 引入，v0.16 移到 user 块置顶）。
         *
         * 必须是【完全固定】的字符串，命中缓存后只有 transcript 部分变化，
         * 整段 user 内容的前 N tokens（前缀）可被多数厂商的 prompt cache 复用。
         */
        private const val OCR_HINT =
            "注意：以下对话来自屏幕 OCR 识别，可能存在个别汉字形近字或繁体误识" +
                "（例如「者」误作「考」、「妻」误作「麦」、「己/已」混淆、「愛」为繁体等）。" +
                "请结合上下文语义自动纠正理解后再生成回复，不要在回复中复述或指出识别错误。"
    }
}
