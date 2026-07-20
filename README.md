# ChatAssist

> 一款本地优先的 Android 聊天辅助 App。
> 屏幕文本双通道（无障碍读屏优先 + 截图 OCR 兜底）→ 解析 → 大模型 → 悬浮窗建议回复。
> **纯本地运行，唯一网络出口是你自配的大模型 API。**

## 当前进度

> 工程已迭代至 **v0.21.0（预设去国外化 + 品牌二级菜单）**：在 v0.18.2 基础上新增 Token 精确统计（输入/输出/cache 分别计、模型未返回时字符估算兜底）+ 模型定价表（7 条内置、用户可编辑）+ 费用计算（微元、cache 折扣）+ 日/周/月/全部聚合 + 月度预算提醒（80%/100% 预警、超额可拦截）+ 用量统计页（预算卡/范围切换/概览/按模型/按App/隐私说明/CSV 导出/清空）+ 定价管理页 + 主流程集成（生成前预算拦截、成功失败都记录）+ 设置页预算卡；历史链路仍保留——双通道捕获 → 解析（置信度+App 预设+多气泡合并+滚动去重+UI 模式检测）→ 流式 LLM 回复 → 悬浮窗展示 → 全局错误兜底 → 后台保活（FGS 启动时机已修）→ 全量测试基础设施（JaCoCo + Paparazzi + androidTest + Macrobenchmark）→ **UI 全中文化**（DomainLabels 枚举→中文显示 / Settings+About+Main 全英文 label 改中文 / 半角冒号→全角）→ **Prompt 缓存命中优化**（System 块 100% 稳定 / OCR 提示移到 User 块置顶 / 命中 OpenAI/Anthropic/DeepSeek prefix 缓存）→ **HiltTestRunner 修复**（v0.12 移除 WorkManagerInitializer 后 androidTest 启动崩 / 改用 KeepAliveManager.runCatching 兜底）→ **v0.16.1 设置页 3 处 Row+Button 改 FlowRow+FilterChip**（主题模式 3 / 触发方式 4 / 对话目标 6 三处横向挤压修复 + 选中态视觉反馈加强）→ **v0.17.0 触发方式精简 4→2**（仅留「悬浮窗生成按钮」+「侧边条滑动触发」，删 NOTIFICATION/GESTURE）+ **对话目标→对话风格重命名**（枚举 ConversationGoal→ConversationStyle / 字段 goal→style / 段名保留「目标语气」保 cache 稳定 / 新单测验证切换后立即注入新风格不叠加旧风格）+ **修复切换触发方式再返回主页闪退**（ConfigRepositoryImpl 换强容错 Json：coerceInputValues + ignoreUnknownKeys + isLenient / OverlayController 视图自动对齐）→ **v0.18.0 双向人设（v2.2 §13.1）**（SelfPersona/CounterpartPersona/PersonaProfile 领域模型 + PersonaMatcher 三级匹配：锁定>包名>关键词>默认 + BuildPromptUseCase 注入双向人设 + 设置页人设方案列表/编辑入口 + 绑定包名常用 App 预设 + v0.18.1 对方人设详情(喜好/讨厌/雷区/口头习惯)并确认逐项注入）→ **v0.18.2 移除首页回复建议模块**（哥哥下旨：删首页冗余「回复建议」卡片含其「生成」闪退入口，悬浮窗生成链路完整保留）→ **198 单测 + 5 仪器测试 = 203 测试 0 失败**，mumu 7555 实测 1.49s 全过；`assembleDebug` + `testDebugUnitTest` 绿。
- **v0.20.0 — 多模型配置方案（v2.2 §13.4）**：`ModelConfig`/`ModelScheme` 领域模型（`deriveProvider` 推导 provider）+ `SecureKeyStorage` 按别名多 key 隔离 + `PresetModels.BUILTIN_PRESETS`（离线内置 12 条含国外模型）+ `ModelSchemeRepository` 写价入计费 + 设置页方案列表/编辑（预设选择、测试连接）+ 删过时定价管理页；`assembleDebug` + `testDebugUnitTest` 绿。
- **v0.20.1 — 价格一致性补丁**：`deriveProvider` 补 `gemini` 分支（原漏导致落 `custom` 计费静默 0）+ `PresetModel.provider` 改 computed 防漂移 + 新增 `PricingConsistencyTest` 锁死；`assembleDebug` + `testDebugUnitTest` 绿。
- **v0.21.0 — 预设去国外化 + 品牌二级菜单**：预设移除全部国外模型（仅留自定义）；新增 `PresetBrand` 品牌分组 + `BUILTIN_PRESETS` 平铺派生；覆盖 9 大国内品牌 38 模型（联网核对官方价，≈ 为估算）；`PricingSeed` 由预设派生根除双源漂移；`deriveProvider` 补国内分支；`ModelSchemeEditScreen` 预设对话框改品牌分组 + 二级菜单展开；`PricingConsistencyTest` 升级；`assembleDebug` + `testDebugUnitTest` 绿，待模拟器点验 UI。

