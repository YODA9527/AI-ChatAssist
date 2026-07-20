package com.chatassist.presentation.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 关于页：版本、开源许可、致谢。
 */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "关于",
                style = MaterialTheme.typography.headlineLarge,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("ChatAssist", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "版本 v0.21.0",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "一款本地优先的 Android 聊天辅助 App。\n" +
                              "截图 → OCR → 解析 → 大模型 → 悬浮窗建议回复。\n" +
                              "所有处理 100% 端侧完成，唯一网络出口是你自配的大模型 API。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("技术栈", style = MaterialTheme.typography.titleMedium)
                    Text("Kotlin 2.0.21 · Jetpack Compose · Material3", style = MaterialTheme.typography.bodySmall)
                    Text("Hilt 2.52 · Room · DataStore · Retrofit", style = MaterialTheme.typography.bodySmall)
                    Text("ML Kit Text Recognition v2 (中文+拉丁)", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("开源", style = MaterialTheme.typography.titleMedium)
                    Text("协议：待定", style = MaterialTheme.typography.bodySmall)
                    Text("代码仓库：https://github.com/YODA9527/AI-ChatAssist.git", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
