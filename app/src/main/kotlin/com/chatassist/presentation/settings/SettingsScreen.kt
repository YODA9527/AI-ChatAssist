package com.chatassist.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.chatassist.domain.model.ConversationStyle
import com.chatassist.domain.model.ThemeMode
import com.chatassist.domain.model.TriggerMode
import com.chatassist.domain.model.displayName
import com.chatassist.presentation.navigation.Route

/**
 * 设置页：API / 主题 / 隐私 / 触发 / 人设 入口。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: NavController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 测试连接结果 Snackbar
    LaunchedEffect(testResult) {
        when (val r = testResult) {
            is TestResult.Success -> snackbarHostState.showSnackbar("连接成功 (${r.latencyMs}ms)")
            is TestResult.Failed -> snackbarHostState.showSnackbar("连接失败：${r.message}")
            else -> {}
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineLarge,
                )

                when (val s = state) {
                    is SettingsUiState.Loading -> Text("加载中…")
                    is SettingsUiState.Loaded -> {
                        ModelSchemeEntryCard(
                            config = s.config,
                            onOpen = { navController.navigate(Route.ModelSchemes.path) },
                        )
                        ThemeCard(
                            config = s.config,
                            onUpdate = viewModel::update,
                        )
                        TriggerCard(
                            config = s.config,
                            onUpdate = viewModel::update,
                        )
                        OverlayCard(
                            config = s.config,
                            onUpdate = viewModel::update,
                        )
                        PrivacyCard(
                            config = s.config,
                            onUpdate = viewModel::update,
                        )
                        StyleCard(
                            config = s.config,
                            onUpdate = viewModel::update,
                        )
                        PersonaCard(
                            config = s.config,
                            onOpen = { navController.navigate(Route.PersonaList.path) },
                        )
                        BudgetCard(
                            config = s.config,
                            onUpdate = viewModel::update,
                        )
                    }
                }
            }

            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    }
}

@Composable
private fun ModelSchemeEntryCard(
    config: com.chatassist.domain.model.AppConfig,
    onOpen: () -> Unit,
) {
    val active = config.activeScheme()
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("模型配置方案", style = MaterialTheme.typography.titleMedium)
                Text(
                    "当前生效：${active.name}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "文本：${active.textModel.model.ifBlank { "未配置" }}　多模态：${active.multimodalModel.model.ifBlank { "未配置" }}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "管理 >",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeCard(
    config: com.chatassist.domain.model.AppConfig,
    onUpdate: ((com.chatassist.domain.model.AppConfig) -> com.chatassist.domain.model.AppConfig) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("主题", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("动态取色（Android 12+）")
                Switch(
                    checked = config.dynamicColor,
                    onCheckedChange = { v -> onUpdate { it.copy(dynamicColor = v) } },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("高对比度")
                Switch(
                    checked = config.highContrast,
                    onCheckedChange = { v -> onUpdate { it.copy(highContrast = v) } },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("减少动效")
                Switch(
                    checked = config.reduceMotion,
                    onCheckedChange = { v -> onUpdate { it.copy(reduceMotion = v) } },
                )
            }

            Spacer(Modifier.height(4.dp))
            Text("主题模式", style = MaterialTheme.typography.bodySmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.values().forEach { mode ->
                    FilterChip(
                        selected = config.themeMode == mode,
                        onClick = { onUpdate { it.copy(themeMode = mode) } },
                        label = { Text(mode.displayName) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriggerCard(
    config: com.chatassist.domain.model.AppConfig,
    onUpdate: ((com.chatassist.domain.model.AppConfig) -> com.chatassist.domain.model.AppConfig) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("触发方式", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TriggerMode.values().forEach { mode ->
                    FilterChip(
                        selected = config.triggerMode == mode,
                        onClick = { onUpdate { it.copy(triggerMode = mode) } },
                        label = { Text(mode.displayName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayCard(
    config: com.chatassist.domain.model.AppConfig,
    onUpdate: ((com.chatassist.domain.model.AppConfig) -> com.chatassist.domain.model.AppConfig) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("悬浮窗体验（v0.8）", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("智能贴边吸附")
                    Text("拖动松手贴边 + 静止 3 秒半透明", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = config.smartSnap,
                    onCheckedChange = { v -> onUpdate { it.copy(smartSnap = v) } },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("打字机效果")
                    Text("流式回复逐字显示", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = config.typewriter,
                    onCheckedChange = { v -> onUpdate { it.copy(typewriter = v) } },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自动粘贴")
                    Text("点回复后自动粘贴（需无障碍/精准模式）", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = config.autoPaste,
                    onCheckedChange = { v -> onUpdate { it.copy(autoPaste = v) } },
                )
            }
        }
    }
}

@Composable
private fun PrivacyCard(
    config: com.chatassist.domain.model.AppConfig,
    onUpdate: ((com.chatassist.domain.model.AppConfig) -> com.chatassist.domain.model.AppConfig) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("隐私", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("隐私模式")
                    Text(
                        "开启后不写入历史记录，截图立即清零",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = config.privacyMode,
                    onCheckedChange = { v -> onUpdate { it.copy(privacyMode = v) } },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleCard(
    config: com.chatassist.domain.model.AppConfig,
    onUpdate: ((com.chatassist.domain.model.AppConfig) -> com.chatassist.domain.model.AppConfig) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("对话风格", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConversationStyle.values().forEach { style ->
                    FilterChip(
                        selected = config.style == style,
                        onClick = { onUpdate { it.copy(style = style) } },
                        label = { Text(style.displayName) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetCard(
    config: com.chatassist.domain.model.AppConfig,
    onUpdate: ((com.chatassist.domain.model.AppConfig) -> com.chatassist.domain.model.AppConfig) -> Unit,
) {
    var budgetInput by remember(config.budget.monthlyBudgetYuan) {
        mutableStateOf(if (config.budget.monthlyBudgetYuan > 0) config.budget.monthlyBudgetYuan.toString() else "")
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("月度预算（v2.2 计费）", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = budgetInput,
                onValueChange = { new ->
                    budgetInput = new
                    val v = new.toDoubleOrNull() ?: 0.0
                    onUpdate { it.copy(budget = it.budget.copy(monthlyBudgetYuan = v)) }
                },
                label = { Text("月度预算（元，0 = 不设）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("达 80% 提醒")
                Switch(
                    checked = config.budget.alertAt80Percent,
                    onCheckedChange = { v -> onUpdate { it.copy(budget = it.budget.copy(alertAt80Percent = v)) } },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("达 100% 提醒")
                Switch(
                    checked = config.budget.alertAt100Percent,
                    onCheckedChange = { v -> onUpdate { it.copy(budget = it.budget.copy(alertAt100Percent = v)) } },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("超额拦截生成")
                    Text("超预算后停止生成（必须用户可见）", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = config.budget.blockAtExceed,
                    onCheckedChange = { v -> onUpdate { it.copy(budget = it.budget.copy(blockAtExceed = v)) } },
                )
            }
        }
    }
}

@Composable
private fun PersonaCard(
    config: com.chatassist.domain.model.AppConfig,
    onOpen: () -> Unit,
) {
    val active = config.personas.firstOrNull { it.id == config.activePersonaId }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("人设方案", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (active != null) "当前生效：${active.name}" else "默认（旧版单人设）",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "管理 >",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
