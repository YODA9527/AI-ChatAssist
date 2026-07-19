# ChatAssist

> 一款本地优先的 Android 聊天辅助 App。
> 屏幕文本双通道（无障碍读屏优先 + 截图 OCR 兜底）→ 解析 → 大模型 → 悬浮窗建议回复。
> **纯本地运行，唯一网络出口是你自配的大模型 API。**

## 当前进度

> 工程已迭代至 **v0.9.1（悬浮窗灰圈修复）**：双通道捕获 → 解析 → 流式 LLM 回复 → 悬浮窗展示，`assembleDebug` 构建绿（BUILD SUCCESSFUL，mumu 模拟器真机验证通过）。

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

> ⚠️ **关于 APK 体积**：本项目不限制 APK 体积，以功能完整性优先（详见 v0.2 决策）。Debug 包因带 ML Kit 中文识别模型（≈20MB）较大属正常。

## 路线图

按 [`聊天辅助App开发指南-v2.md`](./聊天辅助App开发指南-v2.md) 第 11 章 14 步 + v2.1/v2.2 增补，已完成 1–8 步 + 模块09 历史记录（v0.1–v0.7.0）：

**已完成（✅）**
1. 工程骨架 ✅　2. 文档骨架 ✅　3. ConfigRepository + 主界面 ✅　4. ModelApiRepository ✅
5. ScreenCaptureRepository ✅　6. OcrRepository ✅　7. ChatParserRepository ✅　8. Prompt 构造 + 调用闭环 ✅
9. 历史记录（模块 09，v0.7.0）✅
10. 悬浮窗打磨（模块 07/v0.8，v2.1 §12.1）✅

**待办（⬜）**
9. 悬浮窗打磨（v0.8：贴边吸附 / 边缘触发条 / 连续复制 / 自动粘贴 / 打字机）✅ 已完成
10. 历史记录持久化（模块 09：✅ v0.7.0 已接入 Room，历史页可查看 / 删除 / 清空）
11. 后台保活（模块 10：WorkManager 拉活，当前仅前台服务）
12. 计费统计（v2.2 §13.3：BillingRecord / TokenUsage / 预算提醒，完全未做）
13. 全量测试 + 性能调优（模块 12 / 第 6 章 KPI）
14. 兼容性适配（模块 13：各 ROM 真机回归）
15. 用户文档（模块 14 / `docs/user-guide.md` 完善）

## 快速开始

```bash
# 克隆
git clone https://github.com/<your-name>/ChatAssist.git
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
