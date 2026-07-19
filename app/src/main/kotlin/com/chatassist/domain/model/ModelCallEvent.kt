package com.chatassist.domain.model

/**
 * 模型调用事件流（v0.3 引入）。
 *
 * Repository 通过 `Flow<ModelCallEvent>` 推送三类事件：
 * 1. [StreamDelta]  — 增量文本（每收到一个 SSE chunk 发一次）
 * 2. [StreamDone]  — 流结束（含 Token 用量 + 总耗时）
 * 3. [Error]       — 终结事件，流中此后无其他事件
 *
 * **铁律**：
 * - 流终止只能由 `StreamDone` 或 `Error` 之一触发，不会同时发
 * - `Error` 事件**不抛异常**，仅作事件传递（让消费方决定 UI 反馈策略）
 * - Flow 本身在 `Error` 事件后正常 complete（不会自动抛）
 */
sealed interface ModelCallEvent {

    /** 流式增量：纯文本片段，UI 应做打字机拼接。 */
    data class StreamDelta(val text: String) : ModelCallEvent

    /** 流结束：携带最终 Token 用量与端到端耗时（毫秒，从发起请求到收到 DONE 事件）。 */
    data class StreamDone(
        val usage: TokenUsage,
        val totalLatencyMs: Long,
    ) : ModelCallEvent

    /** 终结事件：携带完整 [AppError]（含 code / userMessage / retryable / suggestedAction）。 */
    data class Error(val error: AppError) : ModelCallEvent
}
