# 更新日志

本项目所有重要变更都会记录在此文件。
格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## 关键决策（贯穿 v0.4–v0.6，2026-07-18）

- **捕获方案定为混合**：无障碍读屏（AccessibilityService）为首选主通道，MediaProjection+OCR 为降级兜底通道，由 `CaptureStrategyManager` 自动降级 / 恢复。推翻 v2「纯 OCR」立场，采用新指南第 5 章方案（兼容性 + 性能兼得）。
- **包名保留 `com.chatassist`**：不沿用新指南的 `com.aichatassistant`，避免无谓改名。
- **功能范围以 v2 指南为准**：保留 v2.1/v2.2 增补（双向人设、计费统计、自定义语气、社交画像、边缘触发条、智能贴边、精准模式、多模态 Vision 预留、本地小模型 v3.0 预留等）。新指南作结构基线，不含上述加料。
- **影响文档**：`docs/architecture.md`、`docs/modules/02-屏幕捕获.md`、`README.md`、`PROJECT_KNOWLEDGE.md` 已同步覆写。

## [0.9.1] - 2026-07-19

### 修复（悬浮窗生成按钮四周灰色圈，mumu 截图实测验证）
- **根因定位**：v0.8/v0.9 仅把 `dropShadow`/`shadow`/`clip` 改成 draw 阶段，却漏了 `OverlayContent` 主壳仍在用 `.background(bgColor, shape)` + `.border(BorderStroke, shape)` —— 带 `shape` 参数即触发 clip → 离屏不透明图层 → 圆角外被填灰；内层高光 `Box` 也用 `background(Brush, shape)`；建议项 `background(primaryContainer, shapes.medium)`。这层漏网图层正是灰圈来源。
- **修复**：新增 `roundRectSurface`（draw 阶段 `drawWithContent` + `drawPath(path,color/brush)` 画圆角填充/渐变 + `Stroke` 描边 + 多层 `drawPath` 阴影，零 graphicsLayer），替换主壳与内层全部带 `shape` 的 `background`/`border`；建议气泡项 `background(primaryContainer, shapes.medium)` 一并改 `roundRectSurface(8.dp)`。删 `dropShadow`/`BorderStroke` import，加 `Stroke` import。
- **构建结果**：✅ `:app:assembleDebug` 成功（BUILD SUCCESSFUL 5s）
- **注**：哥哥此前定调"悬浮窗背景不弄了，留白色按键"，本次仅治灰圈、不改外观形态。

## [0.9.0] - 2026-07-19

### 修复（OCR 形近字识别偏差，实测验证）
- **根因定位**：实测环境走的是 `OCR_ONLY` 通道（库里每条消息 `source` 全为 `OCR_ONLY`，说明当时无障碍未授权，策略机 `CaptureStrategyManager.recomputeMode` 自动降级到截图 OCR）。OCR 用 ML Kit 中文识别，对复杂汉字出形近字——如「爱妻者风生水起」被识成「愛麦考风生水起」（妻→麦、者→考为形近字，愛为繁体），「风生水起」识别正确。属 OCR 引擎固有局限，非解析/保存 bug。
- **prompt 语义纠错（零额外调用）**：`BuildPromptUseCase` 检测对话含 `CaptureMode.OCR_ONLY` 时，在 system 指令追加「OCR 文本提示」段，引导 LLM 结合上下文语义自动纠正形近字/繁体后再生成回复（不在回复中复述错误）。立刻改善回复质量。
- **实现摆设开关 `ocrAutoCorrect`**：该配置此前全工程仅定义、零调用。`OcrCaptureChannel.recognize` 识别后接本地零风险繁→简归一（新增 `TRAD_TO_SIMP` 映射表，仅当 raw 含繁体字才替换，绝不误伤简体原文）；`Config.ocrAutoCorrect` 默认值 `false` → `true`。
- **治本建议**：开启无障碍服务走 `TEXT_ONLY` 拿 View 精确 text，可彻底消除形近字（OCR 仅兜底）。`RecognitionMode.VISION` 多模态视觉识别（Config 已预留枚举未实现）是 OCR 通道长期升级方向。