- [x] **v0.1.0 — 工程骨架**：Gradle 8.10.2 + AGP 8.6.1 + Kotlin 2.0.21，Compose + Hilt + Room + Retrofit 全家桶，占位页跑通 `assembleDebug`
- [x] **v0.2.0 — 文档骨架 + ConfigRepository + 主界面框架**：21 篇文档、DataStore + 加密 Key、Material3 主题、4 Tab 导航
- [x] **v0.3.0 — ModelApiRepository 全套网络层**：Retrofit + 流式 SSE + WhitelistInterceptor + 17 单测全绿
- [x] **v0.4.0 — 捕获通道 + 权限 + 策略状态机**：无障碍主通道 + 截图管线 + CaptureStrategyManager 降级/恢复 + 首页实时列表
- [x] **v0.5.0 — OCR 兜底通道**：MediaProjection 截图 + ML Kit 中文识别，无障碍撤→OCR_ONLY 降级
- [x] **v0.6.0 — 解析 + 悬浮窗 + LLM 回复闭环**：ChatParser 双通道 / BuildPrompt+ReplySuggestion 流式 / OverlayController 触发球+面板+复制+生成 / HYBRID 融合 / mumu 真机修复
- [x] **v0.7.0 — 模块 09 历史记录（Room 持久化）**：HistoryEntity/Dao/Database + HistoryRepository + 生成即落库（隐私模式拦截）+ 历史页真实渲染 + 来源包名追踪 + 保留策略清理
- [x] **v0.8.0 — 悬浮窗体验打磨（v2.1 §12.1）**：智能贴边吸附 / 打字机逐字 / 连续复制气泡 / 边缘触发条(EDGE_BAR) / 自动粘贴 + 设置三开关
- [x] **v0.9.0 — OCR 形近字纠错（实测验证）**：定位根因为 OCR_ONLY 通道 ML Kit 形近字（「爱妻者」→「愛麦考」）；BuildPromptUseCase 注入 OCR 语义纠错提示；实现摆设开关 ocrAutoCorrect（繁→简本地归一）；mumu 实测冷启无崩溃，当前版本可运行
- [x] **v0.9.1 — 悬浮窗生成按钮灰圈修复（mumu 截图实测）**：overlay 主壳/内层/建议项带 shape 的 background+border 触发离屏不透明图层→四角填灰；新增 roundRectSurface 转 draw 阶段绘制（零图层）彻底消除灰圈；assembleDebug 构建绿
- [x] **v0.10.0 — 聊天内容解析完善（模块 04 增强）**：A 置信度评分（5 维加权、UI 提示「识别不确定」）+ B App 预设规则库（v0.10.1 精简为 5 个国内主流 IM：微信/QQ/钉钉/飞书/企业微信） + C 多气泡合并（MessageMerger 三条件、7 例单测） + D 滚动续接去重（MessageDeduplicator 5min 滑动窗口、6 例单测） + E UI 模式检测（UiModeDetector 64x64 缩略图、§12.2.1） + F 系统关键词 14→25；52 单测全绿、assembleDebug 绿
- [x] **v0.10.2 — 架构 + 模块文档同步**：`docs/architecture.md` 水印 v0.1.0→v0.11.0 + 9 个 v0.1 骨架模块文档当前状态同步（01/03/05/07/08/10/11/12/13）+ 3 个顶层占位文档状态追踪全量补齐（compatibility/performance/security 勾选实际落地项）
- [x] **v0.11.0 — 模块 12 错误处理与降级（指南 §4.12 0 → 7 核心类）**：AppError sealed class 重构（Fatal/Recoverable） + ErrorClassifier 14 例异常映射 + ErrorPresenter 14 套情感化中文文案 + CrashLogger 本地脱敏落盘（Bearer/sk-/sk_/api_key= 全 REDACTED） + GlobalErrorHandler 链式 defaultUncaughtExceptionHandler + CoroutineErrorHandler 工厂 + DegradationManager 4 维 3 档降级（网络/OCR/内存/电池）；ChatAssistApp.onCreate 接入 + MainViewModel.launchSafe 全 launch 兜底；45 单测全绿、累计 97 单测 0 失败
- [x] **v0.12.0 — 模块 10 后台保活（指南 §4.10 0 → 8 核心类）**：Vendor 枚举（6 厂商 + OTHER）+ KeepAliveEvent 契约 + VendorDetector ROM 三级识别（MANUFACTURER/BRAND/系统属性） + VendorGuide 6 厂商自启动 Intent 矩阵（小米/华为/OPPO/vivo/三星/魅族 + 通用回退） + KeepAliveService ForegroundService（specialUse + 通知常驻 + START_STICKY） + KeepAliveWorker 15min 周期兜底拉活 + KeepAliveManager 协调器（StateFlow + 事件流） + KeepAliveModule Hilt Configuration.Provider；ChatAssistApp.onCreate 接入 + AndroidManifest FOREGROUND_SERVICE_SPECIAL_USE 权限 + 移除 WorkManagerInitializer；36 单测全绿、累计 133 单测 0 失败
- [x] **v0.13.0 — FGS 启动时机修复（mumu logcat 实证）**：v0.12 ChatAssistApp.onCreate 调 start() 在 uidState=CEM 时被 Android 12+ 限制拒（ForegroundServiceStartNotAllowedException），改 MainActivity.onCreate 调（uidState=TOP 时正常拉起）；mumu logcat `Background started FGS: Allowed [uidState: TOP]` 实证；累计 133 单测 0 失败
- [x] **v0.16.0 — UI 全中文化 + Prompt 缓存命中优化**：① 新增 `domain/model/DomainLabels.kt`（ThemeMode/TriggerMode/ConversationGoal/PowerMode/RecognitionMode/ModelType 枚举→中文显示）；② SettingsScreen 全英文 label → 中文（"API URL"→"接口地址"、"API Key (加密存储)"→"接口密钥（加密存储）"、"模型名"→"模型名称"、"API 已配置"→"接口已配置"）；③ AboutScreen v0.2.0→v0.16.0、"GitHub: <待填写>"→"代码仓库：暂未公开"；④ MainScreen "URL: $apiUrl"→"接口地址：$apiUrl"、"Model: $model"→"模型：$model"；⑤ BuildPromptUseCase 缓存命中优化（OCR 提示从 system 移到 user 块置顶，system 块 100% 稳定）；⑥ HiltTestRunner 修复（移除 WorkManager.initialize 手动 init，KeepAliveManager.runCatching 兜底）；累计 185 单测 + 5 仪器测试 = 190 测试 0 失败
- [x] **v0.15.0 — 模块 13 性能 + 仪器测试**：① Macrobenchmark 独立模块（`:benchmark` 用 `com.android.test` plugin + `targetProjectPath = ":app"` + self-instrumenting）配 `app:benchmark` buildType（继承 release + isDebuggable=false + matchingFallbacks）；② 4 项 KPI 基准（KPI-1 冷/热启动 StartupTimingMetric / KPI-2 历史页滚动 FrameTimingMetric / KPI-3 悬浮窗显示延迟 / KPI-4 Tab 导航帧率）；③ androidTest 仪器测试套件（HiltTestRunner + HiltTestApplication 隔离 + MainActivityInstrumentedTest 2 + HistoryDaoInstrumentedTest 3，mumu 7555 全过 5/5）；④ ChatAssistRoot 加 testTag-as-resourceId 锚点（`chatassist_root_layout` 给 By.res 用）；⑤ `<profileable android:shell="true"/>` 启用 trace；累计 185 单测 + 5 仪器测试 = 190 测试 0 失败

