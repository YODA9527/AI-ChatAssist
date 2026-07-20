# ChatAssist — 架构设计

> 本文档为 ChatAssist 的唯一架构参考，描述系统的分层、模块职责、数据流与关键决策。
> 需求方与开发者可凭此文档理解全貌，不必再读源文档第三章。

---

## 1. 一句话定义

ChatAssist 是一款 Android 本地聊天辅助 App：**屏幕文本提取（无障碍读屏优先 + 截图 OCR 兜底）→ 解析 → 大模型 → 悬浮窗建议回复**。
除调用大模型 API 这一项网络出口外，所有处理（OCR、解析、Prompt 构造、UI、存储）100% 端侧完成。

## 2. 设计原则（最高优先级）

| # | 原则 | 含义 |
|---|------|------|
| 1 | **纯本地优先** | 禁止云端后端、统计 SDK、广告 SDK、第三方账号 |
| 2 | **零容忍质量** | 0 崩溃、0 泄漏、0 静默吞错、0 权限滥用 |
| 3 | **思考比需求方更多** | 主动补齐所有边缘场景（保活、OCR 兜底、API 重试、上下文截断、UI 差异、ROM 差异等） |
| 4 | **功能完整优先** | v0.2+ 取消 APK 体积硬性限制，以功能完整性为第一目标 |

## 3. 技术栈（不可更改）

| 层 | 选型 | 备注 |
|---|------|------|
| 语言 | Kotlin 2.0.21（JVM 17） | 全 Kotlin，无 Java 业务代码 |
| SDK | minSdk 26 / target 35 / compile 35 | 覆盖 2026 年活跃设备 98%+ |
| UI | Jetpack Compose + Material3 | 全部页面 Compose 实现，零 XML 布局 |
| 架构 | 单 Activity + Compose Nav + MVVM + Clean | 经典四层：UI → Presentation → Domain → Data |
| 异步 | Coroutines + Flow | 官方推荐，禁 GlobalScope / runBlocking |
| DI | Hilt 2.52 + hilt-work | KSP1 处理（Hilt 2.52 与 KSP2 不兼容） |
| 存储 | DataStore (Preferences) + Room | 配置走 DataStore，历史/计费走 Room |
| 网络 | Retrofit2 + OkHttp4 + Kotlinx Serialization | 流式响应走 OkHttp SSE |
| OCR | ML Kit Text Recognition v2（中文+拉丁） | 端侧离线，体积约 20MB |
| 屏幕 | AccessibilityService（优先）+ MediaProjection+OCR（兜底） | 双通道混合：无障碍读屏最快最省电，截图OCR兜底自绘UI（抖音/小红书） |
| 悬浮窗 | WindowManager + ComposeView | 统一 UI 体系 |
| 构建 | Gradle 8.10.2 + AGP 8.6.1 + KTS + Version Catalog | 依赖单一来源 |
| 测试 | JUnit5 + Turbine + MockK + Paparazzi + Macrobenchmark | 全链路 |
| Lint | ktlint + detekt（最严） | 提交门禁 |

**禁止引入**：RxJava、ButterKnife、EventBus、第三方 OCR SDK、Firebase、广告 SDK、Shizuku、root 依赖。

## 4. 分层架构

```
┌──────────────────────────────────────────────────────────┐
│  UI Layer (Compose)                                      │
│  ├ 悬浮窗 Surface (触发球 / 边缘条 / 建议面板)            │
│  ├ 主界面 (Compose Navigation 路由)                       │
│  └ 主题 (Material3 + Dynamic Color)                      │
└──────────────────┬───────────────────────────────────────┘
                   ↓ StateFlow
┌──────────────────────────────────────────────────────────┐
│  Presentation Layer (ViewModel + UseCase 调用)           │
│  ├ 屏幕状态管理 (Idle/Capturing/OCRing/...)              │
│  └ 用户意图分发 (onRecognize/onCopy/onSettings)          │
└──────────────────┬───────────────────────────────────────┘
                   ↓ suspend / Flow
┌──────────────────────────────────────────────────────────┐
│  Domain Layer (UseCase 业务编排)                         │
│  ├ CaptureChatUseCase                                    │
│  ├ RecognizeTextUseCase                                  │
│  ├ ParseChatUseCase                                      │
│  ├ BuildPromptUseCase                                    │
│  ├ CallModelUseCase                                      │
│  └ ManageHistoryUseCase                                  │
└──────────────────┬───────────────────────────────────────┘
                   ↓
┌──────────────────────────────────────────────────────────┐
│  Data Layer (Repository 数据源封装)                      │
│  ├ ScreenCaptureRepository                               │
│  ├ OcrRepository                                         │
│  ├ ChatParserRepository                                  │
│  ├ ModelApiRepository                                    │
│  ├ ConfigRepository                                      │
│  └ HistoryRepository                                     │
│  ├ UsageRepository（计费记录）                            │
│  └ PricingRepository（模型定价）                          │
└──────────────────┬───────────────────────────────────────┘
                   ↓
┌──────────────────────────────────────────────────────────┐
│  Platform Layer (Android Framework)                      │
│  ├ AccessibilityService (无障碍读屏，主通道)             │
│  ├ MediaProjection (截图OCR，兜底通道)                    │
│  ├ WindowManager (悬浮窗)                                │
│  ├ Notification (前台服务通知)                            │
│  ├ ML Kit (OCR 引擎)                                    │
│  └ OkHttp (网络，WhitelistInterceptor 唯一出口)          │
└──────────────────────────────────────────────────────────┘
```

