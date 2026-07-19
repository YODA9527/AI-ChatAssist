package com.chatassist.data.llm.network

import com.chatassist.data.llm.dto.ChatRequestDto
import com.chatassist.data.llm.dto.ChatResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI 兼容 chat completions 服务接口（v0.3 引入）。
 *
 * 覆盖 OpenAI / DeepSeek / 通义千问 / Kimi / 智谱 GLM / 书生 InternLM / 字节豆包 / 腾讯混元 / 零一万物。
 *
 * **路径约定**：`POST {baseUrl}/chat/completions`
 * 用户配置的 `apiUrl` 必须含完整 base，例如 `https://api.deepseek.com/v1`。
 *
 * **流式 vs 非流式**：
 * - 非流式（本接口）→ 一次返回完整 [ChatResponseDto]
 * - 流式 → 绕过 Retrofit，直接用 OkHttp EventSource（避免 [retrofit2.http.Streaming] 与 Kotlinx 协作的边界陷阱）
 */
interface OpenAiApiService {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body body: ChatRequestDto,
    ): ChatResponseDto
}
