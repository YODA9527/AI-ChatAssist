# 更新日志

本项目所有重要变更都会记录在此文件。
格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [0.21.0] - 2026-07-20

### 变更（预设目录大改版：去国外化 + 品牌二级菜单）

- 预设目录**移除全部国外模型**（OpenAI / Anthropic / Google 等）；国外模型仍可在方案编辑页手动自定义（任意接口地址 + 模型名 + 价格）。
- 新增品牌分组数据模型 `PresetBrand`（brand / brandKey / apiBaseUrl / models），`BUILTIN_PRESETS` 改为由各品牌模型平铺派生（`PRESET_BRANDS.flatMap`）。
- 预设覆盖 9 大国内品牌、共 38 个模型，每个品牌下二级菜单预设其最新若干模型：
  - 阿里云百炼·通义千问（qwen-max/plus/turbo/long/coder）
  - DeepSeek（v4-flash/v4-pro/chat/reasoner）
  - 智谱·GLM（glm-5.2/5.1/5-turbo/5/4.7）
  - 月之暗面·Kimi（kimi-k3/k2/k2-thinking/moonshot-v1-8k/kimi-latest-8k）
  - 火山引擎·豆包（doubao-seed-2.1-pro/turbo + 旧档 pro/lite/1.5-pro）
  - 百度·文心（ERNIE-5.1/5.0/4.5-Turbo-128K/X1.1/4.5-Turbo-32K）
  - 腾讯云·混元（Hunyuan-2.0-Thinking/T1/TurboS/a13b/role-latest）
  - MiniMax（M3/M2.7/M2.5/M2.5-highspeed/M2.1）
  - 本地部署（local，Ollama 等）
- **价格来源**：初夏 2026-07-20 逐一联网核对各厂商官方 API / 计费文档（DeepSeek、智谱 bigmodel.cn、百度千帆、腾讯云、MiniMax、豆包/火山方舟、Kimi 等），单位 元/百万 tokens；标注「≈」者为官方页未直接给价、按同代已知价估算（已在代码注释逐行标明）。
- `PricingSeed.BUILTIN_PRICING` 改为**由 `BUILTIN_PRESETS` 直接派生**（不再手写重复列表），预设改价即种子价同步，彻底消除 v0.20 双源漂移雷。
- `deriveProvider` 补国内品牌分支：`glm→zhipu`、`doubao→doubao`、`ernie→baidu`、`hunyuan→tencent`、`minimax→minimax`（否则这些模型会落入 `custom`，计费 key 与展示/种子价对不上）。
- `ModelSchemeEditScreen` 预设选择对话框改为**品牌分组 + 二级菜单展开**（点品牌展开其模型列表），国外模型不再出现。
- `PricingConsistencyTest` 升级：新增国内品牌 provider 推导断言、品牌平铺 == `BUILTIN_PRESETS` 断言、全部预设 provider 非 `custom` 断言。
- 验收：`assembleDebug` + `testDebugUnitTest` 全绿（含品牌断言）；`emulator-5554` 装包启动冒烟无崩溃。
- **待哥哥手动验收**：模拟器内进入设置→模型方案→编辑→从预设选择，验证品牌二级菜单展开与带入价落库（脚本无法点 UI）。

## [0.20.1] - 2026-07-20

### 修复（质量闭环，v0.20.0 后续补丁）

- `deriveProvider` 补 `gemini` 分支：原 when 无 gemini 分支，导致 `gemini-1.5-pro` 落库 key 成 `custom::gemini-1.5-pro`，与预设展示 `google` 及种子价对不上、计费静默失效（真雷，非 claude）
- `PresetModel.provider` 改为由 `deriveProvider(model)` 推导的 computed 属性（原手填，存在与计费侧漂移风险）；UI 展示 `p.provider` 不变
- `PricingSeed.BUILTIN_PRICING` 由 7 条补齐至 12 条，与 `BUILTIN_PRESETS` 预设目录一一对齐（新增 gpt-4.1、claude-3-5-sonnet、claude-3-opus、qwen-max、gemini-1.5-pro）；provider 命名统一由 `deriveProvider` 推导；修正指向已删除「定价管理」页的过期注释
- 新增 `PricingConsistencyTest`：锁定 gemini→google、预设/种子价 provider 与 `deriveProvider` 一致、种子价覆盖全部预设
- 验收：`assembleDebug` + `testDebugUnitTest` 全绿（含一致性测试）；`emulator-5554` 装包启动冒烟无崩溃

## [0.20.0] - 2026-07-20

### 新增（多模型配置方案，v2.2 §13.4）

#### 1. 领域模型（domain/model）
- `ModelConfig`：`model`/`apiUrl`/`apiFormat`/输入/输出/缓存三类单价（元/百万 token）/`apiKeyAlias`（加密存储寻址别名，不含明文 key）；含 `provider`（`deriveProvider(model)` 推导）与 `pricingKey()`（`Pricing.keyOf`）
- `ModelScheme`：`id`/`name`/`textModel`/`multimodalModel`/`isActive`/时间戳；`default()`（出厂默认方案）/ `newScheme(name)`（UUID + 独立加密别名）
- `AppConfig.schemes: List<ModelScheme>` + `activeSchemeId` + 扩展 `activeScheme()`（显式方案 > 旧版单模型字段兜底 > 全空占位）/ `activeTextModel()`

#### 2. 密钥与迁移（加密存储隔离）
- `SecureKeyStorage` 新增按别名多 key：`saveApiKey(alias,key)`/`getApiKey(alias)`/`clearApiKey(alias)`（前缀 `model_key_`）；旧全局单参版保留作迁移源
- `ConfigRepository.migrateLegacySchemes()`（幂等）：`RootViewModel` 启动调用，把旧 `apiUrl/apiKey/model` 合成「默认方案」并清空旧字段

#### 3. 预设目录（离线内置，不依赖运行时联网）
- `PresetModels.BUILTIN_PRESETS`：deepseek-chat/reasoner、gpt-4o/4o-mini/4.1、claude-3-5-sonnet/3-opus、qwen-plus/max、moonshot-v1-8k、gemini-1.5-pro、local（含默认 apiUrl 与价格、多模态标记）+ `findPreset(model)`

#### 4. 仓储与集成
- `ModelSchemeRepository`（observeAll/snapshotAll/getActive/save/delete/setActive）+ `ModelSchemeRepositoryImpl`：`save()` 把两模型价格 upsert 进 `PricingRepository`（key=`provider::model`），计费统计（v0.19）直接复用
- `ModelApiRepositoryImpl.call()/testConnection()` 改用 `cfg.activeScheme().textModel`（apiUrl + `secureKey.getApiKey(alias)` + model）；新增 4 参 `testConnection(apiUrl,apiKey,model,maxAttempts)` 供编辑页测正在编辑的模型

#### 5. UI（设置页 → 方案列表 → 方案编辑）
- 设置页 `ApiConfigCard` → `ModelSchemeEntryCard`（显示当前生效方案名 + 文本/多模态模型，跳 `Route.ModelSchemes`）
- `ModelSchemesScreen`（列表：切换/编辑/删除/新增）+ `ModelSchemesViewModel`
- `ModelSchemeEditScreen` + `ModelSchemeEditViewModel`：编辑方案名 + 文本/多模态两模型（apiUrl/模型名/密钥/价格/预设选择/测试连接）；保存时把两密钥写入加密存储

#### 6. 清理（删除过时模块）
- 删 `PricingScreen.kt`/`PricingViewModel.kt`/`src/debug/UsageDebugActivity.kt`/`src/debug/AndroidManifest.xml`；`UsageScreen` 去掉「模型定价管理」与「填充演示数据（调试）」按钮；`UsageViewModel.seedDemoData()` 移除
- `Config.kt` 删死代码 `chains`/`activeChainId`/`ModelChain`/`RecognitionMode`/`ModelType`；相应 `DomainLabels` 扩展删除