### 构建结果
- ✅ `:app:assembleDebug` 成功（BUILD SUCCESSFUL）
- ✅ mumu 模拟器安装冷启无 app 崩溃（logcat 中 "FATAL" 为 monkey/MUMU 自身日志，非 app 崩溃；"Start loading thick OCR module" 证 ML Kit OCR 模块正常加载）

## [0.8.0] - 2026-07-19

### 新增（悬浮窗体验打磨，v2.1 §12.1）
- **智能贴边半透明吸附（12.1.2）**：主面板可拖拽，松手吸附最近边缘 + 静止 3s 后半透明（alpha=0.4），触摸即恢复不透明
- **打字机效果**：流式阶段逐字显示累积文本（`OverlayController.showStreaming` 接 `ReplyUiState.Streaming.text`）
- **连续复制小气泡（12.1.3）**：复制一条后收起为「下一条」气泡，连续复制多条，全部复制完自动收起
- **边缘触发条（12.1.1）**：`triggerMode = EDGE_BAR` 时改用 16dp 窄条悬浮窗，双击触发识别、拖动调位置（与生成按钮互斥）
- **自动粘贴（12.1.4）**：`autoPaste` 开启且无障碍可用时，点回复自动 `ACTION_PASTE`（精准模式专属，默认关）
- **设置开关**：`OverlayCard` 暴露 智能贴边 / 打字机 / 自动粘贴 三项

### 构建结果
- ✅ `:app:assembleDebug` 成功（BUILD SUCCESSFUL）

## [0.7.0] - 2026-07-19

### 新增（模块 09 历史记录 / Room 持久化）
- **Room 三件套**：`HistoryEntity`（`data/local/entity`）+ `HistoryDao`（`data/local/dao`，含保留策略清理语句）+ `HistoryDatabase`（`data/local`，version=1）
- **仓库**：`HistoryRepository` 接口（domain）+ `HistoryRepositoryImpl`（data），消息列表 / Prompt 以 kotlinx.serialization JSON 落库，读出失败兜底空列表
- **落库钩子**：`ReplySuggestionUseCase.generate()` 在 `StreamDone` 时写入历史；隐私模式（`ConfigRepository.privacyMode`）开启时不写
- **历史页**：`HistoryViewModel` 接 `observeAll()` 响应式流；`HistoryScreen` 真实渲染列表（时间 / 来源包名 / 对话条数 / Token / 回复预览）+ 删除 + 清空
- **来源追踪**：`CaptureResult` 增 `targetPackage`，无障碍通道经 `ChatAccessibilityService.lastPackage` 填值
- **保留策略**：按 `historyMaxAgeDays` / `historyMaxCount` 清理（保留最近 N 条 + 超龄删除）

### 构建结果
- ✅ `:app:assembleDebug` 成功（BUILD SUCCESSFUL）

## [0.6.0] - 2026-07-18

### 新增
- **ChatParser 双通道解析**（`domain/parser/ChatParser.kt`）
  - TEXT（无障碍节点）/ OCR（ML Kit 文本块）双通道接入
  - 按节点/文本块水平位置判定 `isSelf`（右侧=自己）、`sender`（我/对方/系统）
  - 从文本抠 `HH:MM` 落成当日时间戳；系统消息（撤回/时间行）单独标记
  - 无障碍走 `AccessibilityNodeInfo.bounds`，OCR 走 `boundingBox`
- **Prompt 构造 + 调用闭环**
  - `domain/usecase/BuildPromptUseCase.kt`：实时消息 + 人设 + 目标语气组装 `BuiltPrompt`
  - `domain/usecase/ReplySuggestionUseCase.kt`：经 `ModelApiRepository` 流式调用，解析 3 条候选回复，经 `StateFlow<ReplyUiState>` 暴露
