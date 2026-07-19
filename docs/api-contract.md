# API 契约表

> ChatAssist 内部模块间接口与数据模型契约。
> 所有 `data class` 字段不可空且有序（除特别标注），序列化统一用 Kotlinx Serialization。

---

## A.1 模块间接口

| 提供方 | 消费方 | 接口 | 输入 | 输出 |
|--------|--------|------|------|------|
| `PermissionManager` | UI / VM | `allGranted: StateFlow<Boolean>` | — | Boolean |
| `ScreenCaptureRepository` | `CaptureChatUseCase` | `capture()` | `CaptureConfig` | `Result<CaptureResult>` |
| `OcrRepository` | `RecognizeTextUseCase` | `recognize(bitmap)` | `Bitmap` | `Result<OcrResult>` |
| `ChatParserRepository` | `ParseChatUseCase` | `parse(blocks, pkg, w)` | 三参 | `Result<ParseResult>` |
| `BuildPromptUseCase` | `CallModelUseCase` | `invoke(msgs, persona, goal)` | 三参 | `Result<BuiltPrompt>` |
| `ModelApiRepository` | `CallModelUseCase` | `call(prompt)` | `BuiltPrompt` | `Flow<ModelCallEvent>` |
| `HistoryRepository` | `ManageHistoryUseCase` | `record(entity)` | `HistoryEntity` | `Long` |
| `OverlayController` | `OverlayViewModel` | `expand()` / `collapse()` | — | — |

## A.2 数据模型契约（核心类型）

| 类型 | 字段 | 说明 |
|------|------|------|
| `CaptureConfig` | width, height, dpi, cropRect? | 截屏配置 |
| `CaptureResult` | bitmap, capturedAt, width, height | 截屏结果 |
| `OcrBlock` | text, boundingBox, confidence, lines | OCR 文本块 |
| `OcrResult` | blocks, recognizedAt, width, height | OCR 整体结果 |
| `ChatMessage` | sender, content, timestamp?, confidence, rawBlocks | 解析后的消息 |
| `ParseResult` | messages, parsedAt, targetPackage, overallConfidence | 解析结果 |
| `SelfPersona` | name, identity, speakingStyle, taboo, signature | 己方人设 |
| `CounterpartPersona` | name, identity, relationship, theirStyle, theirPersonality, dynamic, goal | 对方人设 |
| `DualPersona` | self, counterpart | 双向人设 |
| `PersonaProfile` | id, label, dual, bindPackage?, bindKeyword?, createdAt, lastUsedAt | 人设配置档案 |
| `BuiltPrompt` | systemPrompt, messages, temperature, maxTokens, model | 构造好的 Prompt |
| `TokenUsage` | promptTokens, completionTokens, totalTokens | Token 用量 |
| `ModelChainConfig` | id, name, recognitionMode, modelType, provider, model, isDefault | 模型链路配置 |
| `HistoryEntity` | id, capturedAt, targetPackage, messagesJson, promptJson, responseText, promptTokens, completionTokens, totalTokens, cost, chainId | 历史记录 |
| `AppError` | code, userMessage, technicalDetail, retryable, suggestedAction? | 应用错误 |

## A.3 错误码契约

详见 [§B 完整错误码表](#b-完整错误码表)。

---

## B. 完整错误码表

| ErrorCode | HTTP/类型 | userMessage（中文） | retryable | 处理 |
|-----------|-----------|---------------------|-----------|------|
| `NETWORK` | IOException | 请检查网络 | 是 | 指数退避重试 |
| `DNS` | UnknownHostException | 请检查 API URL 是否正确 | 否 | 引导改配置 |
| `AUTH_401` | 401 | API Key 无效 | 否 | 引导改 Key |
| `AUTH_403` | 403 | 无权限访问该模型 | 否 | 引导换模型 |
| `RATE_LIMIT_429` | 429 | 请求过于频繁，已自动等待重试 | 是 | 指数退避 |
| `SERVER_5XX` | 500-599 | 模型服务异常，已重试 | 是 | 最多 3 次 |
| `TIMEOUT` | SocketTimeoutException | 模型响应超时，可重试或换模型 | 是 | 用户可手动重试 |
| `OCR_LOW_CONFIDENCE` | 内部 | 识别不确定，请核对 | 否 | 提示用户修正 |
| `PARSE_LOW_CONFIDENCE` | 内部 | 解析不确定，请核对 | 否 | 提示用户修正 |
| `PERMISSION_DENIED` | SecurityException | 权限被拒 | 否 | 引导开启权限 |
| `STORAGE_FULL` | IOException | 存储空间不足 | 否 | 引导清理 |
| `UNKNOWN` | — | 发生未知错误 | 是 | 重试或联系 |

---

## C. 协议版本

| 版本 | 状态 | 说明 |
|------|------|------|
| 0.1.0 | 当前 | 内部接口稳定，跨模块改动需提 PR |
