package com.chatassist.presentation.usage

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chatassist.domain.model.BudgetStatus

/**
 * 用量统计页（v2.2 §13.3.8）。
 */
@Composable
fun UsageScreen(
    modifier: Modifier = Modifier,
    viewModel: UsageViewModel = hiltViewModel(),
    navController: NavController? = null,
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val byModel by viewModel.byModel.collectAsStateWithLifecycle()
    val byApp by viewModel.byApp.collectAsStateWithLifecycle()
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    val progress by viewModel.budgetProgress.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val exportPath by viewModel.exportPath.collectAsStateWithLifecycle()

    var showClearConfirm by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // 整页可滚动：内容超高时底部操作按钮不再被裁成窄条
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("用量统计", style = MaterialTheme.typography.headlineLarge)

            // 预算进度
            BudgetCard(progress, budget)

            // 时间范围切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UsageRange.values().forEach { r ->
                    FilterChip(
                        selected = range == r,
                        onClick = { viewModel.setRange(r) },
                        label = { Text(r.label) },
                    )
                }
            }

            // 今日/范围概览
            TodaySummaryCard(summary)

            // 按模型
            SectionTitle("按模型")
            if (byModel.isEmpty()) {
                EmptyHint("该范围内暂无用量")
            } else {
                byModel.forEach { ModelUsageRow(it) }
            }

            // 按 App
            SectionTitle("按 App")
            if (byApp.isEmpty()) {
                EmptyHint("该范围内暂无用量")
            } else {
                byApp.forEach { AppUsageRow(it) }
            }

            // 隐私说明
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "用量数据仅保存在本机，不上传任何服务器；记录中不含对话内容，只有 token 数与费用。",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // 操作按钮
            OutlinedButton(
                onClick = { viewModel.exportUsage() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导出用量") }
            if (exportPath != null) {
                Text("已导出：$exportPath", style = MaterialTheme.typography.bodySmall)
                viewModel.clearExportResult()
            }
            OutlinedButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("清空用量记录") }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearAll()
                    showClearConfirm = false
                }) { Text("清空") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) { Text("取消") }
            },
            title = { Text("清空用量记录") },
            text = { Text("确定要删除全部用量记录吗？此操作不可恢复（不影响历史对话）。") },
        )
    }
}

@Composable
private fun BudgetCard(progress: BudgetProgress, budget: com.chatassist.domain.model.BudgetConfig) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("月度预算", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (budget.monthlyBudgetYuan <= 0) "未设置" else "¥%.2f".format(budget.monthlyBudgetYuan),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (budget.monthlyBudgetYuan > 0) {
                LinearProgressIndicator(
                    progress = { progress.ratio.coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "已用 ¥%.4f / 预算 ¥%.2f（%.0f%%）".format(
                        progress.usedYuan,
                        progress.budgetYuan,
                        progress.ratio.coerceAtMost(9.99) * 100,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                val statusText = when (progress.status) {
                    BudgetStatus.OK -> "预算内"
                    BudgetStatus.NEAR_LIMIT -> "已达 80%，注意控制"
                    BudgetStatus.EXCEEDED_WARN -> "已超预算，建议检查"
                    BudgetStatus.EXCEEDED_BLOCK -> "已超预算且开启拦截，生成已停止"
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (progress.status) {
                        BudgetStatus.OK -> MaterialTheme.colorScheme.onSurfaceVariant
                        BudgetStatus.NEAR_LIMIT -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(summary: com.chatassist.domain.model.UsageSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("概览", style = MaterialTheme.typography.titleMedium)
            val costYuan = summary.costMicro / 1_000_000.0
            Text(
                "调用 ${summary.callCount} 次（成功 ${summary.successCount} / 失败 ${summary.failCount}）",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "输入 ${summary.promptTokens} · 输出 ${summary.completionTokens} · 总 ${summary.totalTokens} token",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("费用 ¥%.4f · 平均每次 ¥%.4f".format(costYuan, summary.avgCostYuan), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ModelUsageRow(it: com.chatassist.domain.model.UsageByModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("${it.provider} / ${it.model}", style = MaterialTheme.typography.bodyMedium)
                Text("${it.callCount} 次 · ${it.totalTokens} token", style = MaterialTheme.typography.bodySmall)
            }
            Text("¥%.4f".format(it.costMicro / 1_000_000.0), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AppUsageRow(it: com.chatassist.domain.model.UsageByApp) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(it.targetPackage.ifBlank { "未知应用" }, style = MaterialTheme.typography.bodyMedium)
                Text("${it.callCount} 次 · ${it.totalTokens} token", style = MaterialTheme.typography.bodySmall)
            }
            Text("¥%.4f".format(it.costMicro / 1_000_000.0), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
