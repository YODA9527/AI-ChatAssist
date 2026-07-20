# 兼容性测试报告

> 记录 ChatAssist 在不同 Android ROM 与设备形态下的兼容性情况。
> 当前状态：**v0.10.2 mumu 模拟器（AOSP）已通过，国产 ROM 真机回归待 v0.13**

---

## 1. 必测 ROM

| ROM | 厂商 | 优先级 | 当前状态 |
|-----|------|--------|----------|
| 原生 Android 8.0 | AOSP | P0 | 待测 |
| 原生 Android 10 | AOSP | P0 | 待测 |
| 原生 Android 12 | AOSP | P0 | 待测 |
| 原生 Android 14 | AOSP | P0 | 待测 |
| 原生 Android 15 | AOSP | P0 | 待测 |
| MIUI 14 | 小米/红米 | P0 | 待测 |
| HyperOS | 小米 | P0 | 待测 |
| EMUI 12 | 华为 | P0 | 待测 |
| 鸿蒙 3.0/4.0 | 华为 | P0 | 待测 |
| ColorOS 14 | OPPO/一加 | P0 | 待测 |
| OriginOS 4 | vivo | P0 | 待测 |
| One UI 6 | 三星 | P0 | 待测 |
| Flyme 10 | 魅族 | P1 | 待测 |
| ZUI 15 | 联想 | P1 | 待测 |

## 2. 必测设备形态

| 形态 | 屏幕尺寸 | 优先级 | 当前状态 |
|------|----------|--------|----------|
| 直屏 | 6.1" | P0 | 待测 |
| 大屏 | 6.7" | P0 | 待测 |
| 折叠屏内屏 | 7.6"+ | P0 | 待测 |
| 平板 | 10"+ | P2 | 不强制优化但不可崩 |

## 3. 已知兼容性坑（必须处理）

| 坑 | 处理 | 实现模块 |
|----|------|----------|
| MIUI 悬浮窗权限名称特殊 | `Settings.canDrawOverlays` 兜底 + 引导跳转 | 启动与权限 |
| 华为设备无 Google 服务 | ML Kit 必须用 bundled 版（非 GMS） | OCR 识别 |
| Android 14 前台服务类型强制声明 | manifest 加 `foregroundServiceType` | 屏幕捕获 / 后台保活 |
| Android 12+ 截图需明确前台服务通知 | 通知常驻 | 屏幕捕获 |
| Android 10+ 后台启动 Activity 限制 | 透明 Activity 规避 | 后台保活 |
| 折叠屏展开/折叠时 Compose 重组状态丢失 | ViewModel 持久化状态 | 主题与无障碍 |
| Android 12+ PendingIntent FLAG_IMMUTABLE | 全部加 | 屏幕捕获 / 通知 |
| 小米/华为/OPPO 自启动管理严格 | 引导用户加白名单 | 后台保活 |
| 三星 One UI 后台限制 | 引导用户设置 | 后台保活 |

## 4. 测试方法

### 4.1 真机测试
- 准备至少 3 台真机覆盖：原生 + 国产 ROM（小米/华为/OPPO/vivo 各 1）
- 手工跑核心流程：申请权限 → 触发识别 → OCR → 解析 → 调用模型 → 复制

### 4.2 模拟器测试
- Android Studio 模拟器覆盖各 API 级别（26/29/31/34/35）
- 重点验证 Compose 重组、生命周期、权限状态机

### 4.3 自动化
- Espresso 仪器测试覆盖主流程
- 截图比对（Paparazzi）覆盖主题/关键组件

## 5. 状态追踪（v0.12.0）

- [x] v0.2 — **mumu 模拟器（emulator-5554/7555，AOSP 14/15 模拟器）** 双通道捕获 / 解析 / LLM / 悬浮窗 全流程跑通，零 FATAL（v0.6-v0.9 多次验证）
- [x] v0.5 — v0.10 解析完善 / v0.11 错误处理框架 / v0.12 后台保活在 mumu 上冷启无 app 崩溃（logcat "FATAL" 实为 monkey/MUMU 自身日志）
- [x] v0.8 — 悬浮窗 v0.8.0/v0.9.1/v0.9.2/v0.9.2 在 mumu 上逐项验证通过（智能贴边 / 灰圈根治 / 波纹根治）
- [x] v0.12 — KeepAliveService 启动成功 + 通知常驻（mumu 模拟器 notification dump 验证，可见通知「ChatAssist 后台保活中」）
- [ ] v0.13 — 国产 ROM 真机回归（MIUI/HyperOS/EMUI/ColorOS/OriginOS/One UI 各 1 台）
- [ ] v0.13 — 6 厂商自启动 Intent 实测调整（VendorGuide 路径以官方 ROM 为准）
- [ ] v0.13 — 折叠屏内屏（Galaxy Z Fold / Mate X）适配
- [ ] v1.0 — 完成 9 ROM × 4 形态全兼容性矩阵

### 5.1 v0.10.2 mumu 模拟器实测记录

| 模块 | mumu 状态 | 备注 |
|------|-----------|------|
| 启动 + 权限 | ✅ 冷启无 FATAL | v0.4 PermissionManager 四状态 + 引导 UI 正常 |
| 屏幕捕获（无障碍主通道） | ⚠️ mumu 模拟器对无障碍服务绑定支持有限 | 模拟器无图可读，**精准模式建议真机 / 官方 AVD 验证** |
| 屏幕捕获（OCR 兜底） | ✅ 2s 轮询正常，ML Kit 中文模型加载成功 | mumu 上主要走 OCR_ONLY 通道 |
| ChatParser | ✅ v0.10 五维能力验证通过 | mumu logcat 显示消息 source=OCR_ONLY |
| 悬浮窗 | ✅ v0.8/v0.9.1/v0.9.2 玻璃质感 + 灰圈根治 + 波纹根治验证通过 | TYPE_APPLICATION_OVERLAY 正常挂载 |
| LLM 流式 | ⚠️ 无 API Key 未端到端验证 | v0.3 17 单测全绿 + 测试连接 401 → 200 修复验证通过 |
| 历史 Room | ✅ chatassist_history 库建库正常 | mumu sqlcipher 兼容 |

### 5.2 环境限制（mumu 模拟器已知）

- 无图可读（image 工具不支持）→ UI 视觉手工验证
- 无微信/QQ 等真实聊天 app → 未端到端验证无障碍捕获
- 无真实 API Key → LLM 实调未跑
- 无障碍服务同进程（manifest 未声明 process）→ Hilt 单例共享、跨进程问题不存在
- `evaluateAccessibility enabled=false` 冷启后为 Android 标准行为（`am force-stop` 会禁用 app 的无障碍服务需重新授权）