> ⚠️ **关于 APK 体积**：本项目不限制 APK 体积，以功能完整性优先（详见 v0.2 决策）。Debug 包因带 ML Kit 中文识别模型（≈20MB）较大属正常。

## 路线图

按 [`聊天辅助App开发指南-v2.md`](./聊天辅助App开发指南-v2.md) 第 11 章 14 步 + v2.1/v2.2 增补，已完成 1–8 步 + 模块09（v0.7.0）+ 悬浮窗打磨（v0.8.0）+ OCR 形近字纠错（v0.9.0）+ 悬浮窗灰圈修复（v0.9.1）：

**已完成（✅）**
1. 工程骨架 ✅　2. 文档骨架 ✅　3. ConfigRepository + 主界面 ✅　4. ModelApiRepository ✅
5. ScreenCaptureRepository ✅　6. OcrRepository ✅　7. ChatParserRepository ✅　8. Prompt 构造 + 调用闭环 ✅
9. 历史记录（模块 09，v0.7.0，Room 持久化）✅
10. 悬浮窗打磨（模块 07，v0.8.0，v2.1 §12.1：贴边吸附 / 打字机 / 连续复制气泡 / 边缘触发条 / 自动粘贴）✅
11. OCR 形近字纠错（v0.9.0：BuildPrompt OCR 语义纠错提示 + ocrAutoCorrect 繁→简归一）✅
12. 悬浮窗灰圈根治（v0.9.1：draw 阶段 roundRectSurface 零图层，mumu 实测）✅
13. 聊天内容解析完善（模块 04，v0.10.0：A 置信度 / B App 预设 / C 多气泡合并 / D 滚动去重 / E UI 模式检测 / F 关键词扩充，52 单测全绿）✅
14. 架构 + 模块文档同步（v0.10.2：9 骨架模块状态 + 3 顶层占位补齐）✅
15. 错误处理与降级（模块 12，v0.11.0：7 核心类 + 45 单测，累计 97 单测全绿）✅
16. 后台保活（模块 10，v0.12.0：8 核心类 + 36 单测，累计 133 单测全绿，KeepAliveService specialUse + VendorGuide 6 厂商自启动引导）✅
17. FGS 启动时机修复（v0.13.0：mumu logcat 实证 uidState=TOP 时正常拉起，ChatAssistApp.onCreate 移到 MainActivity.onCreate）✅
18. 全量测试 + 性能调优（v0.14.0：JaCoCo 覆盖率 + Paparazzi 截图 3 张 + androidTest 框架 + 52 新增单测，累计 185 单测 0 失败，Domain 层 ≥80% 目标达标）✅
19. 性能 + 仪器测试（v0.15.0：Macrobenchmark 独立模块 + 4 项 KPI 基准 + androidTest 5 测试全过，mumu 7555 实证，累计 190 测试 0 失败）✅
20. UI 全中文化 + Prompt 缓存命中优化（v0.16.0：DomainLabels 枚举→中文 / Settings+About+Main 全英文 label 改中文 / BuildPromptUseCase System 块 100% 稳定 / HiltTestRunner 修复，累计 190 测试 0 失败）✅
21. 设置页 FlowRow + FilterChip 修复（v0.16.1：ThemeCard/TriggerCard/GoalCard 三处 Row+Button 改 FlowRow+FilterChip，横向挤压修复 + 选中态视觉反馈加强，`assembleDebug` 绿）✅
22. 触发方式精简 + 对话风格重命名 + 切换闪退修复（v0.17.0：TriggerMode 4→2（删 NOTIFICATION/GESTURE，留 MANUAL「悬浮窗生成按钮」+ EDGE_BAR「侧边条滑动触发」）/ ConversationGoal→ConversationStyle / 字段 goal→style / ConfigRepositoryImpl 强容错 Json（coerceInputValues + ignoreUnknownKeys 解决旧 DataStore JSON 反序列化崩溃）/ OverlayController 视图自动对齐 / BuildPromptUseCase 新单测「style 切换后立即注入新风格且不叠加旧风格」）✅
23. 双向人设（v0.18.0，v2.2 §13.1）+ 对方人设详情（v0.18.1）：Persona 领域模型（SelfPersona/CounterpartPersona/PersonaProfile/ResolvedPersona）+ PersonaMatcher 四级匹配（锁定>包名>关键词>默认）+ BuildPromptUseCase 注入双向人设段 + 设置页人设方案列表/编辑入口 + 绑定包名常用 App 预设（覆盖 15 个国内社交/办公 App）+ v0.18.1 对方人设详情（喜好/讨厌/雷区/口头习惯）逐字段注入 system 块并新增断言测试 ✅
24. 首页回复建议模块移除（v0.18.2，哥哥下旨）：删除首页冗余「回复建议」卡片（「生成」按钮 + 流式/候选/复制列表）及其「生成」闪退入口，悬浮窗生成链路（replyUseCase/generateReply/overlay.generateHandler）完整保留 ✅
25. 计费统计（v0.19.0，v2.2 §13.3）：Token 精确统计（输入/输出/cache 分别计 + 字符估算兜底）+ 模型定价表（7 条内置、用户可编辑）+ 费用计算（微元、cache 折扣）+ 日/周/月/全部聚合 + 月度预算提醒（80%/100% 预警、超额可拦截）+ 用量统计页（预算卡/范围切换/概览/按模型/按App/隐私说明/CSV 导出/清空）+ 定价管理页 + 主流程集成（生成前预算拦截、成功失败都记录）+ 设置页预算卡；新增 5 个计费单测类 0 失败、ReplySuggestionUseCaseTest 改写；全量单测 0 失败 ✅
26. 多模型配置方案（v0.20.0，v2.2 §13.4）：`ModelConfig`/`ModelScheme` 领域模型 + `deriveProvider` 推导 provider + `SecureKeyStorage` 按别名多 key 隔离 + 离线内置预设 12 条 + 方案列表/编辑（预设选择、测试连接）+ 删过时定价管理页 ✅
27. 价格一致性补丁（v0.20.1）：`deriveProvider` 补 `gemini` 分支 + `PresetModel.provider` 改 computed 防漂移 + 新增 `PricingConsistencyTest` ✅
28. 预设去国外化 + 品牌二级菜单（v0.21.0）：移除国外模型（仅留自定义）+ `PresetBrand` 品牌分组 + `BUILTIN_PRESETS` 平铺派生 + 9 大国内品牌 38 模型联网核价 + `PricingSeed` 由预设派生根除双源漂移 + `ModelSchemeEditScreen` 品牌二级菜单 ✅