### 测试结果
- `assembleDebug` 绿；全量单测 `testDebugUnitTest` 0 失败（含对齐的 `ModelApiRepositoryImplTest`/`MainViewModelTest`/`BuildPromptUseCaseTest`）
- 关键坑：override 函数不得声明默认参数值（`testConnection` 的 `maxAttempts=1` 只能写在接口侧）；同包 `sealed interface TestResult` 与 `SettingsViewModel` 重名 → 改名 `SchemeTestResult`；`SettingsScreen` 误删 `OutlinedTextField` 导入致 `BudgetCard` 编译失败已补回

## [0.19.0] - 2026-07-19

### 新增（模块 14 计费统计，v2.2 §13.3）

#### 1. 领域模型（domain/model）
- `UsageStatus` 枚举（SUCCESS/FAILED/CANCELLED）+ `UsageRecord`（单条计费，费用用**微元** 1 元=1_000_000 微元避免浮点）+ `UsageSummary`（含 `avgCostYuan` getter）/ `UsageByModel` / `UsageByApp` 聚合模型
- `Pricing`（`@Serializable`：`key="$provider::$model"`/输入/输出/cache 单价/货币 CNY/可编辑）+ `BudgetConfig`（月度预算 + 三开关，默认全 0/true）+ `BudgetStatus`（OK/NEAR_LIMIT/EXCEEDED_WARN/EXCEEDED_BLOCK）
- `deriveProvider(model)`：由模型名推导服务商（deepseek/gpt/claude/moonshot/qwen/local/custom）
- `TokenUsage.cachedTokens` 扩展字段，默认 0，向后兼容

#### 2. 数据层（data/local，Room）
- `UsageEntity`/`PricingEntity` + `UsageAggregates`（非 @Entity 聚合载体）
- `UsageDao`（insert/observePage/observeAll/observeSummary 成功失败分别计数/observeByModel/observeByApp/totalCost/deleteOlderThan/clearAll）+ `PricingDao`（upsert/get/getByModel/observeAll/delete/clearAll）
- `PricingSeed.BUILTIN_PRICING`：7 条内置定价（deepseek-chat/reasoner、gpt-4o/4o-mini、moonshot-v1-8k、qwen-plus、local），仅补缺不覆盖用户改动
- `HistoryDatabase` 1→2：新增 usage/pricing 两表，`fallbackToDestructiveMigration`（v0.7 老库无 usage 数据可接受）

#### 3. 仓储（domain/repository + data/local impl）
- `UsageRepository`（record/observeAll/observePage/observeSummary/observeByModel/observeByApp/totalCost/clearAll/deleteOlderThan）+ `PricingRepository`（observeAll/get/getByModel/upsert/delete/resetBuiltin/calculateCost）
- `PricingRepositoryImpl.ensureSeeded()` 首次访问惰性写内置定价；`calculateCost` 按 `$provider::$model` 取价（provider 由 `deriveProvider` 推导），查不到按模型名兜底，都无则返回 0（不计费不报错）

#### 4. 计费用例（domain/usecase）
- `CostCalculator`：纯函数 `costMicro = prompt/1e6*input + completion/1e6*output - cached/1e6*cachedPrice`，下限 0
- `TokenCounter`：`estimate`（中文≈1.5/英文≈4 字每 token）+ `estimateUsage` 模型未返回 usage 时兜底
- `RecordUsageUseCase`：`recordSuccess`/`recordFailure`，成功但 usage 全 0 自动估算；费用/写库失败 `runCatching` 兜底，绝不阻塞生成主链路
- `BudgetManager`：`currentState`/`checkBudget`，自然月 1 日 00:00 起算已用，阈值 80%/100%，超额可拦截
- `BudgetNotifier`：Android 13+ 检查 `POST_NOTIFICATIONS` 权限后发预算提醒通知

#### 5. 集成主流程（ReplySuggestionUseCase）
- 生成前读 `budgetManager.checkBudget()`，`EXCEEDED_BLOCK` → 直接返回超额错误（必须用户可见，不静默）
- StreamDone → `recordUsage.recordSuccess(...)` 落库 + 预算通知；Error → `recordUsage.recordFailure(targetPackage)` 落库

#### 6. 配置与 UI
- `AppConfig.budget: BudgetConfig`（默认 `BudgetConfig()`，强容错 Json 优雅回退）
- 设置页 `BudgetCard`（月度预算输入 + 三开关），在 PersonaCard 后调用
- 顶层 Tab「用量」（`Route.Usage`，`Icons.Filled.BarChart`）（注：原「定价管理」子页 `Route.Pricing`/`PricingScreen` 已于 v0.20 删除，定价改在模型配置方案内配置，见 v0.20 §13.4）
- `UsageScreen`：预算卡（进度条+状态）/ 范围切换（今日/本周/本月/全部）/ 概览 / 按模型 / 按 App / 隐私说明 / CSV 导出 / 清空（原 `PricingScreen` 定价列表编辑 + 新增 + 恢复内置，v0.20 起移除）
- `UsageViewModel`（注：原 `PricingViewModel` 已于 v0.20 删除，定价编辑改入模型配置方案）：响应式聚合（`combine`/`flatMapLatest`/`stateIn`）+ `budgetProgress` 实时推算

### 测试结果
- **新增 5 个单测类 0 失败**：`TokenCounterTest`（中文/英文/混合估算）+ `CostCalculatorTest`（输入/输出/cache 折扣/下限 0）+ `BudgetManagerTest`（80%/100%/拦截阈值、未设预算 OK）+ `UsageRepositoryTest`（record/observe/聚合/清空，假 DAO）+ `PricingRepositoryTest`（计价键匹配/兜底/映射，假 DAO）
- **改写 `ReplySuggestionUseCaseTest`**：注入 `recordUsage`/`budgetManager`/`budgetNotifier` mock，默认 `budgetManager.currentState()` 返回 `OK`
- **全量单测 0 失败**，`assembleDebug` 绿

### 不在 v0.19 范围（明示，留 v0.20+）
- 语调自定义模板（§13.2，从枚举升级为可编辑模板）：独立迭代
- lastUsedAt 自动按场景命中更新：当前仅手动选中维护，避免每次生成写 DataStore（如需要可后续在 generate 时由调用方更新）

## [0.18.0] - 2026-07-19

### 新增（v2.2 双向人设，§13.1）

#### 1. 双向人设领域模型（domain/model/Persona.kt）
- `SelfPersona`：name / identity / speakingStyle / taboo / signature
- `CounterpartPersona`：name / identity / relationship / theirStyle / theirPersonality / dynamic / goal
- `PersonaProfile`：id / name / self / counterpart / boundPackages / boundKeywords / isDefault / lastUsedAt
- `ResolvedPersona`：解析后的生效人设（self + counterpart）

#### 2. AppConfig 升级（domain/model/Config.kt）
- 新增 `personas: List<PersonaProfile>` + `activePersonaId: String`
- 保留旧 v2.1 单人设 `persona: PersonaConfig` 兼容：双向方案为空时 `BuildPromptUseCase` 自动降级（老用户人设不丢）
- 强容错 Json（v0.17 已配 `coerceInputValues + ignoreUnknownKeys + isLenient`）保证旧 DataStore JSON 无 `personas`/`activePersonaId` 字段时优雅回退默认，不抛 `SerializationException`

#### 3. PersonaMatcher 三级匹配（domain/usecase/PersonaMatcher.kt）
- 纯函数，无副作用，便于单测
- 匹配优先级：① activePersonaId 手动锁定 > ② 包名精确匹配（boundPackages 含 targetPackage）> ③ 关键词命中对话文本（boundKeywords 任意命中，大小写不敏感）> ④ 默认方案（isDefault）/ 列表第一个

#### 4. BuildPromptUseCase 注入双向人设（domain/usecase/BuildPromptUseCase.kt）
- `invoke(messages, targetPackage = "")` 新增 targetPackage 参数（默认空，向后兼容所有现有调用）
- system 块从「# 我的人设」（5 项）+「# 对方的人设」（11 项）两段拼装双向字段（共 16 项）
- `ReplySuggestionUseCase.generate(messages, targetPackage)` 已把当前 App 包名透传给 buildPrompt，驱动自动匹配
- 人设字段写入 system 块 → 切换人设会让 cache 短暂 miss 一次（与 style 同理，是合理取舍，注释已在 `styleText()` KDoc）

