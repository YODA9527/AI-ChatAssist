package com.chatassist.data.llm.network

import com.chatassist.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttpClient 工厂（v0.3 引入）。
 *
 * 集中管理超时、拦截器、日志等全局配置。
 *
 * **超时策略**（流式响应需要长 read）：
 * - connect: 10s（建连超时）
 * - read: 5min（流式响应单 chunk 之间最大间隔）
 * - write: 30s（请求体写入）
 * - call: 0 = 无总超时（流式不能设死，否则长时间对话会被强制切断）
 *
 * **拦截器顺序**（OkHttp 应用/网络拦截器区分）：
 * - 应用层：WhitelistInterceptor（最先）→ Logging（最后，便于看脱敏后的请求）
 * - 暂不加网络层拦截器（无缓存需求）
 *
 * **重试策略**：`retryOnConnectionFailure(false)` —— 由调用方在 Repository 层做可控重试，
 * 避免 OkHttp 默认重试与业务重试叠加导致重复扣费。
 */
@Singleton
class OkHttpClientFactory @Inject constructor(
    private val whitelist: WhitelistInterceptor,
) {
    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_MIN, TimeUnit.MINUTES)
        .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_MIN, TimeUnit.MINUTES)
        .retryOnConnectionFailure(false)
        .addInterceptor(whitelist)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")  // 脱敏 API Key
                    }
                )
            }
        }
        .build()

    companion object {
        private const val CONNECT_TIMEOUT_SEC = 10L
        private const val READ_TIMEOUT_MIN = 5L
        private const val WRITE_TIMEOUT_SEC = 30L
        private const val CALL_TIMEOUT_MIN = 5L
    }
}
