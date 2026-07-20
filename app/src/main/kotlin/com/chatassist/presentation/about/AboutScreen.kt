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
import com.chatassist.BuildConfig

/**
 * 关于页：版本、开源许可、致谢。
 *
 * v0.22 (B6)：版本号 / commit / 构建时间 / 构建类型 全部从 BuildConfig 读。
 * 排错时一眼知道"装的是哪个版本哪个 commit 哪个时间"——这才是真能上生产的样子。
 */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    // BuildConfig.BUILD_TIME 是 ISO8601 字符串（构建期已 format 好），直接展示即可
    val buildTime = BuildConfig.BUILD_TIME
    val versionLine = buildString {
        append(BuildConfig.VERSION_NAME)
        if (BuildConfig.VERSION_SUFFIX.isNotEmpty()) append("-").append(BuildConfig.VERSION_SUFFIX)
    }

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
                        text = "版本 $versionLine",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Commit: ${BuildConfig.GIT_SHA}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "构建时间: $buildTime",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "versionCode: ${BuildConfig.VERSION_CODE}",
                        style = MaterialTheme.typography.bodySmall,
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
                    Text("Kotlin $KOTLIN_VERSION_DISPLAY · Jetpack Compose · Material3", style = MaterialTheme.typography.bodySmall)
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
                    Text("协议：Apache-2.0", style = MaterialTheme.typography.bodySmall)
                    Text("代码仓库：https://github.com/YODA9527/AI-ChatAssist.git", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private const val KOTLIN_VERSION_DISPLAY = "2.0.21"