- **悬浮窗（模块 07）**（`presentation/overlay/OverlayController.kt`）
  - WindowManager 叠加 ComposeView：触发球 + 建议面板 + 复制 + 生成/重试按钮
  - `FLAG_NOT_FOCUSABLE | NOT_TOUCH_MODAL`，权限不足静默不显示；App 启动随权限挂载
  - `OverlayOwner` 自建 LifecycleOwner + SavedStateRegistryOwner，注入 ViewTree
- **HYBRID 融合**：无障碍 + 投影同时授权时策略机切 `HYBRID`，`combine` 双流按内容去重（无障碍优先），`captureOnce` 同融合
- **首页增强**（`presentation/main/MainScreen.kt` + `MainViewModel.kt`）：「生成回复」按钮 + 回复建议卡（流式/候选/复制）+ 悬浮窗开关键
- **测试**：`ChatParserTest`（6 例）+ 既有 `NodeExtractorTest` 全过

### 修复（mumu 模拟器真机验证）
- 启动崩溃：`OverlayController.show()` 缺 LifecycleOwner/SavedStateRegistryOwner → 自建 `OverlayOwner` 注入 ViewTree
- 无障碍检测失灵：① debug 包名 `com.chatassist.debug` 与硬编码不匹配 → 改用 `ComponentName.flattenToString()` 动态生成；② 模拟器读 `ENABLED_ACCESSIBILITY_SERVICES` 返回 null → 主检测改 `getEnabledAccessibilityServiceList` 匹配 + settings 字符串回退双保险
- 保存 API Key / 切换模型崩溃：`ConfigRepositoryImpl` 多余成员扩展 `edit` 无限递归 → `StackOverflowError` → 删除递归扩展 + `SettingsViewModel` 加 `CoroutineExceptionHandler`
- 测试连接「API Key 无效」：`OpenAiApiService.chatCompletions` 缺 `@Header("Authorization")` → 补全并 trim key；流式路径同步 trim
- 悬浮窗缺「生成」按钮：`OverlayController` 新增 `generateHandler` 回调，`SuggestionPanel`/`ErrorPanel` 加生成/重试按钮 + 空态引导

### 构建结果
- ✅ `:app:assembleDebug` 成功（BUILD SUCCESSFUL）
- ✅ `ChatParserTest` + `NodeExtractorTest` 全过

## [0.5.0] - 2026-07-18

### 新增
- **OcrCaptureChannel 真实实现**（`data/capture/OcrCaptureChannel.kt`）
  - MediaProjection 截图 + ML Kit 中文识别，2s 轮询
  - Bitmap 即 recycle，无障碍撤 → `OCR_ONLY` 自动降级

## [0.4.0] - 2026-07-18

### 新增
- **权限 facade**（`data/permission/PermissionManager.kt`）：四状态 + 引导 UI
- **无障碍主通道**（`service/ChatAccessibilityService.kt` + `data/capture/AccessibilityCaptureChannel.kt` + `data/capture/NodeExtractor.kt`）：纯函数节点遍历
- **截图管线**（`service/CaptureForegroundService.kt` + `data/capture/ScreenCaptureManager.kt`）：VirtualDisplay + 裁剪/recycle
- **策略状态机**（`data/capture/CaptureStrategyManager.kt`）：降级/恢复
- **收口**（`domain/usecase/CaptureChatUseCase.kt` + `MainViewModel` + 首页实时列表 + 权限引导 UI）

## [0.3.0] - 2026-07-18

### 新增
- **ModelApiRepository 完整实现**（[docs/modules/06-模型API调用.md](./docs/modules/06-模型API调用.md) v0.2）
  - `data/llm/dto/`：OpenAI 兼容协议 DTO（ChatRequestDto / ChatResponseDto / ChoiceDto / DeltaDto / UsageDto / ErrorDto / StreamOptionsDto / MessageDto）
  - `data/llm/network/WhitelistInterceptor`：唯一网络出口铁闸，host 来自用户配置
  - `data/llm/network/OkHttpClientFactory`：超时 10s/5min/30s/5min + Logging 拦截器（debug only，Authorization 脱敏）
  - `data/llm/network/OpenAiApiService` + `OpenAiApiServiceFactory`：Retrofit interface + 动态 baseUrl 工厂
  - `data/llm/sse/StreamingCallExecutor`：OkHttp EventSource + callbackFlow，OpenAI 兼容 SSE 解析
  - `data/repository/ModelApiRepositoryImpl`：`call()` / `testConnection(maxAttempts)` / `callWithImage()` / `collectFullResponse()`
  - `domain/model/BuiltPrompt` + `Message` + `MessageRole`
  - `domain/model/TokenUsage`（含 `of(prompt, completion, total?)` 工厂方法）
  - `domain/model/ModelCallEvent`（sealed interface：StreamDelta / StreamDone / Error）
  - `domain/repository/ModelApiRepository`（接口 + callWithImage/collectFullResponse 占位）
