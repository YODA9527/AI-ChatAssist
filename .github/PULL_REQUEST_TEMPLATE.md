<!--
  感谢你提 PR!请填写下面所有 ⚠️ 必填部分,这样 review 才高效。
  模板说明: https://docs.github.com/zh/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-template
-->

## 变更说明

⚠️ **做了什么 / 为什么做 (必填)**

> 例: 修复悬浮窗在 MIUI 14 上生成按钮点不到的 bug。原因是 IME insets 未透传到 overlay 容器,导致按钮被状态栏遮挡。

## 关联 Issue

<!--
  用 `Fixes #123` / `Closes #456` / `Refs #789` 关联 issue,
  GitHub 会自动在合并时关闭对应 issue。
-->

- Fixes #
- Closes #
- Refs #

## 变更类型

<!-- 必选一项或多项,删除不适用的 -->

- [ ] 🐛 Bug 修复 (fix)
- [ ] ✨ 新功能 (feat)
- [ ] 💥 破坏性变更 (BREAKING CHANGE)
- [ ] 📝 文档 (docs)
- [ ] ♻️ 重构 (refactor, 无新功能/无 bug 修复)
- [ ] 🧪 测试 (test)
- [ ] 🔧 构建/工具/杂项 (chore)
- [ ] 🚀 性能 (perf)
- [ ] 🎨 格式 (style)

## 影响范围

<!-- 让 reviewer 知道要重点看哪 -->

- 涉及模块: 例: `app/.../presentation/overlay/`
- 涉及 API/数据: 例: `ModelConfig` 加字段
- 涉及权限: 例: 无 / 新增 `POST_NOTIFICATION`
- 涉及迁移: 例: Room v3 → v4,需写 MigrationTest
- 影响版本: 例: 锁定 v0.22.0

## 测试

<!-- 必填: 改了什么 / 跑了什么 / 怎么验 -->

- [ ] 写了单元测试 (`./gradlew testDebugUnitTest` 全绿)
- [ ] 写了截图测试 (Paparazzi / Roborazzi)
- [ ] 写了仪器测试 (`./gradlew connectedDebugAndroidTest`)
- [ ] 写了 Macrobenchmark
- [ ] **未加测试,理由:** ___________

测试命令及结果:

```text
$ ./gradlew testDebugUnitTest
BUILD SUCCESSFUL — 215 tests, 0 failures
```

## 验证清单

- [ ] `./gradlew assembleDebug` 通过
- [ ] `./gradlew testDebugUnitTest` 通过
- [ ] `./gradlew detekt` 通过 (或已知警告已加 `@Suppress`)
- [ ] `./gradlew lintDebug` 通过 (或已知警告已加理由)
- [ ] 本地装到模拟器手测过,无闪退
- [ ] UI 改动附了截图 / 录屏
- [ ] CHANGELOG.md 加了对应条目 (若 release 收尾期)
- [ ] README.md / docs/ 同步更新 (若涉及)

## 截图 / 录屏 (UI 改动必备)

<!-- 拖图或粘贴链接 -->

| Before | After |
|--------|-------|
| (贴图) | (贴图) |

## 备注

<!-- 任何想告诉 reviewer 的事: 折中方案、暂未处理项、follow-up 计划 -->
