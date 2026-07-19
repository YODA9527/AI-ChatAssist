package com.chatassist.domain.usecase

import com.chatassist.domain.model.BuiltPrompt
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.HistoryRecordInput
import com.chatassist.domain.model.ModelCallEvent
import com.chatassist.domain.model.TokenUsage
import com.chatassist.domain.repository.ConfigRepository
import com.chatassist.domain.repository.HistoryRepository
import com.chatassist.domain.repository.ModelApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 回复建议用例（v0.6 落地，模块 09 接历史落库）。
 *
 * 把实时捕获到的消息喂给大模型，收集流式响应，解析出 3 条候选回复，
 * 通过 [state] 暴露给 UI / 悬浮窗。模型调用走 [ModelApiRepository]（唯一网络出口）。
 *
 * 模块 09：生成成功（[ModelCallEvent.StreamDone]）且未开启隐私模式时，
 * 把本次对话 + 回复 + Token 用量写入历史（[historyRepo]）。
 */
@Singleton
class ReplySuggestionUseCase @Inject constructor(
    private val buildPrompt: BuildPromptUseCase,
    private val modelApi: ModelApiRepository,
    private val historyRepo: HistoryRepository,
    private val configRepo: ConfigRepository,
) {

    private val _state = MutableStateFlow<ReplyUiState>(ReplyUiState.Idle)
    val state: StateFlow<ReplyUiState> = _state.asStateFlow()

    suspend fun generate(messages: List<ChatMessage>, targetPackage: String = "") {
        if (messages.isEmpty()) {
            _state.value = ReplyUiState.Error("暂无可分析的对话，先去捕获一些消息吧～")
            return
        }
        val prompt = buildPrompt(messages).getOrElse { e ->
            _state.value = ReplyUiState.Error(e.message ?: "构造 Prompt 失败")
            return
        }
        _state.value = ReplyUiState.Streaming("")
        modelApi.call(prompt).collect { ev ->
            when (ev) {
                is ModelCallEvent.StreamDelta -> {
                    val cur = (_state.value as? ReplyUiState.Streaming)?.text.orEmpty()
                    _state.value = ReplyUiState.Streaming(cur + ev.text)
                }
                is ModelCallEvent.StreamDone -> {
                    val full = (_state.value as? ReplyUiState.Streaming)?.text.orEmpty()
                    _state.value = ReplyUiState.Suggestions(parseSuggestions(full))
                    recordIfAllowed(messages, prompt, full, ev.usage, targetPackage)
                }
                is ModelCallEvent.Error -> _state.value = ReplyUiState.Error(ev.error.userMessage)
            }
        }
    }

    /**
     * 生成成功且未开启隐私模式时落库；失败仅记日志，绝不阻塞 UI / 悬浮窗主链路。
     */
    private suspend fun recordIfAllowed(
        messages: List<ChatMessage>,
        prompt: BuiltPrompt,
        responseText: String,
        usage: TokenUsage,
        targetPackage: String,
    ) {
        runCatching {
            if (!configRepo.snapshot().privacyMode) {
                historyRepo.record(
                    HistoryRecordInput(
                        targetPackage = targetPackage,
                        messages = messages,
                        prompt = prompt,
                        responseText = responseText,
                        usage = usage,
                    ),
                )
            }
        }.onFailure { Timber.w(it, "历史记录写入失败（已忽略）") }
    }

    /** 把模型文本解析成候选列表（兼容「[1] xxx」「1. xxx」「纯文本行」）。 */
    private fun parseSuggestions(full: String): List<String> {
        return full.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val m = SUGGESTION_PREFIX.matcher(line)
                if (m.find()) m.group(2)?.trim() else line
            }
            .filter { it.isNotEmpty() }
            .take(5)
    }

    companion object {
        private val SUGGESTION_PREFIX = Pattern.compile("^\\[?(\\d+)[.、]\\]?\\s*(.*)$")
    }
}

/** 回复建议 UI 状态。 */
sealed interface ReplyUiState {
    data object Idle : ReplyUiState
    data class Streaming(val text: String) : ReplyUiState
    data class Suggestions(val items: List<String>) : ReplyUiState
    data class Error(val message: String) : ReplyUiState
}