- **Hilt DI 绑定**
  - `data/llm/di/NetworkModule`：`@Provides OkHttpClient` + `@Provides Json`（ignoreUnknownKeys/encodeDefaults/explicitNulls=false/isLenient/coerceInputValues）
  - `data/llm/di/NetworkBindModule`：`@Binds ModelApiRepository` → `ModelApiRepositoryImpl`
- **AndroidManifest**：新增 `INTERNET` + `ACCESS_NETWORK_STATE` 权限
- **协议兼容矩阵**：覆盖 OpenAI / DeepSeek / 通义千问 / Kimi / 智谱 GLM / 书生 / 豆包 / 混元 / 零一万物（9 家）
- **17 个单元测试全绿**（2.185s 跑完）
  - `WhitelistInterceptorTest`（5）：通过 / 拒绝 / null / 大小写 / 端口
  - `StreamingCallExecutorTest`（4）：正常流 / 401 / 500 / 坏 JSON 跳过
  - `ModelApiRepositoryImplTest`（8）：testConnection 全部路径 + call + 白名单更新

### 变更
- **删除 `data/local/ConnectionTester.kt`**：原 v0.2 轻量测试工具，统一通过 `ModelApiRepository.testConnection()` 走 WhitelistInterceptor（符合「唯一网络出口」铁律）
- **`ConfigRepositoryImpl` 委托给 `ModelApiRepository.testConnection(maxAttempts=1)`**：保持向后兼容 `SettingsViewModel` 调用
- **`build.gradle.kts` 新增 `testOptions { unitTests.all { it.useJUnitPlatform() } }`**：Android Library 默认 JUnit4，切到 JUnit 5
- **`libs.versions.toml` 新增依赖**：`kotlin-test-junit5`（kotlin.test.* 桥接 JUnit5）+ `junit-platform-launcher`（Gradle 跑 JUnit5 必需）

### 关键决策
1. **Provider<ConfigRepository> 打破循环依赖**：`ConfigRepository.testConnection` 委托 `ModelApiRepository.testConnection`，而后者又需读 `ConfigRepository`。Hilt `javax.inject.Provider<T>` 延迟注入解决构造期死锁。
2. **流式调用不在 Repository 层重试**：流中断不可幂等，错误事件透传 UseCase 层（v0.4 接入）。
3. **baseUrl 完整 path 由用户配**：`apiUrl = "https://api.deepseek.com/v1"`，service path = `chat/completions`。不做自动补 /v1。
4. **JSON `ignoreUnknownKeys = true`**：不同厂商扩展字段差异巨大，前向兼容。
5. **WhitelistInterceptor 单例 + @Volatile allowedHost**：配置变更不重建 OkHttpClient，连接池复用。

### 构建结果
- ✅ `:app:assembleDebug` 成功
- ✅ `:app:test` 17/17 通过（2.185s）

### 踩坑
- OkHttp 4 `MediaType.parse()` 仍按 v0.2 方案用 `String.toMediaType()`（延续）
- Hilt `ConfigRepository ↔ ModelApiRepository` 循环依赖 → `Provider<T>` 解决
- JUnit 5 + Android Library：必须显式 `useJUnitPlatform()`，否则静默 0 tests
- `kotlin-test-junit5` 是 JUnit5 + kotlin.test.* 的官方桥接（assertEquals/assertNotNull/assertTrue/assertThrows reified）
- mockk：`every` 不支持 suspend 函数，需用 `coEvery`（如 `coEvery { repo.snapshot() } returns ...`）
- 详见 [docs/TROUBLESHOOTING.md](./docs/TROUBLESHOOTING.md) #7、#8、#9

