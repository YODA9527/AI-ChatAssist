# 安全策略

## 支持的版本

下表列出当前获得安全更新的 ChatAssist 版本:

| 版本 | 支持 |
|------|------|
| `v0.21.x` | ✅ 积极维护 |
| `v0.20.x` | ✅ 安全更新 |
| `v0.19.x` | ⚠️ 关键漏洞修复 (按需) |
| `< v0.19` | ❌ 不再支持 |

---

## 报告漏洞

**请不要通过公开 GitHub Issue 报告安全漏洞。**

请通过以下任一私密渠道报告:

1. **GitHub 私有 Issue**: 仓库 → Security tab → "Report a vulnerability"
   (URL: `https://github.com/YODA9527/AI-ChatAssist/security/advisories/new`)
2. **邮件**: 见 [`CODEOWNERS`](./CODEOWNERS) 中的维护者联系方式 (待补)

请在报告中尽可能包含:

- 漏洞类型与影响范围
- 复现步骤 / PoC (代码 / 截图 / logcat)
- 受影响版本
- 已知缓解方案 (若有)

---

## 响应时效

| 阶段 | 时限 |
|------|------|
| 初次确认 | 72 小时内 |
| 评估严重等级 | 7 天内 |
| 修复发布 (高/严重) | 30 天内 |
| 修复发布 (中) | 90 天内 |
| 修复发布 (低) | 下个 release |

---

## 安全最佳实践 (本项目)

虽然 ChatAssist 是本地优先应用,网络出口仅限于用户自配的大模型 API,我们仍遵守以下原则:

- ✅ **API Key 加密**: 使用 EncryptedSharedPreferences 存储,不入 DataStore 明文区
- ✅ **密钥不外传**: `local.properties` 已在 `.gitignore`
- ✅ **依赖审计**: 启用 Renovate + Dependabot 持续跟踪
- ✅ **ProGuard/R8**: release 包混淆
- ✅ **最小权限**: `AndroidManifest.xml` 权限逐条审过,无任何 `QUERY_ALL_PACKAGES` 等过度权限
- ✅ **网络安全**: 仅允许 HTTPS 出站
- ✅ **代码审计**: PR 必须过 CI + 维护者 review

详见 [`docs/security.md`](./docs/security.md)。