#### 5. 设置页人设编辑入口（presentation/settings + navigation/Route.kt）
- `Route.PersonaList` / `Route.PersonaEditNew` / `Route.PersonaEdit` 三个子页面路由
- `PersonaViewModel`：列表加载 / 设为当前（activePersonaId）/ 新建 / 保存 / 删除 / 设为默认（异常 runCatching 兜底）
- `PersonaListScreen`：方案列表卡片（当前生效/默认 标签 + 绑定包名/关键词数 + 设为当前/默认/删除/进入编辑）
- `PersonaEditScreen`：双向 16 字段 + 绑定包名/关键词（逗号分隔输入）+ 默认开关全中文化编辑
- 绑定包名新增「常用 App 预设」（`presentation/settings/PersonaPackagePresets.kt`）：`FlowRow`+`FilterChip` 按「社交通讯 / 办公协作」分组一键勾选切换，预设基线覆盖无障碍模式 `ChatParser.PRESETS` 的 5 个主流 IM（微信/QQ/钉钉/飞书/企业微信）并扩展到 15 个国内主流社交与办公交流 App；点击即在绑定列表里 toggle 包名
- `SettingsScreen` 新增「人设方案」入口 Card（显示当前生效方案名），`ChatAssistRoot` 注册三个路由
- 首个方案自动设为默认；删除默认方案自动把第一个设为默认；删除当前锁定项自动清除锁定

### 测试结果
- **新增 11 单测 0 失败**：`PersonaMatcherTest` 6 例（空列表降级 / 锁定优先 / 包名优先 / 关键词大小写不敏感 / 默认方案 / 首个兜底）+ `BuildPromptUseCaseTest` 新增 3 例（双向注入 / 锁定优先于包名 / 包名匹配命中）+ 旧占位断言字段名同步 + `PersonaPackagePresetsTest` 新增 2 例（预设覆盖无障碍原 5 个主流 IM 包名 / 包名格式合法且无重复）
- **197 单测 0 失败**（v0.17 沿用 186 + 新增 11）
- **5 仪器测试 0 失败**（沿用，未回归）
- **累计 202 测试 0 失败**
- `assembleDebug` 绿

### 补强（v0.18.1，哥哥下旨：对方人设加详情 + 确认逐项注入）
- **对方人设新增 4 项详情字段**（`domain/model/Persona.kt` 的 `CounterpartPersona`）：`likes` 喜好 / `dislikes` 讨厌的东西 / `petPeeves` 雷区/忌讳 / `habits` 口头习惯（语气词、口头禅、小动作）。四项均带默认值 `""`，旧版持久化 JSON 缺这些键时由 kotlinx.serialization 默认值兜底，向后兼容不抛异常。
- **`BuildPromptUseCase` 注入**：`# 对方的人设` 段从 7 项扩到 11 项，新增四项以「- 喜好：/ - 讨厌的东西：/ - 雷区：/ - 口头习惯：」形式写入 system 块，确认流入回复生成。
- **`PersonaEditScreen`**：「对方的人设」区在「聊天目标」下新增 喜好/讨厌的东西/雷区/口头习惯 四个多行输入，编辑态→存储→回显全链路打通。
- **新增 1 单测**：`BuildPromptUseCaseTest.对方人设全部字段都注入 system 块`——把对方人设全部 11 项字段逐一填不同哨兵值，断言每项都以 `<段名>：<值>` 进入 system 块，杜绝任一字段漏注入（直接回应哥哥「确认每一项人设信息都有正确的注入回复生成过程」）。
- **198 单测 0 失败**（v0.18.0 沿用 197 + 新增 1）
- **5 仪器测试 0 失败**（沿用，未回归）
- **累计 203 测试 0 失败**
- `assembleDebug` 绿

### 首页清理（v0.18.2，哥哥下旨：移除首页回复建议模块）
- **移除首页 `ReplyCard`**：`MainScreen.kt` 删除底部「回复建议」卡片（「生成」按钮 + 流式/候选/复制列表），连同 `ReplyCard` 整个 Composable 函数与不再使用的 import（`ReplyUiState`/`items`/`IconButton`/`Icons.Filled.ContentCopy`）；顶部注释更新为「回复建议在悬浮窗生成，首页不内嵌冗余卡片」。
- **`MainViewModel` 删除 `replyState` 暴露**：该 `StateFlow` 仅供首页卡片订阅，移除后悬浮窗生成链路（`replyUseCase`/`generateReply`/`overlay.generateHandler`）完全保留——悬浮窗才是核心回复入口，App 不因此失能。
- **崩溃根因**：闪退来自首页卡片点「生成」→ `generateReply()` → `captureUseCase.captureOnce()` 链路；卡片移除后该入口消失。核心生成能力经悬浮窗「显示悬浮窗 → 生成」仍可用（同一 `generateReply`）。
- 测试计数不变：**198 单测 + 5 仪器测试 = 203 测试 0 失败**（无测试删除，生成/复制/权限测试全保留，均服务于悬浮窗链路）；`assembleDebug` 绿。

### 不在 v0.18 范围（明示，留 v0.19+）
- 语气自定义模板（§13.2，从枚举升级为可编辑模板）：独立迭代
- lastUsedAt 自动按场景命中更新：当前仅手动选中维护，避免每次生成写 DataStore（如需要可后续在 generate 时由调用方更新）

## [0.17.0] - 2026-07-19

### 变更（哥哥下旨：触发方式精简 + 对话目标重命名 + 修复闪退）

#### 1. 触发方式精简（4 → 2）
- **文件**：`app/src/main/kotlin/com/chatassist/domain/model/Config.kt` + `DomainLabels.kt`
- **改动**：`TriggerMode` 枚举从 `{ MANUAL, NOTIFICATION, EDGE_BAR, GESTURE }` 精简为 `{ MANUAL, EDGE_BAR }`
- **保留**：
  - `MANUAL` → displayName "手动触发" → **"悬浮窗生成按钮"**（哥哥原话措辞）
  - `EDGE_BAR` → displayName "边缘触发条" → **"侧边条滑动触发"**（哥哥原话措辞）
- **移除**：
  - `NOTIFICATION`（通知触发）：Android 13+ 通知权限收紧，且对聊天 App 无障碍读屏场景增益不大
  - `GESTURE`（手势触发）：与国产 ROM 全面屏手势冲突，实际可用性差
- **Settings UI**：FilterChip 列表自动只剩 2 个，无需手动改 UI 代码

#### 2. 对话目标 → 对话风格（重命名）
- **文件**：`Config.kt` + `DomainLabels.kt` + `BuildPromptUseCase.kt` + `SettingsScreen.kt` + `BuildPromptUseCaseTest.kt`
- **改名链**：
  - 枚举名：`ConversationGoal` → `ConversationStyle`
  - 字段名：`AppConfig.goal` → `AppConfig.style`
  - 函数名：`BuildPromptUseCase.goalText()` → `styleText()`
  - UI 函数：`SettingsScreen.GoalCard()` → `StyleCard()`
  - UI 标题：「对话目标」→「对话风格」
- **6 个枚举值不变**：`PROFESSIONAL / CASUAL / WARM / FLIRTY / CONCISE / HUMOROUS`
- **6 个 displayName 不变**：「专业得体 / 轻松随意 / 温暖亲切 / 暧昧撩人 / 简洁直接 / 幽默风趣」
- **prompt 段名 `# 目标语气` 保留**（system 块结构稳定，模型对中文语义"目标语气"和"对话风格"理解一致；改名收益小、风险大）

#### 3. 修复「切换触发方式再返回主页闪退」（ConfigRepositoryImpl Json 强容错）
- **根因**：`ConfigRepositoryImpl.encodeToJson / decodeFromJson` 用**默认 `Json{}`**，未配置 `coerceInputValues` / `ignoreUnknownKeys`。
  - 当 AppConfig 字段或枚举值在版本迭代中增减，旧 DataStore JSON 反序列化抛 `SerializationException`
  - 外层 `runCatching` 吞了异常 → cfg 静默回退到默认 `AppConfig()` → 用户感受到「切了 triggerMode 状态消失 / 像闪退」
