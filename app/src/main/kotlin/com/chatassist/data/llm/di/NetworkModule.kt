package com.chatassist.data.llm.di

import com.chatassist.data.llm.network.OkHttpClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * 网络层 Hilt 模块（v0.3 引入）。
 *
 * 提供全局唯一的 [OkHttpClient]（由 [OkHttpClientFactory] 构造）和 [Json] 实例。
 *
 * **Json 配置**（适配多厂商 OpenAI 兼容协议的容错需求）：
 * - `ignoreUnknownKeys = true`：忽略厂商扩展字段（如 DeepSeek 的 `prefix` / MoonShot 的插件信息）
 * - `encodeDefaults = true`：默认字段也编码（保证请求协议完整）
 * - `explicitNulls = false`：null 字段不编码（减小 payload）
 * - `isLenient = true`：允许非标准 JSON（如未引号 key）
 * - `coerceInputValues = true`：枚举未知值时回退默认值
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(factory: OkHttpClientFactory): OkHttpClient = factory.create()
}
