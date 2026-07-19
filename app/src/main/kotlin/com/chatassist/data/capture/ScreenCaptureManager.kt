package com.chatassist.data.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.Result
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 截图管线（C 的核心）。
 *
 * 由 [com.chatassist.service.CaptureForegroundService] 注入 MediaProjection 后启动：
 * 创建 ImageReader + VirtualDisplay，按帧截出 Bitmap → 裁剪 → 返回（调用方负责 recycle）。
 * OCR 识别（v0.5）在拿到 Bitmap 之后接 ML Kit；v0.4 仅把截图能力打通。
 *
 * 内存：ImageReader 不可热重建，onTrimMemory 时打点，真正回收在 [stopCapture]。
 */
@Singleton
class ScreenCaptureManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var width = 0
    private var height = 0

    fun startCapture(projection: MediaProjection) {
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics().also { wm.defaultDisplay.getRealMetrics(it) }
        width = metrics.widthPixels
        height = metrics.heightPixels

        mediaProjection = projection
        imageReader = ImageReader.newInstance(
            width, height, PixelFormat.RGBA_8888, 2,
        ).apply {
            virtualDisplay = projection.createVirtualDisplay(
                "ChatAssistCapture",
                width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null,
            )
        }
        Timber.d("ScreenCaptureManager started: ${width}x$height")
    }

    fun stopCapture() {
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { mediaProjection?.stop() }
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        Timber.d("ScreenCaptureManager stopped")
    }

    /** 截取一帧并裁剪（默认全屏）。调用方负责 recycle 返回的 Bitmap。 */
    suspend fun captureRegion(crop: Rect? = null): Result<Bitmap> = runCatching {
        val reader = imageReader ?: throw IllegalStateException("ScreenCaptureManager 未启动")
        val image = reader.acquireLatestImage()
            ?: throw IllegalStateException("未获取到屏幕帧")
        val bitmap = try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val full = Bitmap.createBitmap(
                width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888,
            )
            full.copyPixelsFromBuffer(buffer)
            val src = crop ?: Rect(0, 0, width, height)
            Bitmap.createBitmap(full, src.left, src.top, src.width(), src.height()).also {
                full.recycle()
            }
        } finally {
            image.close()
        }
        bitmap
    }

    fun onMemoryPressure() {
        // ImageReader 不可热重建，仅打点；真正回收在 stopCapture（接 4.12 DegradationManager）
        Timber.d("ScreenCaptureManager: memory pressure signal")
    }
}
