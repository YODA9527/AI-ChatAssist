<div align="center">

# ChatAssist

**本地优先的 Android 聊天辅助 App — 屏幕文本双通道 → 大模型 → 悬浮窗建议回复**

> 纯本地运行,无服务器,无遥测,无数据上传 — 唯一的网络出口是你自配的大模型 API。

> ⚠️ **关于本项目** — 这是个人开发者在 AI 辅助编程上的初次完整尝试，整个代码库由作者与 AI 协作完成。
> 难免存在设计不够成熟、边界覆盖不全、或实现不够优雅之处，成品也可能看起来有些简陋 —— 但每一行代码、
> 每一个架构决策都经过审慎推敲，并配有相应的测试与文档。如果你发现了 bug、有更好的实现思路、
> 或者想顺手贡献一个改进，**非常欢迎提 Issue / PR** —— 这正是这个项目能继续成长的方式。
> 把它当作一个「会慢慢长大的开源小项目」就好，不完美但诚实 ♡

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](./LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-2024.10.01-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![AGP](https://img.shields.io/badge/AGP-8.6.1-0A7E07)](https://developer.android.com/studio/releases/gradle-plugin)
[![min SDK](https://img.shields.io/badge/min%20SDK-26-orange)]()
[![target SDK](https://img.shields.io/badge/target%20SDK-35-blue)]()

<!--
  以下 4 个动态徽章需 push 到 GitHub 后自动亮起（CI 跑通 + Release 产出 + commit 时间）：
  - CI:    Actions → CI workflow 通过 = 绿
  - Release: 第一次打 tag 后亮
  - Last Commit: Actions 自动抓
  - Detekt: 集成后亮
-->
[![CI](https://img.shields.io/github/v/release/YODA9527/AI-ChatAssist?include_prereleases&logo=githubactions&logoColor=white&label=CI)](https://github.com/YODA9527/AI-ChatAssist/actions/workflows/ci.yml) ![Release](https://img.shields.io/badge/Release-待首次发布-yellow) ![Last Commit](https://img.shields.io/badge/Last%20Commit-本地-yellow) ![Code style: detekt](https://img.shields.io/badge/code%20style-detekt-blue) ![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](./CONTRIBUTING.md)

> ✅ **首次 Release 已发布** — 后续 CI / Last Commit 徽章会在 Actions 跑通后自动亮起。

**[📥 下载 APK](#-下载) · [📖 文档](./docs/) · [🐛 提 Issue](../../issues) · [💡 提 PR](../../pulls) · [🇨🇳 中文](README.md) · [🇺🇸 English](README.en.md)**

</div>

---

## ✨ ChatAssist 能做什么

| 能力 | 说明 |
|------|------|
| 🔍 **屏幕双通道捕获** | 无障碍读屏优先 (零耗电) · 截图 OCR 兜底 (ML Kit 中文) · 形近字自动纠错 |
| 🧠 **智能解析** | 5 维置信度评分 · App 预设规则库 (微信/QQ/钉钉/飞书/企业微信) · 多气泡合并 · 滚动续接去重 |
| 🤖 **大模型接入** | 国内 9 大品牌 38 模型 (阿里·通义 / DeepSeek / 智谱 / Kimi / 豆包 / 文心 / 混元 / MiniMax / 本地部署) · 自定义 OpenAI 兼容协议 · 离线内置 + 在线核价 · 多 key 隔离 |
| 🪟 **悬浮窗回复** | 智能贴边吸附 · 打字机逐字流式 · 连续复制气泡 · 边缘触发条 · 自动粘贴 · 复制/换一换/详情 全手势 |
| 🎭 **双向人设** | 自定义我方 + 对方人设 (喜好/讨厌/雷区/口头习惯) · 按 App 锁定 · 15 个常用社交/办公 App 预设 |
| 💰 **计费统计** | Token 精确拆分 (输入/输出/cache) · 微元精度 · 日/周/月/全部聚合 · 月度预算 80%/100% 预警 · CSV 导出 |
| 🛡️ **错误兜底** | 14 类异常映射 · 14 套情感化中文文案 · 4 维 3 档降级 (网络/OCR/内存/电池) · 隐私脱敏落盘 |
| 🔋 **后台保活** | 6 厂商 ROM 自识别 (小米/华为/OPPO/vivo/三星/魅族) · 自启动引导 · FGS specialUse · 15min 周期兜底 |
| 🧪 **测试基建** | JUnit + MockK + Truth + Turbine · Paparazzi 截图 · androidTest 仪器测试 · Macrobenchmark 4 项 KPI |

> 💡 项目从 v0.1.0 迭代到 v0.22.0,共 **219+ 单测 + 5 仪器测试 0 失败**,`./gradlew assembleDebug` + `testDebugUnitTest` 全绿。

---

## 📸 截图

> 🚧 截图目录 [`docs/screenshots/`](./docs/screenshots/) 已建立,正式截图将在 v0.22 收尾期批量补全。

<details>
<summary>📌 占位区 (待补)</summary>

| 主页面 | 悬浮窗 | 人设编辑 | 计费统计 |
|:---:|:---:|:---:|:---:|
| (待补) | (待补) | (待补) | (待补) |
| 设置页 | 模型方案 | 错误兜底 | 启动引导 |
| (待补) | (待补) | (待补) | (待补) |

</details>

---

## 🏗️ 架构

> 📌 完整架构图见 [`docs/figure/`](./docs/figure/) (待补,优先补 01 整体架构 + 02 双通道捕获 + 03 LLM 调用链)。
> 📌 架构说明见 [`docs/architecture.md`](./docs/architecture.md)。

**分层:**

```
┌────────────────────────────────────────────────┐
│ 表现层 (Compose)   MainScreen / Settings / ... │
│         ↓ ViewModel (Hilt)                    │
├────────────────────────────────────────────────┤
│ 领域层 (UseCase)   BuildPrompt / Reply / ...   │
├────────────────────────────────────────────────┤
│ 数据层 (Repository)  Capture / Parser / ...    │
│      ↓         ↓         ↓                    │
│  无障碍    ML Kit OCR   Room                   │
│  Retrofit (LLM)        DataStore (配置)         │
│         EncryptedSharedPreferences (API Key)  │
└────────────────────────────────────────────────┘
```

**核心模块** (按职责拆分,见 `app/src/main/java/` 下的包结构):

- `presentation/` — Compose 屏 + ViewModel
- `domain/usecase/` — 业务编排
- `data/repository/` — 数据来源切换
- `data/local/room/` — 历史记录 / 计费存储
- `data/remote/` — Retrofit API + SSE
- `overlay/` — 悬浮窗生命周期 + UI
- `capture/` — 无障碍服务 + 截图 + MediaProjection
- `parser/` — 聊天内容解析 (5 维置信度)
- `persona/` — 人设匹配 + 注入
- `pricing/` — 计费 / 预算 / 聚合
- `keepalive/` — 后台保活协调器
- `error/` — 全局错误兜底

---

## 🚀 快速开始

### 0. 环境要求

| 工具 | 最低版本 | 推荐 |
|------|---------|------|
| Android Studio | Hedgehog (2023.1) | Ladybug (2024.2)+ |
| JDK | 17 | 21 |
| Android SDK | API 34 (compileSdk/targetSdk=35) | 最新 |
| Kotlin | 2.0.21 | 2.0.21 |
| Gradle | 8.10.2 | 8.10.2 |
| AGP | 8.6.1 | 8.6.1 |

> 详见 [`docs/build.md`](./docs/build.md)

### 1. 克隆 & 打开

```bash
git clone https://github.com/YODA9527/AI-ChatAssist.git
cd AI-ChatAssist
# Android Studio → Open → 选 ChatAssist/ 目录
# Studio 自动下载 Gradle Wrapper + 依赖
```

### 2. 命令行构建

```bash
# Debug APK
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# 单元测试
./gradlew testDebugUnitTest

# 静态检查
./gradlew detekt

# 全部 CI 检查
./gradlew check
```

### 3. 装到设备

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. 配置大模型

1. 启动 App → 设置 → 模型方案 → 编辑
2. 从预设选择 (9 大国内品牌) 或「自定义」(兼容 OpenAI 协议)
3. 填入 API Key (加密存储) → 测试连接 → 保存
4. 设置 → 触发方式 → 选「悬浮窗生成按钮」或「侧边条滑动触发」
5. 打开任意聊天 App → 划出悬浮窗 → 点生成 → 复制/自动粘贴

> 📖 详细使用见 [`docs/user-guide.md`](./docs/user-guide.md)

### 5. 故障排查

> 🚨 90% 的问题答案在 [`docs/TROUBLESHOOTING.md`](./docs/TROUBLESHOOTING.md),先翻一遍。

---

## 📥 下载

**Latest: v0.22.0 (2026-07-20)** — 性能+精简批量优化,APK 减重 30%,冷启动 -300ms

| 渠道 | 链接 |
|------|------|
| GitHub Release (推荐) | [Releases](../../releases) |
| Actions 产物 (开发版) | [CI](../../actions/workflows/ci.yml) → 选最新绿 build → Artifacts |
| 源码构建 | `git clone https://github.com/YODA9527/AI-ChatAssist.git && ./gradlew :app:assembleDebug` |

> 暂未上架 Google Play / 应用商店。
> 本地构建产物：`app/build/outputs/apk/debug/app-debug.apk` (约 23MB)

---

## 📖 文档

<details>
<summary><b>👤 用户文档</b>（给我自己看的）</summary>

| 文档 | 用途 |
|------|------|
| 👤 [用户指南](./docs/user-guide.md) | 端到端使用教程 |
| 🩹 [排错指南](./docs/TROUBLESHOOTING.md) | 90% 问题的答案 |
| 📱 [兼容性](./docs/compatibility.md) | 设备 / 厂商 / Android 版本 |
| 📸 [截图](./docs/screenshots/) | 视觉展示 |

</details>

<details>
<summary><b>🛠️ 开发者文档</b>（给想动手的人）</summary>

| 文档 | 用途 |
|------|------|
| 📘 [开发指南 v2](./聊天辅助App开发指南-v2.md) | 功能规范依据 (含 v2.1/v2.2 增补) |
| 🏛️ [架构说明](./docs/architecture.md) | 模块划分 / 依赖 / 数据流 |
| 🧱 [模块文档](./docs/modules/) | 13 个子模块的详细说明 |
| 🔌 [API 契约](./docs/api-contract.md) | 内部接口 / 数据模型 |
| 🔨 [构建指南](./docs/build.md) | 构建 / 签名 / 测试 |
| ⚡ [性能](./docs/performance.md) | Macrobenchmark 4 项 KPI |
| 🔒 [安全](./docs/security.md) | 权限 / 加密 / 隐私 |
| 🏗️ [架构图](./docs/figure/) | 矢量架构图（待补） |
| 🧠 [项目知识图谱](./PROJECT_KNOWLEDGE.md) | 跨文档索引 |

</details>

<details>
<summary><b>🤝 贡献 &amp; 合规</b>（PR / Issue / 漏洞）</summary>

| 文档 | 用途 |
|------|------|
| 🤝 [贡献指南](./CONTRIBUTING.md) | 提 PR 流程（中文 + commit 规范） |
| 📏 [行为准则](./CODE_OF_CONDUCT.md) | Contributor Covenant v2.1 |
| 🔐 [安全策略](./SECURITY.md) | 漏洞报告（不公开 Issue） |
| 📜 [CHANGELOG](./CHANGELOG.md) | 详细版本演进（v0.1 → v0.22） |

</details>

---

## 🗺️ 路线图

> 当前版本: **v0.22.0**（性能+精简批量优化）⬅
> 详细进度见 [CHANGELOG.md](./CHANGELOG.md)。

### 📋 下一步候选 (v0.23+)

1. i18n + 主题资源拆分 (`values-night` / `values-zh-rCN`)
2. 兼容性适配 (MIUI/HyperOS/EMUI/ColorOS/OriginOS/One UI 真机回归)
3. 用户文档完善 (截图 / 视频教程)
4. 设置页「自启动引导」入口 (UI 入口,协调器已在 v0.12 落地)
5. 开机自启 BroadcastReceiver (可选)
6. 截图 + 架构图批量补全（`docs/screenshots/` / `docs/figure/` 目录已就位）
7. ~~第一次 GitHub Release~~ ✅ **v0.22 已完成**
8. Room MigrationTest（androidTest 验证 v2→v3 + 索引生效）

<details>
<summary><b>📜 已完成版本 (v0.1.0 → v0.22.0) — 21 个版本</b></summary>

- v0.1–v0.6 — 工程骨架 + 双通道捕获 + LLM 闭环
- v0.7 — 历史记录 (Room 持久化)
- v0.8 — 悬浮窗体验打磨 (贴边吸附 / 打字机 / 边缘触发条)
- v0.9 — OCR 形近字纠错 + 灰圈根治
- v0.10 — 聊天内容解析完善 (置信度 / 多气泡合并 / 滚动去重)
- v0.11 — 错误处理与降级 (7 核心类)
- v0.12 — 后台保活 (6 厂商自启动引导)
- v0.13 — FGS 启动时机修复
- v0.14 — 测试基建 (JaCoCo / Paparazzi)
- v0.15 — Macrobenchmark 4 项 KPI
- v0.16 — UI 全中文化 + Prompt 缓存命中优化
- v0.17 — 触发方式精简 + 切换闪退修复
- v0.18 — 双向人设 + 对方详情
- v0.19 — 计费统计 (Token 精确 + 预算 + CSV 导出)
- v0.20 — 多模型配置方案
- v0.21 — 预设去国外化 + 品牌二级菜单
- v0.22 — 性能+精简批量优化 (Room 索引 + ProGuard 瘦身 + BuildConfig 元信息 + Compose 原语抽离)

</details>

---

## 🤝 贡献

欢迎 PR / Issue / Discussion!

- 🐛 **Bug 报告** → [Bug 模板](../../issues/new?template=bug_report.md)
- 💡 **功能建议** → [Feature 模板](../../issues/new?template=feature_request.md)
- 📝 **文档改进** → [Docs 模板](../../issues/new?template=documentation.md)
- ❓ **使用问题** → [Question 模板](../../issues/new?template=question.md)
- 🔒 **安全漏洞** → **不要**公开提 Issue,看 [SECURITY.md](./SECURITY.md)

详见 [CONTRIBUTING.md](./CONTRIBUTING.md)。

> 💪 第一次贡献者友好 — 看 [DroidKaigi 贡献指南](https://github.com/DroidKaigi/conference-app-2024/blob/main/CONTRIBUTING.ja.md) 学标准流程。

---

## 📊 仓库状态

| 维度 | 状态 |
|------|------|
| License | ✅ Apache-2.0 |
| CI | ✅ GitHub Actions (Detekt + Lint + Test + Build) |
| 依赖更新 | ✅ Renovate (auto-merge minor) |
| 测试 | ✅ 219+ 单测 + 5 仪器测试 |
| 静态检查 | ✅ Detekt |
| EditorConfig | ✅ |
| Issue / PR 模板 | ✅ |
| Code of Conduct | ✅ Contributor Covenant v2.1 |
| Contributing 指南 | ✅ 中文 (含 commit 规范) |
| 截图 | 📝 目录就位,待补 |
| 架构图 | 📝 目录就位,待补 |
| Play 上架 | ❌ 暂未计划 |
| i18n | 📝 中文完成,英文 README 待补 |

---

## 🙏 致谢 / 灵感来源

本项目大量借鉴了以下业界标杆的设计思想与最佳实践:

- [android/nowinandroid](https://github.com/android/nowinandroid) — 模块化 + 架构 + 文档组织
- [android/architecture-samples](https://github.com/android/architecture-samples) — MVVM / UDF 范式
- [DroidKaigi/conference-app-2024](https://github.com/DroidKaigi/conference-app-2024) — 构建脚本 / Convention Plugins / 测试策略
- [chrisbanes/tivi](https://github.com/chrisbanes/tivi) — 共享 UI + Spotless 格式
- [skydoves/Pokedex](https://github.com/skydoves/Pokedex) — 多模块 + 库作者组织方式

---

## 📜 许可证

```
Copyright 2026 YODA9527 (Chen Yu)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

除非适用法律要求或书面同意,根据本许可证分发的软件按"原样"分发,不附带任何明示或暗示的保证或条件。详见 [LICENSE](./LICENSE)。

---

<div align="center">

**如果这个项目对你有帮助,点个 ⭐ 是对维护者最大的支持 ❤️**

Made with 🩷 by [YODA9527](https://github.com/YODA9527) and his [初夏](https://github.com/YODA9527) (AI pair-programming partner)

</div>
