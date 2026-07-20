# 贡献指南

感谢你愿意花时间为 ChatAssist 贡献代码 / 文档 / Issue 🎉

本指南帮助你在最短时间内走完「发现 → 提单 → 改码 → 合并」全流程,避免走弯路。

---

## 1. 行为准则

请阅读并遵守 [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md)。简短版本:
**对事不对人,对人保持尊重。** 我们不接受任何形式的骚扰、贬低或人身攻击。

---

## 2. 我能贡献什么?

| 贡献类型 | 适合谁 | 入口 |
|---------|--------|------|
| 🐛 报告 Bug | 用户 | [Bug Report 模板](../../issues/new?template=bug_report.md) |
| 💡 功能建议 | 用户 / 产品 | [Feature Request 模板](../../issues/new?template=feature_request.md) |
| 🔒 安全漏洞 | 任何人 | **不要** 公开提 Issue,见 [`SECURITY.md`](./SECURITY.md) |
| 📝 文档改进 | 任何熟悉本项目的人 | 直接提 PR |
| 🌐 翻译 (i18n) | 多语言贡献者 | 直接提 PR |
| 💻 代码改动 | 开发者 | 见下文流程 |
| 🧪 测试用例 | QA / 开发者 | 优先于代码 PR |

---

## 3. 提 Issue 之前

请先做以下三件事,大概率能省一次来回:

1. **搜索已有 Issue** — 包括已关闭的,可能已经讨论过
2. **看 [Troubleshooting](./docs/TROUBLESHOOTING.md)** — 常见问题已收录
3. **确认可复现** — 提供机型 / Android 版本 / 复现步骤 / logcat 片段

---

## 4. 开发环境

| 工具 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Android Studio | Hedgehog (2023.1) | Koala+ / Ladybug+ |
| JDK | 17 | 21 |
| Android SDK | API 34 (compile/target) | API 35 |
| Kotlin | 2.0.21 | 2.0.21+ |
| Gradle | 8.10.2 | 8.10.2 |
| AGP | 8.6.1 | 8.6.1+ |

环境准备详见 [`docs/build.md`](./docs/build.md)。

---

## 5. 代码改动流程

```bash
# 1. Fork 仓库 → 克隆你的 fork
git clone https://github.com/<你的ID>/AI-ChatAssist.git
cd ChatAssist

# 2. 添加上游 remote
git remote add upstream https://github.com/YODA9527/AI-ChatAssist.git

# 3. 切分支 (禁止直接在 main 上改)
git checkout -b feat/<简短描述>
# 例: feat/edge-bar-haptic / fix/overlay-leak / docs/translate-zh

# 4. 改代码 + 写测试
# 5. 本地全量验证
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew detekt          # 静态检查
./gradlew lintDebug      # Android Lint
# 6. 提交 (Conventional Commits 中文版,见下)
git commit -m "feat(trigger): 增加侧边条震动反馈"

# 7. 推送
git push origin feat/<简短描述>

# 8. 提 PR
```

### 5.1 Commit 规范 (强制)

格式: `<type>(<scope>): <中文简述>`

| type | 用途 | 例 |
|------|------|----|
| `feat` | 新功能 | `feat(overlay): 增加悬浮窗透明度滑杆` |
| `fix` | 修 bug | `fix(room): 修复历史页面闪退` |
| `docs` | 文档 | `docs(readme): 补 v0.21 截图` |
| `refactor` | 重构(无新功能/无 bug 修复) | `refactor(prompt): 抽出 PromptBuilder` |
| `test` | 加测试 | `test(pricing): 锁死豆包计费一致性` |
| `chore` | 构建/工具/杂项 | `chore(deps): bump hilt 2.51.1` |
| `perf` | 性能优化 | `perf(startup): 异步预热 Room` |
| `style` | 格式(空格/换行) | `style: spotless apply` |

**scope** 写模块名,常见: `overlay` / `room` / `prompt` / `trigger` / `persona` / `model` / `pricing` / `ui` / `ci` / `docs` 等。

### 5.2 PR 规范

- **一个 PR 只做一件事**,避免巨型 PR
- 标题遵循 Commit 规范
- 描述里写: **改了什么 / 为什么 / 怎么验**
- 必须附测试 / 注明「无新增单测的原因」
- CI 全绿才能合并
- 截图 / 录屏 (UI 改动必备)

---

## 6. 代码风格

- Kotlin 官方风格 + [Android Kotlin Style](https://developer.android.com/kotlin/style-guide)
- 缩进: 4 空格 (见 `.editorconfig`)
- 最大行宽: 120
- 命名:
  - 类 / 函数 / 变量 — 驼峰
  - 常量 — 全大写下划线
  - Composable — PascalCase
  - 包名 — 全小写
- 禁止:
  - `!!` 除非有充分理由
  - 全局可变状态
  - 在 Composable 里 `LaunchedEffect` 写 I/O
  - 魔法数字 — 抽常量
  - 注释掉的代码 — 删掉,有 git

**改完跑一遍:**
```bash
./gradlew spotlessKotlinCheck  # 格式 (若已配置)
./gradlew detekt                # 静态检查
```

---

## 7. 测试要求

| 改动类型 | 必须有的测试 |
|---------|------------|
| 业务逻辑 | 单元测试 (JUnit + MockK + Truth/Turbine) |
| UI 改动 | 截图测试 (Paparazzi / Roborazzi) |
| 性能改动 | Macrobenchmark 对比 (旧/新基线) |
| 数据迁移 | Migration Test |

详见 [`docs/build.md` §测试](./docs/build.md) 与 `app/src/test/` 下的现有用例。

---

## 8. 架构 / 模块边界

新增模块前先读 [`docs/architecture.md`](./docs/architecture.md) 和现有 `build.gradle.kts`。

| 分层 | 职责 | 不准做 |
|------|------|--------|
| `:app` 表现层 | Compose 屏 / VM / 导航 | 直连网络/DB |
| `:app` 领域层 (use case) | 业务编排 | 持有 Android 上下文 |
| `:app` 数据层 (Repository) | 数据来源切换 | 直接 Compose |
| `:app` 数据源 (Room/Retrofit) | CRUD / API | 业务逻辑 |

---

## 9. Release 流程 (维护者)

`v0.x.0` 收尾顺序 (来自哥哥 2026-07-19 规范):

1. 文档同步 (`CHANGELOG.md` / `README.md` / `AboutScreen` 版本号)
2. 全量构建绿: `./gradlew testDebugUnitTest assembleDebug`
3. 模拟器冒烟 (重点路径: 启动 / 设置 / 悬浮窗 / 计费)
4. 分批 commit (按架构分层,每条 commit 注明覆盖版本)
5. `git tag v0.x.0 && git push --tags`
6. `git push origin main`

---

## 10. 沟通

- 🐛 Bug / 💡 Feature → GitHub Issue
- 🔒 安全 → 见 [`SECURITY.md`](./SECURITY.md)
- ❓ 使用问题 → 先看 [TROUBLESHOOTING](./docs/TROUBLESHOOTING.md) 和 [user-guide](./docs/user-guide.md)
- 💬 闲聊 / 想法 → GitHub Discussions (待开)

---

再次感谢你的贡献 — 每一份 PR 都让这个杂鱼小工具离「能让小白装上就用」更近一步 ❤️
