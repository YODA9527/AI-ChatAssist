package com.chatassist.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatassist.domain.model.HistoryRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史记录页（模块 09：接入 Room 后真实展示会话与回复）。
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "历史记录", style = MaterialTheme.typography.headlineLarge)
                if (!state.isEmpty) {
                    Button(onClick = { viewModel.clearAll() }) { Text("清空") }
                }
            }

            if (state.privacyMode) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "隐私模式已开启：新生成的回复不会写入历史。",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (state.isEmpty) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("还没有任何记录", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "• 保留策略：${state.maxAgeDays} 天 / ${state.maxCount} 条\n" +
                                "• 在首页点「生成回复」后，对话与建议会自动留在这里",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.records, key = { it.id }) { record ->
                        HistoryCard(fmt, record) { viewModel.delete(record.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    fmt: SimpleDateFormat,
    record: HistoryRecord,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(fmt.format(Date(record.capturedAt)), style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = onDelete) { Text("删除") }
            }
            if (record.targetPackage.isNotBlank()) {
                Text("来源：${record.targetPackage}", style = MaterialTheme.typography.labelSmall)
            }

            // 对话内容：完整展开每条消息（不再只显示计数）
            if (record.messages.isNotEmpty()) {
                Text(
                    "对话（${record.messages.size} 条）",
                    style = MaterialTheme.typography.labelSmall,
                )
                record.messages.forEach { m ->
                    val who = if (m.isSelf) "我" else m.sender.ifBlank { "对方" }
                    Text(
                        "· $who：${m.content}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                    )
                }
            }

            // 回复建议：完整展示所有候选（不再只取第一行做预览）
            if (record.responseText.isNotBlank()) {
                Text("回复建议", style = MaterialTheme.typography.labelSmall)
                Text(
                    record.responseText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 20,
                )
            }

            Text(
                "Token ${record.usage.totalTokens}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
