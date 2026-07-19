# 模块 5 — Prompt 构造

> 状态：**v0.1 骨架** / 优先级 P0

## 职责边界

把解析出的 `List<ChatMessage>` + 人设 + 语气 + 历史上下文，组装成最终发给大模型的 Prompt。
**少样本学习**（Few-shot）+ **双向人设** + **语气模板** + **上下文截断**。

## 输入输出契约

```kotlin
data class BuiltPrompt(
    val systemPrompt: String,     // 含人设/语气/任务定义
    val messages: List<PromptMessage>,
    val temperature: Double,
    val maxTokens: Int,
    val model: String,
)

data class PromptMessage(
    val role: Role,                // SYSTEM / USER / ASSISTANT
    val content: String,
)

enum class Role { SYSTEM, USER, ASSISTANT }
```

## 关键类

- `BuildPromptUseCase`
  - `suspend invoke(messages, persona, goal): Result<BuiltPrompt>`

- `PersonaTemplate` — 把 `DualPersona` 拼成 System Prompt 段落
- `ToneTemplate` — 把语气模板拼成 System Prompt 段落
- `ContextTruncator` — 上下文超长时按时间倒序截断
- `FewShotProvider` — 少样本示例（v0.5+）

## Prompt 结构（System 部分）

```
# 任务
你是聊天回复建议生成器，根据上下文给出 3 条候选回复。

# 我的人设
{self_persona}

# 对方的人设
{counterpart_persona}

# 我的语气
{tone_template}

# 当前对话目标
{goal}

# 回复要求
- 候选数：3 条
- 风格：与对方匹配，与我的人设一致
- 长度：1-3 句，不要长篇大论
- 格式：每条独立一行，前面加 [1] [2] [3]

# 少样本示例（Few-shot，可选）
{examples}
```

## 当前状态

- [x] 骨架就位
- [ ] 详细实现（v0.6）

## 后续待办

1. DualPersona → System Prompt 转换
2. ToneTemplate → System Prompt 转换
3. Goal 拼入（PROFESSIONAL / CASUAL / WARM / FLIRTY / ...）
4. 上下文截断（按 maxContextTokens）
5. 少样本示例库（v0.5+）
6. 多模态 Vision Prompt 变体（v0.7+）
7. 单元测试：`BuildPromptUseCaseTest`

## 风险点

- 不同模型对 Prompt 格式敏感（OpenAI 兼容 vs Claude 缓存标记 vs 国产模型）
- 上下文超长时截断策略可能丢失关键信息（保留最后 N 轮 + System 必带）
- 少样本选择需要高质量示例