**铁律**：
- UI **不准**直接调用 Repository，必须经 ViewModel
- 所有 IO 必须走 `Dispatchers.IO`，UI 操作必须 `Dispatchers.Main`
- 所有 IO 资源（Cursor / InputStream / Bitmap）必须 `use {}` 管理
- 禁止 `!!` 强制解包，优先可空类型或 `by lazy`
- 禁止全局可变状态，所有状态在 ViewModel/Repository 内

## 5. 包结构

包名 `com.chatassist`，按功能分层：

```
com.chatassist/
├── ChatAssistApp.kt              # @HiltAndroidApp 入口
├── MainActivity.kt               # 唯一 Activity
├── presentation/
│   ├── main/                     # 主屏（识别状态、快捷操作）
│   ├── settings/                 # 设置页（API Key、模型、人设、主题）
│   ├── history/                  # 历史记录列表与详情
│   ├── overlay/                  # 悬浮窗 ViewModel
│   └── theme/                    # Material3 主题与动态色
├── domain/
│   ├── model/                    # ChatMessage / OcrBlock / Persona 等
│   ├── usecase/                  # 计费/预算 UseCase（CostCalculator/TokenCounter/RecordUsage/BudgetManager/BudgetNotifier）
│   └── repository/               # Repository 接口
├── data/
│   ├── capture/                  # MediaProjection 封装
│   ├── ocr/                      # ML Kit 封装
│   ├── parser/                   # 聊天内容解析
│   ├── llm/                      # LLM API + Prompt 构造
│   ├── local/                    # DataStore + Room DAO
│   └── repository/               # Repository 实现
├── di/                           # Hilt Module
├── service/                      # ForegroundService / Worker
└── util/                         # 工具类（Logger / PermissionHelper / ...）
```

## 6. 核心数据流（一次识别的端到端旅程）

```
用户手势 / 快捷键 / 通知
  ↓
Overlay.onRecognize()
  ↓
CaptureManager (无障碍读屏优先 → 截图OCR兜底 → List<ChatMessage>)
  ↓  （CaptureStrategyManager 自动降级/恢复：TEXT_ONLY ↔ OCR_ONLY ↔ HYBRID）
OcrRepository (ML Kit 端侧识别 → List<OcrBlock>，仅兜底通道使用)
  ↓
ChatParserRepository (位置聚类 + 昵称识别 → List<ChatMessage>)
  ↓
PersonaMatcher (双向人设 + 语气模板匹配)
  ↓
BuildPromptUseCase (拼 System + Messages + History)
  ↓
ModelApiRepository (流式调用 → Flow<ModelCallEvent>)
  ↓
Overlay (打字机效果展示)
  ↓
HistoryRepository (Room 持久化)
  ↓
UsageRepository (Room 计费记录：成功/失败都写，预算通知)
```

**唯一网络出口**：用户配置的模型 API URL。OkHttp `WhitelistInterceptor` 强制白名单，禁止任何其他域名请求。

### 6.1 屏幕文本捕获策略（混合方案，2026-07-18 决策）

