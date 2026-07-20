package com.chatassist.presentation.overlay

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import android.graphics.BlurMaskFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.chatassist.domain.model.AppConfig
import com.chatassist.domain.model.TriggerMode
import com.chatassist.presentation.theme.ChatAssistTheme
import com.chatassist.domain.repository.ConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 圆角面板（阴影 + 背景 + 描边，全部在 draw 阶段绘制，绝不创建离屏图层）。
 *
 * 关键：Compose 的 Modifier.shadow / clip / background(color, shape) 在
 * TYPE_APPLICATION_OVERLAY + PixelFormat.TRANSLUCENT 透明窗口上会触发不透明离屏图层，
 * 把圆角矩形之外区域填成灰底——这就是「四角灰残留」的真凶。
 * 之前只修了 dropShadow 却漏了 background(shape) 的 clip，灰圈一直都在。
 * 此实现完全在 draw 阶段用 canvas 画圆角矩形，四角始终透明，彻底消除灰圈。
 */
private fun Modifier.roundRectSurface(
    radiusDp: androidx.compose.ui.unit.Dp,
    fill: Color? = null,
    brush: Brush? = null,
    borderColor: Color? = null,
    borderWidthDp: androidx.compose.ui.unit.Dp = 0.dp,
    shadowColor: Color? = null,
    shadowElevationDp: androidx.compose.ui.unit.Dp = 0.dp,
): Modifier = this.then(
    drawWithContent {
        val r = radiusDp.toPx()
        // 1) 外阴影：多层向外扩散圆角矩形堆叠，零 offscreen layer。
        if (shadowColor != null) {
            val base = shadowElevationDp.toPx()
            val layers = 5
            repeat(layers) { i ->
                val t = (i + 1).toFloat() / layers
                val grow = base * t
                val dy = base * t * 0.35f
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            -grow,
                            -grow + dy,
                            size.width + grow,
                            size.height + grow + dy,
                            CornerRadius(r, r),
                        ),
                    )
                }
                drawPath(path, color = shadowColor.copy(alpha = shadowColor.alpha * (1f - t * 0.7f)))
            }
        }
        // 2) 背景圆角矩形：直接 draw 阶段填充，不创建 shape/clip 图层，四角透明。
        val path = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
        }
        when {
            brush != null -> drawPath(path, brush = brush)
            fill != null -> drawPath(path, color = fill)
        }
        // 3) 描边（同样 draw 阶段）。
        if (borderColor != null && borderWidthDp > 0.dp) {
            drawPath(path, color = borderColor, style = Stroke(width = borderWidthDp.toPx()))
        }
        drawContent()
    },
)

/**
 * 悬浮窗控制器（v0.8 重构）。
 *
 * 通过 WindowManager 叠加一个 ComposeView 主面板，并可选叠加一个原生 View 边缘触发条：
 * - 收起态（Collapsed）：常驻一个「生成」按钮（无二级触发球/展开面板）。
 * - 展开态（Showing）：点击生成后自动展开回复面板，每条建议点击 = 填充输入框，右侧图标 = 复制。
 * - 连续复制气泡（HistoryBubble）：复制一条后收起为「下一条」小气泡，连续复制多条（12.1.3）。
 *
 * v0.8 体验增强：
 * - 智能贴边半透明吸附（12.1.2）：拖动松手吸附最近边缘 + 静止 3s 后半透明。
 * - 打字机效果：流式阶段逐字显示累积文本。
 * - 边缘触发条（12.1.1）：triggerMode = EDGE_BAR 时改用 16dp 窄条，双击触发识别。
 *
 * 状态只由内部 [OverlayState] 驱动，回复内容由
 * [com.chatassist.domain.usecase.ReplySuggestionUseCase] 经 ViewModel 转发进来。
 */