## [0.2.0] - 2026-07-18

### 新增
- **完整文档骨架（21 个 Markdown）**：[docs/architecture.md](./docs/architecture.md) +
  13 个模块骨架（[docs/modules/](./docs/modules/)）+ 6 类专题文档 +
  [TROUBLESHOOTING.md](./docs/TROUBLESHOOTING.md)
- **ConfigRepository + DataStore 存储**（`data/local/ConfigRepositoryImpl.kt`）
  - 25+ 配置项数据类（`domain/model/Config.kt`）
  - 加密 API Key 存储（`SecureKeyStorage` 用 EncryptedSharedPreferences + Keystore）
  - 测试连接工具（`ConnectionTester` 用 OkHttp 4 + Kotlinx Serialization）
- **DI 集成**：`data/local/di/LocalModule.kt` 提供 DataStore + 绑定 ConfigRepository
- **主题系统**（`presentation/theme/{Color,Type,Theme}.kt`）：Material3 + Dynamic Color
- **底部导航 + 4 个 Tab**（`presentation/ChatAssistRoot.kt` + `navigation/Route.kt`）
  - 首页（API 状态 / 隐私提示 / 使用步骤）
  - 设置（API / 主题 / 触发 / 隐私 / 对话目标）
  - 历史（v0.10 接入占位）
  - 关于（版本 / 技术栈 / 开源）
- **4 个 ViewModel**：Main / Settings / History / （About 无状态）
- **图标资源**：聊天气泡 adaptive icon v26+

### 构建结果
- ✅ `:app:assembleDebug` 成功，产物 `app-debug.apk`（64.75MB）
- ✅ KSP + Hilt 编译通过，Configuration Cache 复用

### 踩坑
- OkHttp 4 `MediaType.parse()` 已废弃 → 改用 `String.toMediaType()` 扩展
- `MutableStateFlow<T>(initial)` 协变推断陷阱 → 显式标注 `MutableStateFlow<T>`
- 详见 [docs/TROUBLESHOOTING.md](./docs/TROUBLESHOOTING.md) #5、#6

## [0.1.0] - 2026-07-17

### 新增
- 工程骨架：Gradle 8.10.2 + AGP 8.6.1 + Kotlin 2.0.21
- Version Catalog（`gradle/libs.versions.toml`）：Compose / Hilt / Room / Retrofit / ML Kit 全家桶
- 最小可跑 Compose 入口：`MainActivity` + `PlaceholderScreen`
- `ChatAssistApp`（@HiltAndroidApp 入口）+ Timber 日志初始化
- AndroidManifest 基础声明（启动器、主题、备份规则）
- ProGuard 规则（ML Kit / Hilt / Kotlinx Serialization / Retrofit / Room）
- Launcher Icon（聊天气泡 vector + 蓝色背景，adaptive icon v26+）

### 构建结果
- ✅ `:app:assembleDebug` 成功，产物 `app/build/outputs/apk/debug/app-debug.apk`（64.5MB）
- ⚠️ 首次 build 失败 → 修复后成功，详见 [docs/TROUBLESHOOTING.md](./docs/TROUBLESHOOTING.md)

### 决策变更
- **APK 体积取消硬性限制**：按需求方要求「功能完整性优先」，
  第六章 KPI 中「APK 体积 ≤ 60MB」改为参考值，不再作为拦截门槛。
  Debug 包较大主要因 ML Kit 中文模型 + 调试符号，Release 包经 R8 后会显著缩小。

### 已知问题
- 启动器图标暂用占位 vector，v0.2 替换为品牌设计稿
- 主题为 Material3 默认色，v0.2 应用 ChatAssist 品牌色
- Hilt 2.52 + KSP2 存在兼容性 BUG（已知），暂用 KSP1（`ksp.useKSP2=false`）