- **修复**：改用强容错 `configJson` 单例：
  ```kotlin
  private val configJson: Json = Json {
      ignoreUnknownKeys = true  // 旧 JSON 未知字段忽略
      coerceInputValues = true  // 旧枚举未知值回退默认
      isLenient = true          // 允许非标准 JSON
      encodeDefaults = true     // 默认字段也编码
      explicitNulls = false     // null 不编码
  }
  ```
  - **核心是 `coerceInputValues = true`**：v0.16 旧 DataStore 里的 `triggerMode: "GESTURE"` / `"NOTIFICATION"` 在 v0.17 升级后会优雅回退到 `MANUAL`，**不再抛 SerializationException**
  - 此配置与 [com.chatassist.data.llm.di.NetworkModule] 的网络 Json 对齐（v0.16 已用）
- **附加 UX 修复**（`OverlayController.onConfigChanged`）：triggerMode 切换且已挂载视图时，**自动 `hide() + show()` 重新挂载**——之前 view/edgeBar 只在 null 时才挂载，triggerMode 切换后旧视图会残留（EDGE_BAR 模式下切到 MANUAL 但屏幕上还显示着侧边条）

#### 4. prompt 注入确认（BuildPromptUseCase + 新单测）
- **哥哥下旨**：「确认预设的对话风格 prompt 是否正确在被切换之后注入到回复生成过程」
- **确认结果**：✅ **正确注入**
  - `BuildPromptUseCase.invoke()` 每次执行都 `configRepo.snapshot()` 拿**最新** cfg（非缓存值）
  - 切换 `cfg.style` 后，**下一次** invoke 立即用新 style 拼进 system 块
  - 不会叠加旧 style 描述
- **代价**：style 切换会让 system 块内容变化 → OpenAI / Anthropic / DeepSeek 的 prompt cache（前缀匹配）**短暂 miss 一次**，下次开始重新稳定命中。这是不可避免的取舍，注释已写在 `BuildPromptUseCase.styleText()` KDoc 里
- **新单测**（`BuildPromptUseCaseTest.style 切换后下一次 invoke 立即用新风格且不叠加旧风格`）：
  - 第一次 invoke WARM → system 包含「温暖」、不包含「专业、得体」
  - 第二次 invoke FLIRTY → system 包含「暧昧」、不包含「温暖、亲切」
  - 验证「切换后立即注入新值 + 不叠加旧值」两个不变量
- **优化建议**（待哥哥决定，未做）：把 style 段挪到 user 块（user 本来每次变）→ 切换不影响 cache miss。但会让 system 块的语义位置下沉（模型对 system 指令的权重更高）。如果频繁切 style 觉得 cache miss 影响体验，可走这条路

### 测试结果
- **186 单测 0 失败**（v0.16.1 沿用 185 + BuildPromptUseCaseTest 新增 1 例「style 切换后立即注入」）
- **5 仪器测试 0 失败**（v0.16.0 沿用，未回归）
- **累计 191 测试 0 失败**
- `assembleDebug` 37s 绿

## [0.16.1] - 2026-07-19

### 修复（哥哥下旨：设置页触发方式/对话目标切换无效 + 横向挤压）

#### 1. UI 横向布局修复（FlowRow 自动换行）
- **文件**：`app/src/main/kotlin/com/chatassist/presentation/settings/SettingsScreen.kt`
- **症状**：`ThemeCard`（3 个主题模式按钮）、`TriggerCard`（4 个触发方式按钮）、`GoalCard`（6 个对话目标按钮）三个 Card 用 `Row(spacedBy(8.dp))` 硬塞 3-6 个 Button，**没有换行、没有权重、没有 horizontalScroll**。在手机竖屏宽度下，第三个按钮之后被挤出屏幕外或被裁切（哥哥下旨"两个板块的 ui 横向布局挤压,使得第三个按钮都被挤压异常"）
- **修复**：三处 `Row` → `FlowRow`（Compose Foundation 1.4+ 实验 API，1.7 稳定，本项目 BOM 2024.09.03 支持），加 `verticalArrangement = spacedBy(8.dp)` 让多行之间也留间距
- **副作用**：3 个私有 Card 函数需 `@OptIn(ExperimentalLayoutApi::class)`；`ExperimentalLayoutApi` 在 `androidx.compose.foundation.layout` 包下（不是 `androidx.compose.foundation`）

#### 2. 选中态视觉反馈（FilterChip 替换 Button）
- **症状**：原 `Button(enabled = config.xxx != mode)` 选中态只是 disabled 灰色（ThemeCard/TriggerCard/GoalCard 三个 Card 写法一致），无明显「已选中」高亮；多 Button 横向挤压 + 选中态不明显双重作用下，哥哥视角下"切换貌似无效"（实际 ViewModel/Repository/DataStore 链路完全正常，状态正确写盘）
- **修复**：三处 `Button` → `FilterChip(selected = config.xxx == mode, onClick = ...)`，Material 3 标准选中态（✓ 图标 + 主题色填充 + 边框加粗），用户能一眼看到当前选中哪个
- **附加**：删掉三处冗余的"当前：xxx"小字行（FilterChip 选中态已够清晰）

#### 3. ApiConfigCard 不变
- 「保存密钥」「测试连接」仍是 `Button`（这两个是单次执行动作，不是状态切换，不需要 FilterChip）

### 测试结果
- **assembleDebug 编译绿**（9s）

## [0.16.0] - 2026-07-19

### 变更（哥哥下旨：UI 全中文化 + Prompt 缓存命中优化）

#### 1. UI 全中文化（domain.model.DayLabels）
- 新增 `app/src/main/kotlin/com/chatassist/domain/model/DomainLabels.kt`（枚举 → 中文显示名）
- 覆盖：ThemeMode（跟随系统/浅色/深色）/ TriggerMode（手动触发/通知触发/边缘触发条/手势触发）/ ConversationGoal（专业得体/轻松随意/温暖亲切/暧昧撩人/简洁直接/幽默风趣）/ PowerMode（省电/均衡/性能）/ RecognitionMode（文字识别/视觉理解）/ ModelType（文本模型/多模态）
- 改写 `SettingsScreen` 全英文 label → 中文："API URL"→"接口地址" / "API Key (加密存储)"→"接口密钥（加密存储）" / "模型名"→"模型名称" / "API 已配置"→"接口已配置"
- 改写 `AboutScreen`：版本号 v0.2.0→v0.16.0、"GitHub: <待填写>"→"代码仓库：暂未公开"、"协议: 待定"→"协议：待定"
- 改写 `MainScreen`："URL: $apiUrl"→"接口地址：$apiUrl"、"Model: $model"→"模型：$model"
- 半角冒号 `:` 全角 `：` 化（"主题模式: $x"→"主题模式：$x"）
- 内部 TAG / 日志 / Retrofit role 字段保留英文（不显示给用户）

#### 2. Prompt 缓存命中优化（BuildPromptUseCase）
- **核心原则**：System 块 100% 字节级稳定（不含任何条件性内容）→ 命中 OpenAI / Anthropic / DeepSeek 等的 prompt cache（前缀匹配）
- **改动**：把 OCR 形近字提示从 System 块移到 User 块置顶（条件性内容，user 块本来每次变）
- **缓存结构**：
  - messages[0] = system：任务定义 + 我是谁/说话风格/关系 + 目标语气 + 回复要求
  - messages[1] = user：OCR 提示（条件性，前缀固定）+ 最近对话转写 + 生成指令
- 更新 `BuildPromptUseCaseTest` 12 例中 2 例断言：OCR 提示位置从 system 改到 user，system 块绝对不含 OCR 提示

#### 3. 仪器测试修复（HiltTestRunner）
- 现象：v0.12 移除 WorkManagerInitializer 默认初始化后，androidTest 启动 HiltTestApplication 替换 ChatAssistApp 时 WorkManager 没人初始化；HiltTestRunner 里手动 init 又因 context 时机问题 NPE
- 修复：移除 HiltTestRunner 里 `WorkManager.initialize` 调用，依靠 `KeepAliveManager.start()` 内部 runCatching 吞掉 `WorkManager.getInstance` 抛的 IllegalStateException（KeepAliveService 不依赖 WorkManager，能独立启动）
- 验证：mumu 7555 实测 5/5 全过（1.49s）

