# 📸 Screenshots / 截图

> 此目录存放 App 截图、动图、演示视频,用于 README 和 `docs/` 文档展示。

## 收录规范

| 维度 | 规范 |
|------|------|
| 格式 | `.png` 静态截图 / `.webp` 压缩版 / `.gif` 或 `.webm` 动图 |
| 分辨率 | 1080×2400 起,推荐 1440×3200 (高分屏压缩前) |
| 大小 | 单图 ≤ 2 MB,动图 ≤ 8 MB |
| 命名 | `<模块>_<功能>_<状态>.png`,例: `main_home_idle.png`、`overlay_reply_streaming.png` |
| 主题 | 同时提供浅色/深色两版 (后缀 `_light` / `_dark`) |
| 隐私 | 打码后再放: 微信号 / 头像 / 聊天内容 / API Key |

## 推荐截图清单 (随版本更新)

- [ ] `splash.png` — 启动屏
- [ ] `main_home_idle.png` / `main_home_active.png` — 主页面
- [ ] `overlay_compact.png` / `overlay_expanded.png` — 悬浮窗两态
- [ ] `overlay_reply_streaming.png` — 悬浮窗流式生成中
- [ ] `settings_general.png` / `settings_persona.png` / `settings_model.png` — 设置页
- [ ] `model_scheme_edit.png` / `model_scheme_brand_picker.png` — 模型方案
- [ ] `persona_edit_self.png` / `persona_edit_counterpart.png` — 人设编辑
- [ ] `pricing_overview.png` / `pricing_by_model.png` / `pricing_by_app.png` — 计费页
- [ ] `overlay_demo.gif` — 端到端演示 (录屏)

## 制作方式

| 方式 | 工具 | 适用 |
|------|------|------|
| 真机/模拟器截图 | `adb shell screencap` / 模拟器截图工具 | 真实界面 |
| Paparazzi 截图测试 | `app/src/test/.../Paparazzi*Test.kt` | 组件 / 主题对比 |
| Roborazzi 截图测试 | `app/src/test/.../Roborazzi*Test.kt` | Compose 预览 |

Paparazzi/Roborazzi 截图存在仓库内,作为视觉回归基线;真机截图放此目录作展示用。

## 当前状态

🚧 占位,尚未开始批量截图,v0.22 收尾期统一补。
