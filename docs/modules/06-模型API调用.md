# 模块 6 — 模型 API 调用

> 状态：**v0.2 完整实现** / 优先级 P0

## 职责边界

通过 Retrofit + OkHttp + Kotlinx Serialization 调用用户自配的大模型 API。
支持**流式响应**（SSE / OkHttp EventSource）、**重试**（指数退避 1s/2s/4s，仅非流式）、
**Token 精确统计**、**白名单拦截**（唯一网络出口铁闸）、**错误码映射**。

## 实现概览（v0.3 落地）

```
data/llm/
├── di/
│   ├── NetworkModule.kt        # @Provides OkHttpClient + Json
│   └── NetworkBindModule.kt    # @Binds ModelApiRepository
├── dto/
│   ├── ChatRequestDto.kt       # OpenAI 兼容请求体
│   └── ChatResponseDto.kt      # OpenAI 兼容响应体 + chunk
├── network/
│   ├── WhitelistInterceptor.kt # 唯一网络出口铁闸
│   ├── OkHttpClientFactory.kt  # 超时 + 日志 + Whitelist 注入
│   ├── OpenAiApiService.kt     # Retrofit interface（非流式）
│   └── OpenAiApiServiceFactory.kt # 动态 baseUrl 工厂
├── sse/
│   └── StreamingCallExecutor.kt # callbackFlow + EventSource
└── (上层)
    data/repository/ModelApiRepositoryImpl.kt
    domain/repository/ModelApiRepository.kt
    domain/model/BuiltPrompt.kt
    domain/model/TokenUsage.kt
    domain/model/ModelCallEvent.kt
```

## 输入输出契约

```kotlin
// BuiltPrompt（输入）
@Serializable
data class BuiltPrompt(
    val systemPrompt: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024,
    val model: String,
)

@Serializable
data class Message(val role: MessageRole, val content: String)
enum class MessageRole { SYSTEM, USER, ASSISTANT, TOOL }

// TokenUsage
@Serializable
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

// ModelCallEvent（输出 Flow）
sealed interface ModelCallEvent {
    data class StreamDelta(val text: String) : ModelCallEvent
    data class StreamDone(val usage: TokenUsage, val totalLatencyMs: Long) : ModelCallEvent
    data class Error(val error: AppError) : ModelCallEvent
}
```

## 关键类

- **`ModelApiRepository`**（@Singleton, `Provider<ConfigRepository>` 注入避免循环）
  - `fun call(prompt: BuiltPrompt): Flow<ModelCallEvent>` — 流式
  - `suspend fun testConnection(maxAttempts: Int = 1): Result<Long>` — 非流式，可重试
  - `fun callWithImage(...)` — v0.3 占位，v0.4 接入多模态
  - `suspend fun collectFullResponse(...)` — v0.3 占位，v0.4 接入历史折叠

- **`WhitelistInterceptor`**（@Singleton, `@Volatile var allowedHost`）
  - 强制只允许出站到用户配置的 API 域名
  - 任何其他 host / 未配置 → 抛 `AppError(NETWORK)` + `SuggestedAction.OPEN_SETTINGS`
  - host 大小写不敏感，端口不参与匹配

- **`StreamingCallExecutor`**（@Singleton）
  - `callbackFlow<ModelCallEvent>` + `EventSourceListener`
  - 解析 `data: {json}` chunk → `StreamDelta`
  - 收 `data: [DONE]` → `onClosed` → `StreamDone(usage, latencyMs)`
  - JSON 损坏 chunk 自动跳过（不中断流）
  - 401/403/429/5xx → 解析 body 提取 `error.message` 作为 userMessage
  - `peekBody(8KB)` 读错误信息（消费安全，不破坏后续流）
  - `AtomicBoolean terminated` 保证 `StreamDone` / `Error` 互斥

- **`OkHttpClientFactory`**（@Singleton）
  - 超时：connect=10s / read=5min / write=30s / call=5min
  - 拦截器：WhitelistInterceptor（先）+ HttpLoggingInterceptor（debug only，BASIC level，`redactHeader("Authorization")`）
  - `retryOnConnectionFailure(false)` —— 由调用方控制重试

- **`OpenAiApiServiceFactory`**（@Singleton）
  - 接受原始 `apiUrl`，标准化为 `trim + 尾部强制 /`
  - 校验 scheme 为 http/https

## 重试策略

| 场景 | 策略 |
|------|------|
| `call`（流式） | 不在 Repository 层重试（流中断后不可幂等），错误事件透传给上层 UseCase（v0.4 接入） |
| `testConnection`（非流式） | 3 次指数退避（1s/2s/4s），仅 `retryable=true` 的错误（5xx/429/Network/Timeout）才重试；4xx 直接抛 |

## 错误码映射（HTTP → ErrorCode）