- **主通道（TEXT_ONLY）**：`AccessibilityService` 读节点树，实时提取聊天文本。最快最省电，原生 UI 的 App 准确率近 100%，并能拿到「对方正在输入」等状态。
- **兜底通道（OCR_ONLY）**：`MediaProjection` 截图 + ML Kit OCR。覆盖无障碍读不到的自绘 UI（抖音 / 小红书 / 部分游戏内聊天）。
- **自动降级 / 恢复**：`CaptureStrategyManager` 监测连续空结果 / 低置信度（默认 3 次）→ 降级到 OCR_ONLY；OCR_ONLY 下每 30s 探一次无障碍，5s 内有有效结果即切回 TEXT_ONLY。
- **决策依据**：推翻 v2「纯 OCR」立场（v2 因无障碍对自绘 UI 失效而否决其为主方案）；新指南第 5 章主张无障碍优先 + OCR 兜底，两者互补，兼容性 + 性能兼得。功能范围仍以 v2 指南（含 v2.1/v2.2 双向人设、计费、语气、社交画像等）为准。
- **包名**：保持 `com.chatassist` 不变（新指南的 `com.aichatassistant` 仅作命名参考，不沿用）。

## 7. 关键状态机

### 7.1 权限状态机

```
NotRequested ──request()──→ Requesting
                                  ↓ 用户授权        ↓ 用户拒绝
                              Granted            Denied
                                  ↑                 │
                                  └─ 用户重试 ─────┘
Revoked (系统撤销) ──handleOnResume()──→ Requesting
```

### 7.2 识别全流程状态机

```
Idle ──onRecognize()──→ Capturing
                          ↓ 拿到 Bitmap
                       OCRing
                          ↓ 拿到 blocks
                       Parsing
                          ↓ 拿到 messages
                       BuildingPrompt
                          ↓ 拿到 prompt
                       CallingModel
                          ↓ 流式增量
                       Streaming
                          ↓ [DONE]
                       Done
                          ↓
                       Showing (用户看到)
                          ↓
                       Idle
                            ↘ 错误
                            Failed ──用户重试/取消──→ Idle
```

## 8. 性能指标

| 指标 | 目标 | 说明 |
|------|------|------|
| 冷启动可交互 | ≤ 800ms | Macrobenchmark |
| 热启动 | ≤ 300ms | Macrobenchmark |
| 触发→出结果 P50 | ≤ 3s | 真机埋点 |
| 触发→出结果 P95 | ≤ 6s | 真机埋点 |
| OCR 单次 | ≤ 500ms | 真机埋点 |
| 截图单次 | ≤ 100ms | 真机埋点 |
| 悬浮窗动画 | ≥ 55fps | Perfetto |
| 内存峰值（1h） | ≤ 250MB | Android Profiler |
| 后台待机 1h 耗电 | ≤ 2% | 真机实测 |
| ~~APK 体积~~ | ~~≤ 60MB~~ | **已取消硬性限制**（v0.2 起以功能完整优先） |
| Crash / ANR | 0 | 真机测试 |

## 9. 兼容性范围

**必测 ROM**：原生 Android 8/10/12/14/15、MIUI、HyperOS、EMUI/鸿蒙、ColorOS、OriginOS、One UI、Flyme、ZUI

**必测形态**：6.1" 直屏、6.7" 大屏、折叠屏内屏、平板（不强制优化但不可崩）

**已知坑**（详见 [compatibility.md](./compatibility.md)）：
- MIUI 悬浮窗权限名特殊
- 华为无 GMS → ML Kit 必用 bundled 版
- Android 14 前台服务类型声明
- Android 12+ PendingIntent FLAG_IMMUTABLE

## 10. 隐私红线

- API Key → EncryptedSharedPreferences（Android Keystore）
- 历史记录 → Room 数据库，永不出现 API Key
- 截图内存 → 用完立即清零
- Release 日志 → 全部脱敏
- 隐私模式 → 不写历史、不缓存截图
- 唯一网络出口 → OkHttp WhitelistInterceptor 白名单

## 11. 文档索引

| 文档 | 内容 |
|------|------|
| [architecture.md](./architecture.md) | 本文档 |
| [modules/](./modules/) | 14 个模块的详细设计 |
| [api-contract.md](./api-contract.md) | 模块间接口契约表 |
| [compatibility.md](./compatibility.md) | 兼容性测试报告 |
| [performance.md](./performance.md) | 性能测试报告 |
| [security.md](./security.md) | 数据流审计与隐私分析 |
| [user-guide.md](./user-guide.md) | 面向需求方的使用手册 |
| [build.md](./build.md) | 构建与签名说明 |
| [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) | 已知踩坑与修复 |

---

_初夏 2026-07-19 自动构建于 v0.19.0（v0.10.0 解析完善 + v0.11.0 模块 12 错误处理框架 + v0.12.0 模块 10 后台保活 + v0.13.0 FGS 启动时机修复 + v0.14.0 测试基建 + v0.15.0 性能 + 仪器测试 + v0.16.0 UI 全中文化 + v0.17.0 触发精简/对话风格重命名 + v0.18.0 双向人设 + v0.19.0 计费统计）_