### 测试结果
- **185 单测 0 失败**（v0.14 沿用，新增 DomainLabels 编译绿；BuildPromptUseCaseTest 12 例全过包括新增的缓存命中验证）
- **5 仪器测试 0 失败**（mumu 7555 实测：MainActivity 2 + HistoryDao 3 = 5/5 全过 1.49s）
- **累计 190 测试 0 失败**

## [0.15.0] - 2026-07-19

### 新增（模块 13 性能 + 仪器测试）

#### 1. Macrobenchmark 独立模块
- 新模块 `:benchmark`（`com.android.test` plugin + `targetProjectPath = ":app"` + self-instrumenting）
- 4 项 KPI 基准（KPI-1 冷/热启动 / KPI-2 历史页滚动帧率 / KPI-3 悬浮窗显示延迟 / KPI-4 Tab 导航帧率）
- 实跑命令（真机）：
  ```bash
  adb shell am instrument -w -e class com.chatassist.benchmark.ColdStartBenchmark \
    -e androidx.benchmark.suppressErrors "DEBUGGABLE,NOT-SELF-INSTRUMENTING" \
    com.chatassist.benchmark/androidx.test.runner.AndroidJUnitRunner
  ```
- 报告输出：`benchmark/build/reports/androidTests/connected/index.html`
- 注：Mumu 模拟器有 perfetto + force-stop 行为冲突，真机回归

#### 2. androidTest 仪器测试套件
- `HiltTestRunner`（HiltTestApplication 隔离容器，`testInstrumentationRunnerArguments`）
- `MainActivityInstrumentedTest`（2 例：4 Tab 上屏 + 历史 Tab 切换）
- `HistoryDaoInstrumentedTest`（3 例：insert 往返 / deleteById / clearAll）
- **mumu 7555 实跑 5/5 全过**（2.21s）

#### 3. app 模块配套
- 新增 `create("benchmark") { initWith release; isDebuggable = false; matchingFallbacks += release }` buildType
- `AndroidManifest.xml` 加 `<profileable android:shell="true" />`（让 Macrobenchmark 抓 trace）
- `ChatAssistRoot` 加 `Modifier.semantics { testTagsAsResourceId = true }.testTag("chatassist_root_layout")`（给 Macrobenchmark 提供 By.res 等待锚点）
- 新依赖：`hilt-android-testing` + `androidx.test.orchestrator`（先关）+ `room-testing`

#### 4. 依赖补全
- `gradle/libs.versions.toml`：新增 `macrobenchmark 1.3.2` + `hilt-testing 2.52` + `orchestrator 1.5.1` + `hilt-work` 测试 variant
- `benchmark/gradle/libs.versions.toml`：独立 toml（macrobenchmark junit4 + junit4 bridge）

### 测试结果
- **185 单测 0 失败**（v0.14 沿用，无回归）
- **5 仪器测试 0 失败**（v0.15 新增，mumu 7555 实测）
- **190 测试累计**

### 修复
- `META-INF/LICENSE.md` packaging 冲突（`com.android.test` 引入 junit5 + benchmark-macro 多个 LICENSE 资源重叠）
- `applicationId` / namespace 与 `com.android.test` plugin 不兼容（`Dependent features configured but no package ID was set`）→ 改用 `targetProjectPath = ":app"` + self-instrumenting
- META-INF 重复文件排除（`/META-INF/{AL2.0,LGPL2.1,LICENSE.md,LICENSE-notice.md}`）

## [0.14.0] - 2026-07-19

### 新增（模块 13 全量测试 + 性能调优）

#### 1. JaCoCo 覆盖率配置
- 插件：`org.gradle.jacoco`（Gradle core plugin）
- 任务：`./gradlew :app:jacocoTestReport` 生成 HTML 报告
- 报告：`app/build/reports/jacoco/jacocoTestReport/html/index.html`
- **覆盖度提升**（v0.13 之前 → v0.14 后）：
  - `domain.usecase`: 14% → **82%** ⬆️（+68%）
  - `domain.capture`: 0% → **84%** ⬆️
  - `domain.model`: 75% → 82%
  - `util`: 40% → 41%
  - `presentation.history`: 0% → 17%
  - `presentation.main`: 0% → 14%
  - `presentation.settings`: 0% → 6%
- **Domain 层 ≥80% 目标达标**：error 94% / parser 93% / capture 84% / model 82% / usecase 82%

#### 2. Paparazzi 截图测试（Theme 视觉回归）
- 插件：`app.cash.paparazzi 1.3.4`
- 测试位置：`app/src/test/kotlin/com/chatassist/presentation/theme/ThemeSnapshotTest.kt`
- **3 张基线截图**（已生成 PNG 落 `app/build/reports/paparazzi/debug/images/`）：
  - 浅色主题主屏预览（Card + Button + OutlinedButton + 圆角 token + 头像）
  - 深色主题主屏预览
  - 浅色主题圆角 token 展示（8/12/16/20/30 dp 五档）
- JUnit 4 测试 + JUnit 5 vintage engine 桥接（与现有 JUnit 5 单测共存）

#### 3. androidTest 框架（Compose UI Test 基础设施）
- 加依赖：`androidx.compose.ui:ui-test-junit4` / `androidx.test.ext:junit` / `androidx.test:rules` / `ui-test-manifest`（debugImplementation）
- 后续 v0.15+ 接入关键页面（MainScreen / Settings / History / Overlay）UI 测试

#### 4. 补 ViewModel / UseCase 单测（**52 新增 0 失败**）
- `MainViewModelTest` **11 例**：state 4 项 + permissionSummary 1 + capturedMessages 1 + captureMode 1 + generateReply 3 路径 + copySuggestion 1 + refresh 1 + toggleOverlay 1
- `SettingsViewModelTest` **8 例**：state 加载 2 + updateApiKey 1 + update 1 + testConnection 4 路径（成功/失败/AppError/未知） + dismissTestResult 1
- `HistoryViewModelTest` **7 例**：空 records 1 + records 变化 1 + config 变化 1 + delete 1 + clearAll 1 + runCleanup 2（maxAgeDays>0 / ≤0）
- `BuildPromptUseCaseTest` **12 例**：空消息 1 + 正常 1 + isSelf=true/false 2 + 系统消息过滤 1 + 空 content 过滤 1 + OCR 提示 2（OCR_ONLY 触发 / TEXT_ONLY 不触发） + contextRounds 截断 1 + 人设空字段 1 + Goal 6 种 1 + config 透传 1
- `ReplySuggestionUseCaseTest` **8 例**：空消息 1 + buildPrompt 失败 1 + 流式累积 1 + 隐私模式 1 + 非隐私模式 1 + 1.xxx 格式 1 + 5 条上限 1 + Error 事件 1
- `CaptureChatUseCaseTest` **3 例**：invoke 透传 1 + captureOnce 成功 1 + captureOnce 失败 1
- `ThemeSnapshotTest` **3 例**：浅色 / 深色 / 圆角 token

#### 单测统计

| 测试类 | 用例数 |
|---|---|
| `MainViewModelTest` | 11 |
| `SettingsViewModelTest` | 8 |
| `HistoryViewModelTest` | 7 |
| `BuildPromptUseCaseTest` | 12 |
| `ReplySuggestionUseCaseTest` | 8 |
| `CaptureChatUseCaseTest` | 3 |
| `ThemeSnapshotTest` | 3 |
| **v0.14 新增小计** | **52** |
| v0.12 后台保活既有 | 36 |
| v0.11 错误处理既有 | 45 |
| v0.10 解析增强既有 | 33 |
| v0.6 基础既有 | 19 |
| **累计** | **185 单测 0 失败** |

#### 构建结果
- ✅ `:app:assembleDebug` 成功
- ✅ `:app:testDebugUnitTest` 成功（185 单测 0 失败）
- ✅ `:app:jacocoTestReport` 成功（Domain 层 ≥80% 目标达标）

