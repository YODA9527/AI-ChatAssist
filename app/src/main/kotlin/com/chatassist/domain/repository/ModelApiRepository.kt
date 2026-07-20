package com.chatassist.domain.repository

import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.BuiltPrompt
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.ModelCallEvent
import com.chatassist.domain.model.TokenUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 模型 API 调用仓库接口（v0.3 引入）。
 *
 * 唯一对外暴露模型调用能力的入口。Domain / Presentation 层只依赖此接口。
 * 实现位于 [com.chatassist.data.repository.ModelApiRepositoryImpl]。
 */
interface ModelApiRepository {
    /**
     * 流式调用大模型。
     *
     * @param prompt 已构造好的 [BuiltPrompt]（来自 [com.chatassist.domain.usecase.BuildPromptUseCase]）
     * @return [ModelCallEvent] 事件流，UI 层订阅后做打字机效果
     *
     * 终结事件：[ModelCallEvent.StreamDone]（成功）或 [ModelCallEvent.Error]（失败），二选一。
     */
    fun call(prompt: BuiltPrompt): Flow<ModelCallEvent>

    /**
     * 测试 API 连接（当前激活方案的文本模型）。
     *
     * @param maxAttempts 失败时最大尝试次数（含首次），默认 1（设置页快速反馈）。
     *                    生产环境调用可传 3（指数退避 1s/2s）。
     * @return 成功 `Result.success(latencyMs)`；失败 `Result.failure(AppError)`
     */
    suspend fun testConnection(maxAttempts: Int = 1): Result<Long>

    /**
     * 按指定配置测试连接（编辑方案时测正在编辑的模型，而非仅当前激活方案）。
     *
     * @return 成功 `Result.success(latencyMs)`；失败 `Result.failure(AppError)`
     */
    suspend fun testConnection(
        apiUrl: String,
        apiKey: String,
        model: String,
        maxAttempts: Int = 1,
    ): Result<Long>

    /**
     * 多模态调用（v0.3 占位，未实现）。
     *
     * @throws AppError 始终抛 `ErrorCode.UNKNOWN`，提示用户切换到 v0.4+
     */
    @Suppress("UNUSED_PARAMETER")
    fun callWithImage(
        prompt: BuiltPrompt,
        imageBase64: String,
        mimeType: String = "image/png",
    ): Flow<ModelCallEvent> = flow {
        emit(
            ModelCallEvent.Error(
                AppError.of(
                    code = ErrorCode.UNKNOWN,
                    userMessage = "多模态调用尚未实现，请切换到经济模式（OCR_ONLY）",
                    technicalDetail = "callWithImage is not implemented in v0.3",
                    retryable = false,
                ),
            ),
        )
    }

    /**
     * 收集完整响应用于历史记录（v0.4 接入）。
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun collectFullResponse(events: Flow<ModelCallEvent>): Result<Pair<String, TokenUsage>> =
        Result.failure(
            AppError.of(
                code = ErrorCode.UNKNOWN,
                userMessage = "collectFullResponse 占位，请勿调用",
                technicalDetail = "v0.3 not implemented",
                retryable = false,
            ),
        )
}
