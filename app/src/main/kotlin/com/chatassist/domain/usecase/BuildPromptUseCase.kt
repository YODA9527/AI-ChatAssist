package com.chatassist.domain.usecase

import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.BuiltPrompt
import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.Message
import com.chatassist.domain.model.MessageRole
import com.chatassist.domain.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 构造大模型 Prompt（v0.6 落地，文档 05 章）。
 *
 * 把最近 N 轮对话 + 人设 + 目标语气，组装成 [BuiltPrompt]：
 * - System：任务定义 + 我是谁/说话风格/关系 + 目标语气 + 回复要求（3 条候选）。
 * - User：最近对话转写（「我：… / 对方：…」）+ 生成指令。
 * 上下文超长时按 [AppConfig.contextRounds] 截断（保留最后 N 轮 + System 必带）。
 */
@Singleton
class BuildPromptUseCase @Inject constructor(
    private val configRepo: ConfigRepository,
) {

    suspend operator fun invoke(messages: List<ChatMessage>): Result<BuiltPrompt> = runCatching {
        val cfg = configRepo.snapshot()
        val recent = messages
            .takeLast(cfg.contextRounds * 2)
            .filter { it.sender != "系统" && it.content.isNotBlank() }

        val transcript = if (recent.isEmpty()) {
            "（暂无历史对话）"
        } else {
            recent.joinToString("\n") { "${if (it.isSelf) "我" else "对方"}：${it.content}" }
        }

        // 对话含 OCR 来源（屏幕识别，可能有形近字/繁体误识）→ 让模型结合语境语义纠错
        val hasOcr = messages.any { it.source == CaptureMode.OCR_ONLY }
        val system = buildSystemPrompt(cfg, hasOcr)
        val user = buildString {
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
            model = cfg.model,
        )
    }

    private fun buildSystemPrompt(cfg: AppConfig, hasOcr: Boolean = false): String = buildString {
        appendLine("# 任务")
        appendLine("你是聊天回复建议生成器，根据上下文为用户（「我」）生成合适的回复候选。")
        appendLine()
        appendLine("# 我的人设")
        val p = cfg.persona
        appendLine("- 我是谁：${p.whoAmI.ifBlank { "（未填写）" }}")
        appendLine("- 说话风格：${p.speakingStyle.ifBlank { "（未填写）" }}")
        appendLine("- 与对方关系：${p.relationship.ifBlank { "（未填写）" }}")
        appendLine()
        appendLine("# 目标语气")
        appendLine(goalText(cfg.goal))
        appendLine()
        appendLine("# 回复要求")
        appendLine("- 候选数：3 条")
        appendLine("- 风格：与对方匹配，与「我」的人设一致")
        appendLine("- 长度：1-3 句，不要长篇大论")
        appendLine("- 格式：每条独立一行，前面加 [1] [2] [3]")
        if (hasOcr) {
            appendLine()
            appendLine("# OCR 文本提示")
            appendLine(
                "注意：上方对话来自屏幕 OCR 识别，可能存在个别汉字形近字/繁体误识" +
                    "（例如「者」误作「考」、「妻」误作「麦」、「己/已」混淆、「愛」为繁体等）。" +
                    "请结合上下文语义自动纠正理解后再生成回复，不要在回复中复述或指出识别错误。",
            )
        }
    }

    private fun goalText(goal: com.chatassist.domain.model.ConversationGoal): String = when (goal) {
        com.chatassist.domain.model.ConversationGoal.PROFESSIONAL -> "专业、得体、克制"
        com.chatassist.domain.model.ConversationGoal.CASUAL -> "轻松、随意、自然"
        com.chatassist.domain.model.ConversationGoal.WARM -> "温暖、亲切、有温度"
        com.chatassist.domain.model.ConversationGoal.FLIRTY -> "暧昧、撩人、带点小调皮"
        com.chatassist.domain.model.ConversationGoal.CONCISE -> "简洁、直接、不废话"
        com.chatassist.domain.model.ConversationGoal.HUMOROUS -> "幽默、有趣、爱开玩笑"
    }
}