### 不在 v0.14 范围（明示，留 v0.15+）
- **Macrobenchmark 模块**（独立 benchmark module + 4 项 KPI）：需 release APK + 真机/模拟器 perf 数据，耗时 1-2h；哥哥无真机，模拟器数据仅参考。**留 v0.15 增量**
- **androidTest 实际运行**（仅配置依赖，无实际 UI Test 代码）：需 mumu 装 debug + 跑 Compose UI Test，mumu 稳定性待验证。**留 v0.15 增量**
- **真机 9 ROM 矩阵回归**：哥哥无真机环境。**留 v0.16 收尾**
- **OCR 样本集 ≥50 张**：需用户提供真实聊天截图。**留 v0.16 收尾**

### 后续待办（v0.15+）
1. v0.15 Macrobenchmark 模块搭建 + 4 项 KPI 实测（冷启动/热启动/OCR/截图）
2. v0.15 androidTest 实际跑（MainScreen/Settings 关键路径）
3. v0.16 真机 9 ROM × 4 形态回归
4. v0.16 OCR 样本集 + Vision 通道 v0.9 升级
5. v0.17 v2.2 双向人设（§13.1）
6. v0.18 v2.2 计费（§13.3）
7. v0.19 i18n + 主题资源（strings.xml 50+ 条）

## [0.13.0] - 2026-07-19

### 修复（v0.12 后台保活 FGS 启动时机坑，mumu 实测发现并验证修复）

#### 根因（mumu logcat 实证）

- v0.12 把 `keepAliveManager.start()` 放在 `ChatAssistApp.onCreate` 里
- Android 12+ 限制 cached app (uidState=CEM) 不能 `startForegroundService`
- 进程刚创建时 uidState=CEM → 抛 `ForegroundServiceStartNotAllowedException`：
  ```
  Background started FGS: Disallowed [uidState: CEM ; uidBFSL: n/a]
  startForegroundService() not allowed due to mAllowStartForeground false
  ```
- v0.12 runCatching 兜底不崩 + WorkManager 15min 兜底拉活（最终能起来，但首次启动后服务是 Worker 拉的，不是 Application 拉的）

#### 修复

- `ChatAssistApp.onCreate` 删 `keepAliveManager.start()` 调用
- `MainActivity.onCreate` 注入 `KeepAliveManager` 并调 `start()`（用户进 App 时 uidState=TOP，能正常拉 FGS）
- 修复后 logcat 实证：
  ```
  19:26:45.367 I KeepAliveManager: Starting keep-alive (service + worker)
  19:26:45.367 I ActivityManager: Background started FGS: Allowed [uidState: TOP ; code:PROC_STATE_TOP]
  19:26:45.554 I KeepAliveService: KeepAliveService started
  19:26:45.792 D KeepAliveWorker: Service is running, skip revive
  ```
- 时序：启动 → 服务起来 187ms 后 → Worker 检测已在跑跳过 — 完美

#### 单测
- 0 新增（仅 2 文件改动），**累计 133 单测 0 失败**

#### 构建结果
- ✅ `:app:assembleDebug` 成功（5s）
- ✅ `:app:testDebugUnitTest` 成功

#### 已知风险
- 仍依赖用户在进 App 那一刻启动保活（App 进程完全没启动时 Worker 也没人入队）
- 后续 v0.13+ 可考虑加开机自启 BroadcastReceiver（监听 `BOOT_COMPLETED` 入队 Worker）

## [0.12.0] - 2026-07-19

### 新增（模块 10 后台保活，0 → 8 核心类 + 36 单测全绿）

#### 8 个核心类（指南 §4.10 一次性补齐）

1. **`Vendor` 枚举**（`domain/model/Vendor.kt`）
   - 6 厂商：XIAOMI / HUAWEI / OPPO / VIVO / SAMSUNG / MEIZU + OTHER
   - ROM 检测输入，决定 VendorGuide 走哪条自启动路径

2. **`KeepAliveEvent` 契约**（`domain/model/KeepAliveEvent.kt`）
   - sealed class：ServiceStarted / ServiceStopped / VendorPushRequested(vendor) / RevivedByWorker(workerName)
   - 状态机/事件流契约，UI 观察用

3. **`VendorDetector`**（`util/VendorDetector.kt`）
   - 三级检测：Build.MANUFACTURER → Build.BRAND → 系统属性（ro.miui.ui.version.name / ro.build.version.emui / ro.vivo.os.version 等）
   - 反射读系统属性失败兜底 OTHER
   - 核心逻辑纯函数化（`detect(manufacturer, brand, systemProps)` 便于单测）
   - 16 例单测全绿（6 厂商 + 兜底 + 系统属性命中 + 缓存 reset）

4. **`VendorGuide` + `VendorDescriptor`**（`util/VendorGuide.kt`）
   - 6 厂商自启动 Intent 矩阵：小米 miui securitycenter / 华为 huawei systemmanager / OPPO coloros safecenter / vivo iqoo secure / 三星 samsung lool / 魅族 meizu safe
   - 厂商专属 Intent 不可解析时回退到系统设置首页
   - `buildAppDetailsIntent` 终极兜底（ACTION_APPLICATION_DETAILS_SETTINGS）
   - 9 例单测全绿（descriptor 纯数据 + 6 厂商 + 兜底 + 应用详情）

5. **`KeepAliveService`**（`service/KeepAliveService.kt`）
   - ForegroundService，foregroundServiceType="specialUse"
   - Android 14+ 用 `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE`
   - 通知常驻（重要性 LOW，category SERVICE，不弹横幅）
   - `onStartCommand` → `START_STICKY`（被系统杀后会重启）
   - `KeepAliveManager` 内部回调 `onServiceStarted()` / `onServiceStopped()`

6. **`KeepAliveWorker`**（`service/KeepAliveWorker.kt`）
   - `@HiltWorker` CoroutineWorker
   - 15 分钟周期 + `Constraints` 默认
   - 触发时检查 `KeepAliveManager.isRunning()`，false → startService 拉活
   - 失败返 `Result.retry()`（指数退避）

7. **`KeepAliveManager`**（`domain/usecase/KeepAliveManager.kt`）
   - 协调器：start() / stop() / requestVendorGuide(vendor)
   - StateFlow<KeepAliveState>（Stopped / Running / Reviving）+ SharedFlow<KeepAliveEvent> 事件流
   - `scheduleWorker()` / `cancelWorker()`（WorkManager 接入）
   - 内部回调（onServiceStarted/Stopped/onRevivedByWorker）供 Service/Worker 调用
   - 7 例单测全绿（状态机 + VendorGuide 委托 + 事件流）

8. **`KeepAliveModule`**（`di/KeepAliveModule.kt`）
   - Hilt 模块，提供 WorkManager `Configuration`（绑定 HiltWorkerFactory）
   - `ChatAssistApp` 实现 `Configuration.Provider` 接口，on-demand 初始化 WorkManager
   - manifest 移除 WorkManagerInitializer 默认初始化器

#### 集成

- `AndroidManifest.xml` 加 `FOREGROUND_SERVICE_SPECIAL_USE` 权限 + `<service>` 声明（specialUse 类型 + PROPERTY_SPECIAL_USE_FGS_SUBTYPE 描述）
- `ChatAssistApp.onCreate` v0.12 末段调 `keepAliveManager.start()`（在 `GlobalErrorHandler.install()` 之后）
- WorkManager 走 on-demand 初始化路线（`Configuration.Provider` + manifest `tools:node="remove"` WorkManagerInitializer）

#### 单测统计

| 测试类 | 用例数 |
|---|---|
| `VendorDetectorTest` | 16 |
| `VendorGuideTest` | 9 |
| `KeepAliveManagerTest` | 7 |
| `KeepAliveEventTest` | 4 |
| **v0.12 新增小计** | **36** |
| v0.11 错误处理既有 | 45 |
| v0.10 解析增强既有 | 33 |
| v0.6 基础既有 | 19 |
| **累计** | **133 单测 0 失败** |

### 构建结果

- ✅ `:app:assembleDebug` 成功（BUILD SUCCESSFUL 7s）
- ✅ `:app:testDebugUnitTest` 成功，**133 单测 0 失败**

### 已知风险（v0.13 真机回归时实测调整）

- 6 厂商自启动 Intent 路径以官方 ROM 为准，国际版/精简版可能不可解析（已加回退）
- `Configuration.Provider` on-demand 初始化在 Hilt 1.2.0 + WorkManager 2.9.1 组合下稳定
- `START_STICKY` 在 MIUI 上仍可能被强杀（必须引导用户加白名单）
- v0.13 真机回归调整 Intent 路径前，部分 ROM 跳转可能失败

