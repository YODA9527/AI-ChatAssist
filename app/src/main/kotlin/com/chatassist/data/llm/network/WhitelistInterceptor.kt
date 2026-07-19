package com.chatassist.data.llm.network

import com.chatassist.domain.model.AppError
import com.chatassist.domain.model.ErrorCode
import com.chatassist.domain.model.SuggestedAction
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 出站白名单拦截器（v0.3 引入）。
 *
 * **核心铁闸**：ChatAssist 唯一网络出口是用户配置的 API URL。
 * 任何出站请求的 host 必须与 `allowedHost` 完全匹配，否则直接抛 [AppError]。
 *
 * 设计要点：
 * 1. `allowedHost` 是 `@Volatile` 可变引用——配置变化时由 [com.chatassist.data.repository.ModelApiRepositoryImpl] 更新
 * 2. 不放子域通配——避免「api.deepseek.com」被滥用放行「evil.deepseek.com」
 * 3. 抛 [AppError]（RuntimeException）而非 IOException——保留结构化错误信息
 *    供调用方在 catch 中精确处理（OkHttp/Retrofit 都会原样向上传递）
 * 4. 端口不参与白名单匹配（host 等于即放行，端口由 URL 自己决定）
 *
 * 重置白名单：`allowedHost = null` 即可禁用所有出站。
 */
@Singleton
class WhitelistInterceptor @Inject constructor() : Interceptor {

    @Volatile
    var allowedHost: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val targetHost = request.url.host
        val allowed = allowedHost

        if (allowed == null) {
            throw AppError(
                code = ErrorCode.NETWORK,
                userMessage = "API URL 未配置，请先在设置中填写 API 地址",
                technicalDetail = "WhitelistInterceptor.allowedHost is null, target=$targetHost",
                retryable = false,
                suggestedAction = SuggestedAction.OPEN_SETTINGS,
            )
        }

        if (!targetHost.equals(allowed, ignoreCase = true)) {
            throw AppError(
                code = ErrorCode.NETWORK,
                userMessage = "请求被拦截：域名 $targetHost 不在白名单（允许：$allowed）",
                technicalDetail = "Whitelist blocked: target=$targetHost, allowed=$allowed",
                retryable = false,
                suggestedAction = SuggestedAction.OPEN_SETTINGS,
            )
        }

        return chain.proceed(request)
    }
}