**待办（⬜）**
1. v0.20 i18n + 主题资源（strings.xml 50+ 条 + values-night / values-zh-rCN 拆分）
2. 兼容性适配（模块 13：MIUI/HyperOS/EMUI/ColorOS/OriginOS/One UI 真机回归）
3. 用户文档（模块 14：`docs/user-guide.md` 完善 + 截图）
4. 设置页「自启动引导」入口（接 VendorGuide，v0.12 协调器已落地，UI 入口留 v0.18+）
5. 开机自启 BroadcastReceiver（监听 BOOT_COMPLETED 拉起 KeepAliveService，可选 v0.18+）
6. BuildPromptUseCase 缓存优化（v0.17 待办：style 段挪到 user 块 → 切换不影响 prefix cache miss，待哥哥决定）

## 快速开始

```bash
# 克隆
git clone https://github.com/YODA9527/AI-ChatAssist.git
cd ChatAssist

# 首次打开项目（推荐 Android Studio Hedgehog 或更新）
# Studio 会自动下载 Gradle Wrapper 和依赖

# 命令行构建 Debug APK
./gradlew :app:assembleDebug
```

## 文档

- [聊天辅助App开发指南-v2.md](./聊天辅助App开发指南-v2.md) — 功能规范依据（含 v2.1/v2.2 增补：双向人设/计费/语气/社交画像）
- [PROJECT_KNOWLEDGE.md](./PROJECT_KNOWLEDGE.md) — 项目知识图谱
- `docs/` — 交付文档（架构、模块、API、兼容性、性能、安全、用户指南、构建）

## 许可证

待定（项目未发布前先不放 LICENSE）。
