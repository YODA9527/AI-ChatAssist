---
name: 🐛 Bug 报告
about: 报告一个让 ChatAssist 跑不起来的 bug
title: "[Bug] "
labels: ["bug", "triage"]
assignees: []
---

## 现象描述

用一两句话说清楚出了什么问题。

<!-- 例: 悬浮窗生成回复后,点击「复制」按钮 App 直接闪退 -->

## 复现步骤

1. 第一步
2. 第二步
3. …

## 期望行为

应该出现什么。

## 实际行为

实际出现了什么 (贴 logcat / 截图 / 录屏)。

## 环境信息

| 项目 | 值 |
|------|----|
| ChatAssist 版本 | v0.21.0 (从「设置→关于」查) |
| Android 版本 | 例: 14 (HyperOS 1.0.5) |
| 设备 | 例: 小米 14 Pro |
| 安装方式 | 例: `adb install -r app-debug.apk` |
| 模型服务商 | 例: 智谱 GLM-4 / DeepSeek / 自定义 |
| API Base URL | 例: `https://open.bigmodel.cn/api/paas/v4` |

## logcat 片段

```text
// 在 issue 正文里贴关键异常栈,不要全贴
```

## 已尝试

你尝试过哪些 workaround?

## 关联 Issue

- Related to #
- Blocks #

---

✅ **提交前请确认:**
- [ ] 已搜索过现有 Issue,没有重复
- [ ] 已阅读 [`docs/TROUBLESHOOTING.md`](../../docs/TROUBLESHOOTING.md)
- [ ] 提供了 logcat 和设备信息
- [ ] 描述了复现步骤