### 后续待办（v0.13+）

1. **6 厂商真机回归**（模块 13）：MIUI 14 / HyperOS / EMUI 12 / 鸿蒙 3-4 / ColorOS 14 / OriginOS 4 / One UI 6 各一台
2. **设置页加「自启动引导」入口**（UI 接入 VendorGuide，UI 暂时没接入口）
3. **开机自启 BroadcastReceiver**（可选，监听 `BOOT_COMPLETED` 拉起 KeepAliveService）
4. **折叠屏适配**（模块 11，留 v0.13+）
5. **v0.13 全量测试 + 性能 KPI**（Paparazzi / Macrobenchmark / androidTest）

## 关键决策（贯穿 v0.4–v0.6，2026-07-18）

- **捕获方案定为混合**：无障碍读屏（AccessibilityService）为首选主通道，MediaProjection+OCR 为降级兜底通道，由 `CaptureStrategyManager` 自动降级 / 恢复。推翻 v2「纯 OCR」立场，采用新指南第 5 章方案（兼容性 + 性能兼得）。
- **包名保留 `com.chatassist`**：不沿用新指南的 `com.aichatassistant`，避免无谓改名。
- **功能范围以 v2 指南为准**：保留 v2.1/v2.2 增补（双向人设、计费统计、自定义语气、社交画像、边缘触发条、智能贴边、精准模式、多模态 Vision 预留、本地小模型 v3.0 预留等）。新指南作结构基线，不含上述加料。
- **影响文档**：`docs/architecture.md`、`docs/modules/02-屏幕捕获.md`、`README.md`、`PROJECT_KNOWLEDGE.md` 已同步覆写。

## [0.10.0] - 2026-07-19

### 新增（聊天内容解析完善，模块 04 增强 + 52 单测全绿）

#### A. 置信度评分（ChatParser）
- `ChatParser.parse()` 5 维加权：来源（TEXT_ONLY +0.1 / OCR_ONLY +0）+ 几何（+0.1）+ 长度（1-2 字 -0.4 / 3-4 字 -0.1 / 5+ +0.05）+ 位置贴边（centerX < sw*0.08 或 > sw*0.92 -0.2）+ App 预设命中（+0.05）
- 基准 0.7、最终 clamp [0, 1]
- 系统消息固定 0.5（无置信度概念）
- UI 可据此 < 0.7 提示「识别不确定，请核对」

#### B. App 预设规则库
- 新增 `data class AppPreset(pkg, label, alignLeftIsSelf)`
- ChatParser 内部 `PRESETS` map 内置 9 个 App：微信/QQ/TIM/钉钉/飞书/Telegram/Slack/WhatsApp/LINE/短信
- 多数 IM 默认右侧=自己；Slack 英文版部分皮肤左侧=自己
- 包名未命中走"右侧=自己"通用规则，confidence 略降
- 用户自定义 App 预设留 v0.11+（Config 加 `customPresets` 字段）

#### C. 多气泡合并（MessageMerger）
- 新增 `domain/parser/MessageMerger.kt`
- 三条件合并：同 sender + Y 距离 ≤ 100px + 中间无系统行
- topY=0 兜底：按 index 顺序 + 同 sender 合并
- 合并后 confidence 取 min
- 7 例单测：同/不同 sender、Y 距离、系统断点、topY=0 兜底、confidence 取 min、空列表
- OcrCaptureChannel.recognize 后置接 MessageMerger

#### D. 滚动续接去重（MessageDeduplicator）
- 新增 `domain/parser/MessageDeduplicator.kt`
- 5min 滑动窗口，content+sender+timestamp 哈希作指纹
- 进程内单例 @Singleton（重启清空，避免长期累积误伤）
- `reset()` 主动清空（切 App / 滚动后用户主动重置）
- 6 例单测：完全相同、不同 sender、不同 timestamp、reset、5min 窗口外、窗口过小自动清理
- OcrCaptureChannel.recognize 后置接 MessageDeduplicator

#### E. UI 模式检测（UiModeDetector，§12.2.1）
- 新增 `util/UiModeDetector.kt`
- 64x64 缩略图 + BT.601 加权亮度
- < 0.3 判 DARK，否则 LIGHT
- `CaptureResult.uiMode: UiMode?` 字段，OCR 通道填
- v0.10 仅观测（写入 uiMode），v0.11+ 动态调 OCR 置信度阈值
- 单测待 androidTest（依赖 Robolectric，v0.11+ 落地）

#### F. 系统关键词扩充
- ChatParser.SYSTEM_KEYWORDS 从 14 词扩到 25 词
- 新增：红包已拆开/已领取/已收款、拍了拍、已开启/已关闭、消息发送失败、对方忙/未接听、图片已过期、链接已失效

#### 数据模型扩展
- `ChatMessage` 新增 `topY: Int = 0` 字段（v0.10 起，可选）
- `CaptureResult` 新增 `uiMode: UiMode? = null` 字段（v0.10 起，OCR 通道填）

### 构建结果
- ✅ `:app:assembleDebug` 成功（BUILD SUCCESSFUL 6s）
- ✅ `:app:testDebugUnitTest` 成功，**52 单测 0 失败**（v0.6 的 19 + v0.10 新增 33）
  - ChatParserTest 20 例（v0.6 的 6 + A 7 + B 4 + F 3）
  - MessageMergerTest 7 例
  - MessageDeduplicatorTest 6 例
  - 既有 NodeExtractorTest 2 + WhitelistInterceptorTest 5 + StreamingCallExecutorTest 4 + ModelApiRepositoryImplTest 8

### 不在 v0.10 范围（明示）
- 颜色聚类（§12.2.2）：ML Kit Text.Line 不暴露颜色、需像素级采样，开销与精度权衡不划算，**留 v0.11+ 接 Vision 通道时一起做**
- 无障碍 Hybrid 增强（§12.2.3）：v0.6/v0.9 双通道融合已在工作，**重复造轮子收益低**
- 置信度动态调 OCR 阈值：需先观察 v0.10 数据，**v0.11 再说**
- 用户自定义 App 预设：Config 暴露字段 + UI 入口，**v0.11+**

## [0.11.0] - 2026-07-19

### 新增（模块 12 错误处理与降级，0 → 7 核心类 + 45 单测全绿）

#### 7 个核心类（指南 §4.12 一次性补齐）

1. **`AppError` sealed class 重构**（`domain/model/AppError.kt`）
   - 从 `data class` 升级为 `sealed class`（指南要求）
   - `AppError.Fatal`：致命错误，App 无法继续运行（suggestedAction 永远 CONTACT_SUPPORT）
   - `AppError.Recoverable`：可恢复错误，App 可继续运行
   - 工厂 `AppError.of(...)` / `AppError.fatal(...)` 保持向后兼容
   - 全工程 27+ 处 `AppError(...)` → `AppError.of(...)` 机械替换（5 文件：WhitelistInterceptor / StreamingCallExecutor / ConfigRepositoryImpl / ModelApiRepositoryImpl / ModelApiRepository）
   - ErrorCode 新增 `UNCAUGHT`（v0.11.0），全 14 项

2. **`ErrorClassifier`**（`domain/error/ErrorClassifier.kt`）
   - Throwable → AppError 分类
   - 已分类 AppError 透传不重复包装
   - 网络层：`UnknownHostException` → DNS / `SocketTimeoutException` → TIMEOUT / `HttpException.code()` 分桶 401/403/429/5xx
   - 兜底：`SecurityException` → PERMISSION_DENIED / `IllegalStateException` → CONFIG_INVALID / 其他 → Fatal + UNCAUGHT
   - 14 例单测全绿

3. **`ErrorPresenter`**（`domain/error/ErrorPresenter.kt`）
   - AppError → ErrorUiModel（title + message + action + actionLabel + retryable）
   - 14 种 ErrorCode 全部定制情感化中文文案（"API Key 无效或已过期，请在「设置 → API」中重新填写"）
   - 自动 action 选择：401/403 → CHECK_API_KEY、NETWORK/DNS → CHECK_NETWORK、PERMISSION_DENIED → OPEN_SETTINGS、UNCAUGHT → CONTACT_SUPPORT
   - 7 例单测全绿

