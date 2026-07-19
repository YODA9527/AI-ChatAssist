# 模块 3 — OCR 识别

> 状态：**v0.1 骨架** / 优先级 P0

## 职责边界

封装 Google ML Kit Text Recognition v2：Bitmap → List<OcrBlock>（含文本、坐标、置信度）。
**多策略融合**应对中英文混排、深浅色 UI、复杂背景。
**不负责**聊天语义解析（那是 4 号模块的事）。

## 输入输出契约

```kotlin
data class OcrBlock(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float,
    val lines: List<OcrLine>,
)

data class OcrLine(
    val text: String,
    val boundingBox: Rect,
    val elements: List<OcrElement>,
)

data class OcrResult(
    val blocks: List<OcrBlock>,
    val recognizedAt: Long,
    val width: Int,
    val height: Int,
)
```

## 关键类

- `OcrRepository`（@Singleton）
  - `suspend recognize(bitmap: Bitmap): Result<OcrResult>`
  - `fun warmUp()` — 预热 ML Kit
  - `fun release()` — 释放模型内存

- `OcrPreprocessor`（图片预处理）
  - 深浅色模式检测（自动反转）
  - 文字区域增强（对比度/锐度）

## 当前状态

- [x] 骨架就位
- [ ] 详细实现（v0.4）

## 后续待办

1. ML Kit Chinese Text Recognition 集成
2. 中文/英文混排识别优化
3. 深浅色 UI 模式检测与自动反转
4. 置信度阈值过滤（< 0.6 丢弃）
5. 无障碍 Hybrid 增强（精准模式，v0.5 整合到 4.4 模块）
6. 单元测试：`OcrRepositoryTest`（用合成 Bitmap）

## 风险点

- 华为无 GMS → ML Kit 必须用 bundled 离线模型
- 模型体积 20MB，APK 体积影响显著
- 极小字号 / 表情包 / 颜文字识别率低
