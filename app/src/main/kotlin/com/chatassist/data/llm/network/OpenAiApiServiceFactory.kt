package com.chatassist.data.llm.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenAiApiService 工厂（v0.3 引入）。
 *
 * 用户修改 `apiUrl` 时由 [com.chatassist.data.repository.ModelApiRepositoryImpl] 调用本工厂
 * 重建 service 实例。OkHttpClient 是单例共享的，连接池得到复用。
 */
@Singleton
class OpenAiApiServiceFactory @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    /**
     * 根据用户配置的 `apiUrl` 构造 service。
     *
     * @param rawApiUrl 用户原始配置（可能带或不带尾部 `/`）
     * @return 新的 service 实例
     */
    fun create(rawApiUrl: String): OpenAiApiService {
        val normalized = normalizeBaseUrl(rawApiUrl)
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(OpenAiApiService::class.java)
    }

    /**
     * 标准化 baseUrl：去前后空白 + 强制尾部 `/`（Retrofit 硬性要求）。
     */
    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "apiUrl cannot be blank" }
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "apiUrl must start with http:// or https://, got: $trimmed"
        }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