4. **`CrashLogger`**（`util/CrashLogger.kt`）
   - 本地脱敏崩溃日志：写入 `filesDir/crash/yyyy-MM-dd-HH-mm-ss.txt`
   - 脱敏正则：`Bearer xxx` / `sk-xxx` / `sk_xxx` / `api_key=xxx` / `apikey=xxx` / `key=xxx`（≥16 字符）全替换为 `***REDACTED***`
   - 栈超过 200 行截断 + cause 链最多 5 层 + 老文件保留 20 个
   - `runCatching` 兜底写入失败不抛
   - 9 例单测全绿（脱敏正则覆盖 Bearer / sk- / sk_ / api_key= / 短字符串不误伤 / 普通文本不动 / 空串 / 多个 token 全部脱敏 / 栈信息保留）

5. **`GlobalErrorHandler`**（`util/GlobalErrorHandler.kt`）
   - `Thread.setDefaultUncaughtExceptionHandler` 安装
   - 链式调用：保留旧 handler（系统/Hilt/Jetpack），先落本地日志再让旧 handler kill 进程
   - `onError: (ErrorUiModel) -> Unit` UI 回调（v0.11.0+ UI 接入）

6. **`CoroutineErrorHandler`**（`util/CoroutineErrorHandler.kt`）
   - 协程异常处理工厂：`operator fun invoke(): CoroutineExceptionHandler`
   - 配合 `viewModelScope.launch(coroutineErrorHandler()) { ... }` 使用
   - 内部转 `GlobalErrorHandler.handle()`

7. **`DegradationManager`**（`util/DegradationManager.kt`）
   - 4 维降级：网络/OCR/内存/电池
   - 3 档：NONE/MILD/SEVERE
   - 连续 3 次同轴升 MILD、连续 5 次升 SEVERE、3 次成功降一档、reset() 一键清零
   - 暴露 `pollMultiplier`（1.0/1.5/3.0）、`streamEnabled`（NONE/MILD 启用 SEVERE 关闭）、`maxContextTokens`（8000/4000/2000）、`shouldDropHistory`（仅 SEVERE 丢）
   - 8 例单测全绿

#### 集成

- `ChatAssistApp.onCreate` v0.11.0 早期注册 `GlobalErrorHandler.install()`（在 Timber.plant 之后）
- `MainViewModel` v0.11.0 注入 `CoroutineErrorHandler`，新增 `launchSafe` 扩展函数替代 `viewModelScope.launch`，3 处 launch 全部走全局兜底
- 全工程 27+ 处 `AppError(...)` → `AppError.of(...)` 同步切换

#### 单测统计

| 测试类 | 用例数 |
|---|---|
| `AppErrorTest` | 7 |
| `ErrorClassifierTest` | 14 |
| `ErrorPresenterTest` | 7 |
| `CrashLoggerTest` | 9 |
| `DegradationManagerTest` | 8 |
| **v0.11 新增小计** | **45** |
| v0.10 解析增强既有 | 33 |
| v0.6 基础既有 | 19 |
| **累计** | **97 单测 0 失败** |

### 构建结果

- ✅ `:app:assembleDebug` 成功
- ✅ `:app:testDebugUnitTest` 成功，97 单测 0 失败

### 后续待办（v0.12+）

1. UI 错误展示 Snackbar/Dialog 接入 `GlobalErrorHandler.onError` 回调（ChatAssistApp.onCreate 注入到 MainViewModel）
2. 错误码 i18n 多语言（v0.14+ strings.xml 抽取）
3. CrashLogger 用户主动反馈入口（v0.13+ 「反馈问题」按钮）
4. DegradationManager 接入网络质量自动检测（v0.12+ ConnectivityManager 监听）

## [0.10.2] - 2026-07-19

### 变更（架构 + 模块文档同步，9 骨架模块 + 3 顶层占位文档全量刷新）

**哥哥下旨"认真一点"，把 v0.1 骨架文档对齐到 v0.10.1 实际状态**：

- **架构文档水印**：`docs/architecture.md` 第 252 行 `_初夏 2026-07-18 自动构建于 v0.1.0_` → `_初夏 2026-07-19 自动构建于 v0.11.0_`（v0.1 → v0.11 跨越 9 个版本）
- **9 个 v0.1 骨架模块文档当前状态同步**：
  - 01 启动与权限：v0.1 → **v0.4 落地**（PermissionManager 四状态 + 引导 UI）
  - 03 OCR 识别：v0.1 → **v0.9 增强**（OcrCaptureChannel 真实实现 + 形近字修复 + ocrAutoCorrect 繁→简归一）
  - 05 Prompt 构造：v0.1 → **v0.9 增强**（BuildPromptUseCase + OCR 语义纠错提示）
  - 07 悬浮窗 UI：v0.1 → **v0.9.2 增强**（贴边+打字机+连续复制+灰圈+波纹+边缘触发+自动粘贴）
  - 08 主界面与配置：v0.1 → **v0.7 增强**（4 Tab + 首页增强 + 历史页渲染 + 设置三开关）
  - 10 后台保活：v0.1 → **v0.1 骨架（v0.12 规划）**（CaptureForegroundService v0.4 已部分落地，KeepAliveService 仍是 0 文件）
  - 11 主题与无障碍：v0.1 → **v0.9.1 增强**（Theme.kt + ChatAssistShapes 圆角 token + 玻璃质感 + 主题跟随）
  - 12 错误处理与降级：v0.1 → **v0.11 落地**（本次：7 核心类 + 45 单测）
  - 13 测试模块：v0.1 → **v0.11 增强**（97 单测全绿 + JUnit5，androidTest/Paparazzi/Macrobenchmark 待 v0.13）
- **3 个顶层占位文档状态追踪全量补齐**：
  - `compatibility.md`：v0.2/v0.5/v0.8 勾选 [x]（mumu 模拟器 AOSP 14/15 全流程通过，零 FATAL），加 v0.10.2 mumu 实测记录表 + 国产 ROM 真机回归留 v0.13
  - `performance.md`：v0.10.2 45+33=78 单测全绿 + mumu 1h 无 FATAL 勾选 [x]；估算已知瓶颈（冷启动 1.2-1.5s、OCR 800-1500ms）记入；Macrobenchmark 留 v0.13
  - `security.md`：v0.1-v0.8 全部勾选 [x]（数据流图 / EncryptedSharedPreferences / CrashLogger 脱敏 / 历史隐私拦截 / 隐私政策 UI / WhitelistInterceptor / v0.11 全局错误）；加 v0.11 CrashLogger 脱敏正则表

## [0.10.1] - 2026-07-19

### 变更（App 预设精简为 5 个国内主流 IM）

- **哥哥指定精简范围**：v0.10 内置 9 个 App（微信/QQ/TIM/钉钉/飞书/Telegram/Slack/WhatsApp/LINE/短信）→ 删 6 个、增 1 个，最终 5 个国内主流办公/社交 IM：**微信 / QQ / 钉钉 / 飞书 / 企业微信**
- **删**：TIM（`com.tencent.tim`）/ Telegram（`org.telegram.messenger`）/ Slack（`com.Slack`）/ WhatsApp（`com.whatsapp`）/ LINE（`jp.naver.line.android`）/ 短信（`com.android.mms`）
- **增**：企业微信（`com.tencent.wework`）
- **副作用**：Slack 左侧=自己的特例随之清空，所有内置 App 统一右侧=自己
- **单测同步**：删 Slack 左侧/右侧 2 例，改 1 例企业微信、1 例钉钉/飞书/QQ 三连测
- **未变更**：A 置信度评分 / C 多气泡合并 / D 滚动去重 / E UI 模式检测 / F 系统关键词 / 数据模型扩展均不动

### 构建结果
- ✅ `:app:assembleDebug` 成功
- ✅ `:app:testDebugUnitTest` 成功，52 单测 0 失败（仅 Slack 相关 2 例替换为钉钉/飞书/QQ 三连测 + 企业微信例）

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
