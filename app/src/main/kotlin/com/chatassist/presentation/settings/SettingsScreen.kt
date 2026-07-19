package com.chatassist.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatassist.domain.model.ConversationGoal
import com.chatassist.domain.model.ThemeMode
import com.chatassist.domain.model.TriggerMode

/**
 * 设置页：API / 主题 / 隐私 / 触发 / 人设 入口。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
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
                        ApiConfigCard(
                            config = s.config,
                            onApiKeyChange = viewModel::updateApiKey,
                            onUpdate = viewModel::update,
                            onTestConnection = viewModel::testConnection,
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
                        GoalCard(
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
private fun ApiConfigCard(
    config: com.chatassist.domain.model.AppConfig,
    onApiKeyChange: (String) -> Unit,
    onUpdate: ((com.chatassist.domain.model.AppConfig) -> com.chatassist.domain.model.AppConfig) -> Unit,
    onTestConnection: () -> Unit,
) {
    var apiUrlInput by remember(config.apiUrl) { mutableStateOf(config.apiUrl) }
    var apiKeyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var modelInput by remember(config.model) { mutableStateOf(config.model) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("大模型 API", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = apiUrlInput,
                onValueChange = { new ->
                    apiUrlInput = new
                    onUpdate { it.copy(apiUrl = new) }
                },
                label = { Text("API URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("API Key (加密存储)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { onApiKeyChange(apiKeyInput) },
                enabled = apiKeyInput != config.apiKey,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存 API Key")
            }

            HorizontalDivider()

            OutlinedTextField(
                value = modelInput,
                onValueChange = { new ->
                    modelInput = new
                    onUpdate { it.copy(model = new) }
                },
                label = { Text("模型名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onTestConnection,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("测试连接")
            }
        }
    }
}

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
            Text("主题模式: ${config.themeMode.name}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.values().forEach { mode ->
                    Button(
                        onClick = { onUpdate { it.copy(themeMode = mode) } },
                        enabled = config.themeMode != mode,
                    ) {
                        Text(mode.name)
                    }
                }
            }
        }
    }
}

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
            Text("当前: ${config.triggerMode.name}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriggerMode.values().forEach { mode ->
                    Button(
                        onClick = { onUpdate { it.copy(triggerMode = mode) } },
                        enabled = config.triggerMode != mode,
                    ) {
                        Text(mode.name)
                    }
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

@Composable
private fun GoalCard(
    config: com.chatassist.domain.model.AppConfig,
    onUpdate: ((com.chatassist.domain.model.AppConfig) -> com.chatassist.domain.model.AppConfig) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("对话目标", style = MaterialTheme.typography.titleMedium)
            Text("当前: ${config.goal.name}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConversationGoal.values().forEach { goal ->
                    Button(
                        onClick = { onUpdate { it.copy(goal = goal) } },
                        enabled = config.goal != goal,
                    ) {
                        Text(goal.name)
                    }
                }
            }
        }
    }
}