@Singleton
class OverlayController @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val ctx: Context,
    private val configRepo: ConfigRepository,
) {
    private val wm = ctx.getSystemService(WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var cfg: AppConfig = AppConfig()

    private var view: ComposeView? = null
    private var overlayOwner: OverlayOwner? = null
    private var mainLp: WindowManager.LayoutParams? = null

    private var edgeBar: View? = null
    private var edgeBarLp: WindowManager.LayoutParams? = null

    private var currentSuggestions: List<String> = emptyList()
    private val dimHandler = Handler(Looper.getMainLooper())
    private var dimRunnable: Runnable? = null

    init {
        scope.launch { configRepo.observeAll().collect { onConfigChanged(it) } }
    }

    /**
     * 配置变更回调（v0.17 增强）。
     *
     * **关键职责**：triggerMode 变化时，重新对齐已挂载的悬浮窗视图。
     * 修复哥哥下旨的"切换触发方式再返回主页闪退"附带 UX 问题：
     *  - 之前 [show] 只在 view/edgeBar 为 null 时才挂载，triggerMode 切换后
     *    已挂载的旧视图**不会**自动换成新模式（EDGE_BAR → MANUAL 时 edgeBar 残留）
     *  - 现在 triggerMode 变化时，hide() + show() 重新挂载，确保视图模式与 cfg 一致
     *
     * 注意：show() 内部仍会 `canDrawOverlays` 检查 + 重复挂载守卫，这里只处理
     * "已有视图 + 模式不匹配" 这一种 case。
     */
    private fun onConfigChanged(newCfg: AppConfig) {
        val oldMode = cfg.triggerMode
        cfg = newCfg
        if (oldMode != newCfg.triggerMode && isShowing()) {
            // 触发方式切换：先卸后挂，确保视图模式一致
            hide()
            show()
        }
    }

    private class OverlayOwner : SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

        fun create() {
            savedStateController.performAttach()
            savedStateController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
        fun start() = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        fun resume() = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun destroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            savedStateController.performSave(android.os.Bundle())
        }
    }

    /** 复制回调：由 ViewModel 注入（访问系统剪贴板），携带该条在列表中的下标。 */
    var copyHandler: (String, Int) -> Unit = { _, _ -> }

    /** 生成回复回调：由 ViewModel 注入（触发按需捕获 + 模型调用）。 */
    var generateHandler: () -> Unit = {}

    /** 填充输入框回调：由 ViewModel 注入（复制兜底 + 无障碍 setText）。 */
    var fillHandler: (String) -> Unit = {}

    private val _state = kotlinx.coroutines.flow.MutableStateFlow<OverlayState>(OverlayState.Collapsed)
    val state: kotlinx.coroutines.flow.StateFlow<OverlayState> = _state.asStateFlow()

    /** 悬浮窗权限是否就绪。 */
    fun canShow(): Boolean = Settings.canDrawOverlays(ctx)

    /** 当前是否正在显示（主面板或边缘条任一已挂载）。 */
    fun isShowing(): Boolean = view != null || edgeBar != null

    private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()
    private fun screenWidth(): Int = ctx.resources.displayMetrics.widthPixels
    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    /** 挂载悬浮窗（权限不足时静默返回；按 triggerMode 决定主面板 / 边缘触发条）。 */
    fun show() {
        if (!canShow()) return
        if (view != null || edgeBar != null) return
        if (cfg.triggerMode == TriggerMode.EDGE_BAR) attachEdgeBar() else attachMainPanel()
    }

    private fun attachMainPanel() {
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth() - dp(240)
            y = 140
        }
        mainLp = lp
        val owner = OverlayOwner().apply { create() }
        overlayOwner = owner
        view = ComposeView(ctx).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                ChatAssistTheme {
                    OverlayContent(
                        state = _state,
                        onCopy = { text, idx -> requestCopy(text, idx) },
                        onFill = { fillHandler(it) },
                        onToggle = { toggle() },
                        onGenerate = { generateHandler() },
                    )
                }
            }
        }
        wm.addView(view, lp)
        owner.start(); owner.resume()
    }

    private fun attachEdgeBar() {
        val v = View(ctx).apply {
            setBackgroundColor(0x33000000)
            alpha = 0.3f
        }
        val lp = WindowManager.LayoutParams(
            dp(16),
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.END }
        edgeBarLp = lp
        val detector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                showMainPanelIfNeeded()
                generateHandler()
                return true
            }
        })
        var downY = 0f
        v.setOnTouchListener { _, e ->
            detector.onTouchEvent(e)
            when (e.action) {
                MotionEvent.ACTION_DOWN -> downY = e.rawY
                MotionEvent.ACTION_MOVE -> {
                    val dy = (e.rawY - downY).toInt()
                    if (kotlin.math.abs(dy) > 8) {
                        lp.y += dy
                        wm.updateViewLayout(v, lp)
                        downY = e.rawY
                    }
                }
            }
            true
        }
        edgeBar = v
        wm.addView(v, lp)
    }

    private fun showMainPanelIfNeeded() {
        if (view == null) attachMainPanel()
    }

    /** 卸载悬浮窗（主面板 + 边缘触发条）。 */
    fun hide() {
        overlayOwner?.destroy()
        overlayOwner = null
        view?.let { wm.removeView(it) }
        view = null
        mainLp = null
        cancelDimTimer()
        edgeBar?.let { wm.removeView(it) }
        edgeBar = null
        edgeBarLp = null
    }

    /** 收起/展开切换（仅用于从展开态收起回生成按钮）。 */
    fun toggle() {
        if (view == null) {
            showMainPanelIfNeeded()
            _state.value = OverlayState.Showing(emptyList(), false)
            return
        }
        _state.value = when (val s = _state.value) {
            is OverlayState.Collapsed -> OverlayState.Showing(emptyList(), false)
            else -> OverlayState.Collapsed
        }
    }

    fun showStreaming(text: String = "") {
        val display = if (cfg.typewriter) listOf(text) else emptyList()
        _state.value = OverlayState.Showing(display, true)
    }

    fun showSuggestions(list: List<String>) {
        currentSuggestions = list
        _state.value = OverlayState.Showing(list, false)
    }

    fun showError(msg: String) {
        _state.value = OverlayState.Error(msg)
    }

    /** 复制某条：复制 + 进入连续复制气泡（还有下一条时）。 */
    private fun requestCopy(text: String, index: Int) {
        copyHandler(text, index)
        _state.value = if (index + 1 < currentSuggestions.size) {
            OverlayState.HistoryBubble(currentSuggestions, index)
        } else {
            OverlayState.Collapsed
        }
    }

    /** 连续复制气泡：复制下一条。 */
    private fun requestCopyNext(bubble: OverlayState.HistoryBubble) {
        val nextIdx = bubble.currentIndex + 1
        if (nextIdx >= bubble.suggestions.size) {
            _state.value = OverlayState.Collapsed
            return
        }
        copyHandler(bubble.suggestions[nextIdx], nextIdx)
        _state.value = if (nextIdx + 1 < bubble.suggestions.size) {
            OverlayState.HistoryBubble(bubble.suggestions, nextIdx)
        } else {
            OverlayState.Collapsed
        }
    }

    /** 轻量提示（填充/复制反馈用）。 */
    fun toast(msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    // ======================== 拖动 + 智能贴边 ========================

    private fun snapToEdge(releaseX: Float) {
        val sw = screenWidth()
        val targetX = if (releaseX < sw / 2) 0 else sw - (view?.width ?: dp(240))
        view?.animate()?.x(targetX.toFloat())?.setDuration(250)
            ?.withEndAction {
                val lp = mainLp ?: return@withEndAction
                lp.x = targetX
                wm.updateViewLayout(view, lp)
                view?.translationX = 0f
            }?.start()
        scheduleDim()
    }

    private fun scheduleDim() {
        cancelDimTimer()
        if (!cfg.smartSnap) return
        dimRunnable = Runnable { if (view != null) view?.animate()?.alpha(0.4f)?.setDuration(300)?.start() }
        dimHandler.postDelayed(dimRunnable!!, 3000)
    }

    private fun cancelDimTimer() {
        dimRunnable?.let { dimHandler.removeCallbacks(it) }
        dimRunnable = null
    }

    @Composable
    private fun OverlayContent(
        state: kotlinx.coroutines.flow.StateFlow<OverlayState>,
        onCopy: (String, Int) -> Unit,
        onFill: (String) -> Unit,
        onToggle: () -> Unit,
        onGenerate: () -> Unit,
    ) {
        val s by state.collectAsState()
        val isDark = isSystemInDarkTheme()
        val bgColor = if (isDark) {
            Color(0xFF1E1F24).copy(alpha = 0.92f)
        } else {
            Color.White.copy(alpha = 0.9f)
        }
        val borderColor = if (isDark) {
            Color.White.copy(alpha = 0.16f)
        } else {
            Color.Black.copy(alpha = 0.08f)
        }
        // v0.9.2 哥哥定调"只留白色按钮"：去除外阴影（多层圆角矩形堆叠导致同心灰波纹），
        // 主壳与按钮一律不画 shadow，背景由 fill/border 承担即可。
        Box(
            modifier = Modifier
                .padding(14.dp)
                .roundRectSurface(
                    radiusDp = 20.dp,
                    fill = bgColor,
                    borderColor = borderColor,
                    borderWidthDp = 0.75.dp,
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            cancelDimTimer()
                            view?.alpha = 1f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val lp = mainLp ?: return@detectDragGestures
                            lp.x += dragAmount.x.toInt()
                            lp.y += dragAmount.y.toInt()
                            lp.gravity = Gravity.TOP or Gravity.START
                            wm.updateViewLayout(view, lp)
                        },
                        onDragEnd = {
                            val lp = mainLp ?: return@detectDragGestures
                            snapToEdge(lp.x.toFloat())
                        },
                    )
                },
        ) {
            Box(
                Modifier.roundRectSurface(
                    radiusDp = 20.dp,
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White.copy(alpha = if (isDark) 0.10f else 0.5f),
                            0.4f to Color.Transparent,
                            1.0f to MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.08f else 0.05f),
                        ),
                    ),
                ),
            ) {
                when (val it = s) {
                    is OverlayState.Collapsed -> GenerateButton(onGenerate)
                    is OverlayState.Showing -> SuggestionPanel(
                        suggestions = it.suggestions,
                        isStreaming = it.isStreaming,
                        onCopy = onCopy,
                        onFill = onFill,
                        onToggle = onToggle,
                        onGenerate = onGenerate,
                    )
                    is OverlayState.Error -> ErrorPanel(it.message, onToggle, onGenerate)
                    is OverlayState.Expanding -> SuggestionPanel(
                        emptyList(), true, onCopy, onFill, onToggle, onGenerate,
                    )
                    is OverlayState.HistoryBubble -> CopyBubble(
                        total = it.suggestions.size,
                        currentIndex = it.currentIndex,
                        onNext = { requestCopyNext(it) },
                        onClose = { _state.value = OverlayState.Collapsed },
                    )
                }
            }
        }
    }

    /** 常驻「生成」按钮（无二级触发球）。 */
    @Composable
    private fun GenerateButton(onGenerate: () -> Unit) {
        Box(modifier = Modifier.clickable(onClick = onGenerate).padding(8.dp)) {
            Text("生成", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }

    @Composable
    private fun SuggestionPanel(
        suggestions: List<String>,
        isStreaming: Boolean,
        onCopy: (String, Int) -> Unit,
        onFill: (String) -> Unit,
        onToggle: () -> Unit,
        onGenerate: () -> Unit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isStreaming) "生成中…" else "回复建议",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.clickable(enabled = !isStreaming, onClick = onGenerate).padding(4.dp)) {
                    Text("生成", color = MaterialTheme.colorScheme.primary)
                }
                Box(modifier = Modifier.clickable(onClick = onToggle).padding(4.dp)) {
                    Text("收起", color = MaterialTheme.colorScheme.primary)
                }
            }
            if (isStreaming) {
                if (suggestions.isEmpty()) {
                    Text("（等待模型返回…）", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(suggestions.first(), style = MaterialTheme.typography.bodyMedium)
                }
            } else if (suggestions.isEmpty()) {
                Text(
                    "还没有建议～点「生成」，让 AI 根据当前对话给你 3 条回复。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!isStreaming) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(suggestions) { idx, item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .roundRectSurface(
                                    radiusDp = 8.dp,
                                    fill = MaterialTheme.colorScheme.primaryContainer,
                                )
                                .clickable { onFill(item) }
                                .padding(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    item,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                IconButton(onClick = { onCopy(item, idx) }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "复制")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CopyBubble(
        total: Int,
        currentIndex: Int,
        onNext: () -> Unit,
        onClose: () -> Unit,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.clickable(onClick = onNext).padding(4.dp)) {
                Text("← 下一条 (${currentIndex + 2}/$total)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Box(modifier = Modifier.clickable(onClick = onClose).padding(4.dp)) {
                Text("✕", color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    @Composable
    private fun ErrorPanel(message: String, onToggle: () -> Unit, onGenerate: () -> Unit) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("出错了", style = MaterialTheme.typography.titleSmall)
            Text(message, style = MaterialTheme.typography.bodySmall)
            Row {
                Box(modifier = Modifier.clickable(onClick = onGenerate).padding(4.dp)) {
                    Text("重试", color = MaterialTheme.colorScheme.primary)
                }
                Box(modifier = Modifier.clickable(onClick = onToggle).padding(4.dp)) {
                    Text("收起", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
