# 性能测试报告

> 记录 ChatAssist 性能指标的实测数据与改进路线。
> 当前状态：**v0.10.2 mumu 模拟器无 FATAL/v0.11 错误处理 45 例单测全绿，Macrobenchmark 启动测试待 v0.13**

---

## 1. 硬性 KPI（第六章）

| 指标 | 目标 | 当前实测 | 状态 | 测量方式 |
|------|------|----------|------|----------|
| 冷启动可交互 | ≤ 800ms | 待测 | ⏳ | Macrobenchmark |
| 热启动 | ≤ 300ms | 待测 | ⏳ | Macrobenchmark |
| 触发→出结果 P50 | ≤ 3s | 待测 | ⏳ | 真机埋点 |
| 触发→出结果 P95 | ≤ 6s | 待测 | ⏳ | 真机埋点 |
| OCR 单次 | ≤ 500ms | 待测 | ⏳ | 真机埋点 |
| 截图单次 | ≤ 100ms | 待测 | ⏳ | 真机埋点 |
| 悬浮窗动画帧率 | ≥ 55fps | 待测 | ⏳ | Perfetto |
| 内存峰值（运行 1h） | ≤ 250MB | 待测 | ⏳ | Android Profiler |
| 后台待机 1h 耗电 | ≤ 2% | 待测 | ⏳ | 真机实测 |
| ~~APK 体积~~ | ~~≤ 60MB~~ | **已取消** | N/A | 需求方要求功能完整优先 |
| 无 ANR | 0 | 待测 | ⏳ | 真机测试 |
| 无 Crash | 0 | 待测 | ⏳ | 真机测试 |

## 2. 关键路径时间预算

| 阶段 | P50 目标 | 累计 P50 | 备注 |
|------|----------|----------|------|
| 截图（含 MediaProjection） | 100ms | 100ms | |
| OCR 识别 | 500ms | 600ms | |
| 解析 + 构造 Prompt | 200ms | 800ms | |
| 模型首 token（流式） | 1200ms | 2000ms | |
| 流式渲染剩余内容 | 1000ms | 3000ms | |
| **合计 P50** | — | **3000ms** | ✅ |
| **合计 P95** | — | **6000ms** | ✅ |

## 3. 测量工具

| 工具 | 用途 | 接入方式 |
|------|------|----------|
| Macrobenchmark | 冷/热启动 | 单独 benchmark 模块 |
| Perfetto | 帧率、系统调用 | Android Studio Profiler |
| Android Profiler | 内存、CPU、网络 | Android Studio |
| 真机埋点 | 关键路径耗时 | `PerformanceTracker` 工具类 |
| Battery Historian | 电量 | ADB 抓 battery trace |

## 4. 改进路线

### 4.1 启动优化
- Baseline Profile（启动 + 关键路径热点预编译）
- 启动器 SplashScreen API
- Hilt 启动开销优化（懒加载）

### 4.2 OCR 优化
- 预热 ML Kit（App 启动时 warmUp）
- 截图降采样（仅识别必要区域）
- 异步并行（OCR + 解析可重叠）

### 4.3 模型调用优化
- 流式响应（首 token 后立即渲染）
- 请求合并（短时间内多次触发合并为 1 次）
- 本地缓存（相同消息结构命中缓存）

### 4.4 渲染优化
- 悬浮窗 Compose 懒加载
- Bitmap 复用（避免重复分配）

## 5. 状态追踪（v0.10.2）

- [x] v0.10.2 — **错误处理单测 45 例 0 失败**（AppError 7 + ErrorClassifier 14 + ErrorPresenter 7 + CrashLogger 9 + DegradationManager 8）
- [x] v0.10.2 — **解析增强单测 33 例 0 失败**（ChatParser 14 + MessageMerger 7 + MessageDeduplicator 6 + ChatParserTest 既有 6）
- [x] v0.10.2 — mumu 模拟器 1h 冷启动 / force-stop / 后台↔前台切换均无 FATAL
- [x] v0.10.2 — `app:assembleDebug` 构建绿 6s、`app:testDebugUnitTest` 97 单测 0 失败
- [ ] v0.13 — 接入 PerformanceTracker + Macrobenchmark 启动测试
- [ ] v0.13 — 跑通核心路径 P50 ≤ 3s（截图 100ms + OCR 500ms + 解析 200ms + 模型首 token 1200ms + 渲染 1000ms）
- [ ] v0.13 — 跑通冷启动 ≤ 800ms / 热启动 ≤ 300ms
- [ ] v1.0 — 所有 12 项 KPI 达标（含悬浮窗 ≥ 55fps、内存 ≤ 250MB、后台耗电 ≤ 2%）

### 5.1 已知瓶颈（v0.10.2 估算）

| 路径 | 估算 | 优化空间 |
|------|------|----------|
| 冷启动（含 Hilt + Compose） | 1.2-1.5s（AOSP 14 模拟器） | Baseline Profile + SplashScreen API → 目标 800ms |
| OCR 截图+识别单次 | 800-1500ms | 截图降采样 + ML Kit 预热 → 目标 600ms |
| 触发→出结果 P50 | 3.5-4s | 流式首 token 优化 + 本地缓存 → 目标 3s |
| 悬浮窗动画 | 60fps（mumu 模拟器） | 已用 draw 阶段避免 graphicsLayer 触发 |
