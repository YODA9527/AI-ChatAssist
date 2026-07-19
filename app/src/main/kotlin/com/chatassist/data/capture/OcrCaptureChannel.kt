package com.chatassist.data.capture

import android.graphics.Bitmap
import com.chatassist.domain.capture.CaptureChannel
import com.chatassist.domain.capture.CaptureResult
import com.chatassist.domain.model.CaptureMode
import com.chatassist.domain.model.ChatMessage
import com.chatassist.domain.model.PermissionState
import com.chatassist.data.permission.PermissionManager
import com.chatassist.domain.parser.ChatParser
import com.chatassist.domain.repository.ConfigRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.Result
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCR 兜底通道（C 截图 + ML Kit 中文识别）。v0.5 真实接通。
 *
 * 当策略机切到 [CaptureMode.OCR_ONLY]（无障碍被撤销、MediaProjection 已授权）时，
 * 本通道以轮询方式向 [ScreenCaptureManager] 要一帧 Bitmap，过 ML Kit 中文模型识别，
 * 把文本块映射成 [ChatMessage] 实时流出。截图 Bitmap 用完即 recycle，内存可见可控。
 *
 * 不反向依赖无障碍 Service，也不直接持有 MediaProjection——投影会话由前台服务持有，
 * 本通道只消费 [ScreenCaptureManager] 产出的帧，因此可随策略机自由降级/恢复。
 */
@Singleton
class OcrCaptureChannel @Inject constructor(
    private val screenCaptureManager: ScreenCaptureManager,
    private val permissionManager: PermissionManager,
    private val chatParser: ChatParser,
    private val configRepo: ConfigRepository,
) : CaptureChannel {

    override val mode: CaptureMode = CaptureMode.OCR_ONLY

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build(),
    )

    /** 轮询间隔：截图+OCR 较重，2s 一帧足够 Demo，也避免 ImageReader 被饿死。 */
    private companion object {
        const val POLL_INTERVAL_MS = 2000L

        /** 零风险繁→简映射：仅当 raw 含繁体字才替换，绝不误伤简体原文。 */
        val TRAD_TO_SIMP = mapOf(
            "愛" to "爱", "麥" to "麦", "個" to "个", "來" to "来", "們" to "们",
            "這" to "这", "時" to "时", "會" to "会", "國" to "国", "說" to "说",
            "話" to "话", "東" to "东", "車" to "车", "電" to "电", "腦" to "脑",
            "對" to "对", "開" to "开", "關" to "关", "媽" to "妈", "嗎" to "吗",
            "麼" to "么", "後" to "后", "處" to "处", "點" to "点", "題" to "题",
            "應" to "应", "當" to "当", "實" to "实", "學" to "学", "習" to "习",
            "長" to "长", "兩" to "两", "歲" to "岁", "親" to "亲", "見" to "见",
            "視" to "视", "問" to "问", "體" to "体", "飛" to "飞", "魚" to "鱼",
            "鳥" to "鸟", "馬" to "马", "風" to "风", "雲" to "云", "夢" to "梦",
            "發" to "发", "髮" to "发", "過" to "过", "與" to "与", "從" to "从",
            "妳" to "你", "妳" to "你",
        )
    }

    override val stream: Flow<List<ChatMessage>> = callbackFlow {
        if (permissionManager.projectionState.value !is PermissionState.Granted) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }
        val job: Job = ioScope.launch {
            while (isActive) {
                screenCaptureManager.captureRegion()
                    .onSuccess { bmp ->
                        try {
                            trySend(recognize(bmp))
                        } finally {
                            bmp.recycle()
                        }
                    }
                    .onFailure {
                        Timber.w(it, "OCR 通道截图失败")
                        trySend(emptyList())
                    }
                delay(POLL_INTERVAL_MS)
            }
        }
        awaitClose { job.cancel() }
    }

    override suspend fun captureOnce(): Result<CaptureResult> = runCatching {
        if (permissionManager.projectionState.value !is PermissionState.Granted) {
            return@runCatching CaptureResult(
                mode = CaptureMode.OCR_ONLY,
                messages = emptyList(),
                note = "MediaProjection 未授权：请先在权限引导中开启「屏幕录制」",
            )
        }
        val bmp = screenCaptureManager.captureRegion().getOrElse {
            return@runCatching CaptureResult(
                mode = CaptureMode.OCR_ONLY,
                messages = emptyList(),
                note = "截图失败：${it.message}",
            )
        }
        val messages = try {
            recognize(bmp)
        } finally {
            bmp.recycle()
        }
        val avgConf = if (messages.isEmpty()) {
            null
        } else {
            messages.map { it.confidence }.average().toFloat()
        }
        CaptureResult(
            mode = CaptureMode.OCR_ONLY,
            messages = messages,
            confidence = avgConf,
        )
    }

    /** ML Kit 中文识别（阻塞于 IO 线程；识别完由调用方 recycle Bitmap）。 */
    private suspend fun recognize(bitmap: Bitmap): List<ChatMessage> = withContext(Dispatchers.IO) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = suspendCancellableCoroutine<Text> { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        val screenWidth = bitmap.width
        val autoCorrect = configRepo.snapshot().ocrAutoCorrect
        var idx = 0
        visionText.textBlocks.flatMap { block ->
            block.lines.map { line ->
                val box = line.boundingBox
                val centerX = box?.centerX()?.toInt() ?: -1
                val w = if (box != null) screenWidth else 0
                val raw = line.text ?: ""
                // ocrAutoCorrect：OCR 后本地归一（繁→简为主，零成本、零额外调用）
                val corrected = if (autoCorrect) normalizeOcrText(raw) else raw
                chatParser.parse(corrected, centerX, w, "", CaptureMode.OCR_ONLY, idx++)
            }
        }
    }

    /**
     * OCR 文本本地归一（[AppConfig.ocrAutoCorrect] 开启时调用）。
     *
     * 只做**零风险**的繁→简映射：ML Kit 中文模型常把「爱/麦/个」输出成繁体「愛/麥/個」，
     * 这类是稳定的字形映射，不会误伤正确简体字。复杂的形近字（如「妻」误为「麦」、
     * 「者」误为「考」）依赖语义、无法仅靠字典安全纠正，交由上层 LLM 在生成时结合语境纠错。
     */
    private fun normalizeOcrText(raw: String): String {
        var s = raw
        for ((trad, simp) in TRAD_TO_SIMP) {
            s = s.replace(trad, simp)
        }
        return s
    }

    /** 释放 ML Kit 模型与轮询作用域（进程退出或无障碍恢复后可选调用）。 */
    fun release() {
        runCatching { recognizer.close() }
        ioScope.cancel()
    }
}