| HTTP | ErrorCode | userMessage（中文） | retryable | suggestedAction |
|------|-----------|---------------------|-----------|-----------------|
| 401 | AUTH_401 | API Key 无效（带服务端 message） | false | CHECK_API_KEY |
| 403 | AUTH_403 | 无权限访问该模型 | false | OPEN_SETTINGS |
| 429 | RATE_LIMIT_429 | 请求过于频繁 | true | RETRY |
| 500-599 | SERVER_5XX | 模型服务异常（HTTP 5xx） | true | RETRY |
| SocketTimeout | TIMEOUT | 模型响应超时 | true | RETRY |
| UnknownHost | DNS | 请检查 API URL | false | OPEN_SETTINGS |
| 其他 IOException | NETWORK | 网络异常 | true | CHECK_NETWORK |
| 解析异常（chunk） | — | 静默跳过 | — | — |

## 协议兼容性（v0.3 支持）

| 厂商 | Provider 标识 | 协议 | 备注 |
|------|---------------|------|------|
| OpenAI | `openai` | `POST {baseUrl}/chat/completions` | 官方 |
| DeepSeek | `deepseek` | OpenAI 兼容 | 默认配置 |
| 通义千问 DashScope | `qwen` | OpenAI 兼容模式 `https://dashscope.aliyuncs.com/compatible-mode/v1` | OK |
| 月之暗面 Kimi | `kimi` | OpenAI 兼容 `https://api.moonshot.cn/v1` | OK |
| 智谱 GLM | `glm` | OpenAI 兼容 `https://open.bigmodel.cn/api/paas/v4` | OK |
| 书生 InternLM | `internlm` | OpenAI 兼容 | OK |
| 字节豆包 | `doubao` | OpenAI 兼容（火山引擎） | OK |
| 腾讯混元 | `hunyuan` | OpenAI 兼容 | OK |
| 零一万物 | `yi` | OpenAI 兼容 `https://api.lingyiwanwu.com/v1` | OK |
| Claude | — | — | **v0.4 接入** |
| 自建协议 | — | — | **v0.4+ 接入** |

## 当前状态（v0.3 收尾）

- [x] DTO 协议类（OpenAI 兼容）
- [x] WhitelistInterceptor + OkHttpClientFactory
- [x] OpenAiApiService + 动态 baseUrl 工厂
- [x] StreamingCallExecutor（SSE 解析 + 错误处理）
- [x] ModelApiRepository 接口 + 实现
- [x] Hilt DI 绑定
- [x] ConfigRepository 委托给 ModelApiRepository
- [x] 17 个单元测试（5 + 4 + 8）全绿
- [x] assembleDebug 通过
- [ ] callWithImage 多模态（v0.4 链路 B）
- [ ] 流式重试（v0.4 UseCase 层）
- [ ] Token 计费持久化（v0.5 接 HistoryRepository）
- [ ] Claude 协议（v0.4）
- [ ] 自定义请求模板（v0.6）

## 关键决策记录

1. **Provider<ConfigRepository> 打破循环依赖**
   `ConfigRepository.testConnection` 委托给 `ModelApiRepository.testConnection`，
   而 ModelApiRepository 自身又需要从 ConfigRepository 读配置。
   Hilt 用 `javax.inject.Provider<T>` 延迟注入避免构造期死锁。

2. **流式调用不在 Repository 层重试**
   流中断后重发会导致「已生成的文本」丢失且不可幂等（LLM 生成是概率性的）。
   错误事件透传，由 UseCase 层根据 retryable 决定是否在流开启前重试。

3. **baseUrl 由用户配置完整 path**
   `apiUrl = "https://api.deepseek.com/v1"` → service path = `chat/completions`。
   不做「自动补 /v1」猜测——避免厂商差异导致调用失败。

4. **JSON 配置：ignoreUnknownKeys = true**
   不同厂商扩展字段差异巨大（DeepSeek 的 `prefix`、Claude 的 `thinking`、OpenAI 的 `system_fingerprint`）。
   忽略未知字段保证前向兼容。

5. **唯一网络出口铁闸**
   `WhitelistInterceptor` 是单例 + `@Volatile allowedHost`，每次 `call`/`testConnection` 前由
   `ModelApiRepositoryImpl` 同步更新。配置变更不需要重建 OkHttpClient（连接池复用）。

## 测试覆盖

| 测试类 | 数量 | 覆盖点 |
|--------|------|--------|
| `WhitelistInterceptorTest` | 5 | 通过 / 拒绝 / null / 大小写 / 端口 |
| `StreamingCallExecutorTest` | 4 | 正常流 / 401 / 500 / 坏 JSON 跳过 |
| `ModelApiRepositoryImplTest` | 8 | testConnection 全部路径 + call + 白名单更新 |

**总计 17 个测试全绿**，0 failures, 0 ignored，2.185s 跑完。

## 风险点（v0.4 待解决）

- 不同 Provider 的流式协议差异巨大（虽兼容 90%，但首包超时、keep-alive 各异）
- 长连接超时配置：5 分钟对长对话仍可能不够
- 重试可能导致重复扣费（必须按 idempotency-key 去重，但 LLM 不支持）
- 中文 Provider 的内容过滤可能误杀无害 prompt

---

_初夏 2026-07-18 维护 — 这是 v0.3 的实现纪要，v0.4 会补多模态和流式重试。_
