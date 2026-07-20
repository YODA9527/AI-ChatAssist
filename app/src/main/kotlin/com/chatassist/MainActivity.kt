package com.chatassist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chatassist.domain.usecase.KeepAliveManager
import com.chatassist.presentation.ChatAssistRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 唯一 Activity。
 *
 * @AndroidEntryPoint 让 Hilt 为本 Activity 生成依赖注入入口。
 * setContent 内只挂载 [ChatAssistRoot]，所有页面路由由 Compose Navigation 接管。
 *
 * v0.13 修复（FGS 启动时机）：在 [onCreate] 调 [KeepAliveManager.start]，
 * 替代原 v0.12 在 `ChatAssistApp.onCreate` 调的位置。
 *
 * **修复原因**：Android 12+ 限制 cached app (uidState=CEM) 启动 FGS；
 * `ChatAssistApp.onCreate` 跑在进程刚创建时（cached 状态）→ `startForegroundService` 抛
 * `ForegroundServiceStartNotAllowedException`（v0.12 mumu 实测发现）。
 * 改在用户进 App（uidState=FG）时调，startForegroundService 允许通过。
 *
 * **幂等性**：[KeepAliveManager.start] 内部已做幂等（isRunning() 检查），
 * 即便用户从其他 Activity / Task 反复进 MainActivity 也只会启动一次。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var keepAliveManager: KeepAliveManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatAssistRoot()
        }
        // v0.13：移到此处启动后台保活（用户进 App 时 uidState=FG，FGS 允许启动）
        keepAliveManager.start()
    }
}
