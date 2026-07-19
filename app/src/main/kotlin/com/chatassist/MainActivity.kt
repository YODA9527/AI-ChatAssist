package com.chatassist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chatassist.presentation.ChatAssistRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * 唯一 Activity。
 *
 * @AndroidEntryPoint 让 Hilt 为本 Activity 生成依赖注入入口。
 * setContent 内只挂载 [ChatAssistRoot]，所有页面路由由 Compose Navigation 接管。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatAssistRoot()
        }
    }
}
