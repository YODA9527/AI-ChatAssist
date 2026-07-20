package com.chatassist.util

import android.graphics.Bitmap
import android.graphics.Color
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI 模式检测器（v0.10：E 项，§12.2.1）。
 *
 * 通过采样 64x64 缩略图的平均亮度，判断截图处于深色还是浅色 UI 模式。
 * 用于：
 * - v0.10：观测（写入 CaptureResult.uiMode，让上层能感知截图深浅）
 * - v0.11+：动态调 OCR 置信度阈值（深色模式降 0.2）
 *
 * 实现要点：
 * - 64x64 缩略图（4096 像素）足够代表全局亮度，计算量小
 * - Y 分量用 BT.601 加权（0.299R + 0.587G + 0.114B）
 * - 缩略图采样后立即 recycle，不留内存
 *
 * 阈值：< 0.3 视为深色模式；>= 0.3 视为浅色。
 * 边界用例：纯黑/纯白截图会被判定为深色/浅色，符合直觉。
 */
@Singleton
class UiModeDetector @Inject constructor() {

    /** 深色判定阈值（0-1 归一化亮度），< 此值判深色。 */
    var darkThreshold: Double = 0.3

    /**
     * 检测 [bitmap] 所属 UI 模式。
     * 不修改入参 bitmap。
     */
    fun detect(bitmap: Bitmap): UiMode {
        val sample = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        return try {
            var sum = 0L
            for (x in 0 until sample.width) {
                for (y in 0 until sample.height) {
                    val c = sample.getPixel(x, y)
                    val yLum = 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
                    sum += yLum.toLong()
                }
            }
            val avg = sum.toDouble() / (64 * 64) / 255.0
            if (avg < darkThreshold) UiMode.DARK else UiMode.LIGHT
        } finally {
            sample.recycle()
        }
    }
}

/**
 * UI 模式枚举（v0.10：E 项）。
 */
enum class UiMode { LIGHT, DARK }
