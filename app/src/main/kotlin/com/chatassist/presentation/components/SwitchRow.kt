package com.chatassist.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 设置页标准 Switch 行（v0.22 C3 抽出）。
 *
 * 替代 5+ 处 `Row { Text + Switch }` 模板代码：
 * ```kotlin
 * // 之前
 * Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
 *     Text("动态取色")
 *     Switch(checked = x, onCheckedChange = ...)
 * }
 *
 * // 之后
 * SwitchRow("动态取色", checked = x, onCheckedChange = ...)
 * ```
 */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
