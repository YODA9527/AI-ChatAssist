package com.chatassist.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 设置页统一 Card 容器（v0.22 C3 抽出）。
 *
 * 设计要点：
 * - 标准 Card 外壳（自动跟主题色），避免每个屏自己写 padding/spacedBy
 * - 标题与内容统一间距：title 用 `titleMedium`、内容区 8dp 间距
 * - 纯 stateless Composable，无 remember / LaunchedEffect
 *
 * 用途：所有设置相关页（Settings / ModelSchemeEdit / PersonaEdit）都应使用此 Card，
 * 减少每屏 5-8 行 padding/spacedBy 模板代码。
 *
 * 用法：
 * ```kotlin
 * SettingsSectionCard(title = "主题") {
 *     SwitchRow("动态取色", checked, onCheckedChange)
 *     // ...
 * }
 * ```
 */
@Composable
fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
