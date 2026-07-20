package com.chatassist.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.navigation.NavController
import com.chatassist.domain.model.ModelConfig
import com.chatassist.domain.model.PRESET_BRANDS
import com.chatassist.domain.model.PresetModel
import com.chatassist.presentation.navigation.Route

/**
 * 模型方案编辑页（v2.2 §13.4）：编辑方案名 + 文本模型 + 多模态模型（各自 apiUrl/密钥/价格）。
 */
@Composable
fun ModelSchemeEditScreen(
    modifier: Modifier = Modifier,
    viewModel: ModelSchemeEditViewModel = hiltViewModel(),
    navController: NavController,
    schemeId: String? = null,
) {
    val scheme by viewModel.scheme.collectAsStateWithLifecycle()
    val textApiKey by viewModel.textApiKey.collectAsStateWithLifecycle()
    val mmApiKey by viewModel.mmApiKey.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(schemeId) { viewModel.load(schemeId) }
    LaunchedEffect(testResult) {
        when (val r = testResult) {
            is SchemeTestResult.Success -> snackbarHostState.showSnackbar("连接成功 (${r.latencyMs}ms)")
            is SchemeTestResult.Failed -> snackbarHostState.showSnackbar("连接失败：${r.message}")
            else -> {}
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (schemeId.isNullOrBlank()) "新增模型方案" else "编辑模型方案", style = MaterialTheme.typography.headlineLarge)
                    OutlinedButton(onClick = { navController.navigateUp() }) { Text("返回") }
                }

                OutlinedTextField(
                    value = scheme.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("方案名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ModelConfigEditor(
                    title = "文本模型（用于回复生成）",
                    config = scheme.textModel,
                    apiKey = textApiKey,
                    onConfigChange = viewModel::updateText,
                    onApiKeyChange = viewModel::setTextApiKey,
                    onTest = viewModel::testTextConnection,
                )

                ModelConfigEditor(
                    title = "多模态模型（预留，当前未接入推理）",
                    config = scheme.multimodalModel,
                    apiKey = mmApiKey,
                    onConfigChange = viewModel::updateMultimodal,
                    onApiKeyChange = viewModel::setMultimodalApiKey,
                    onTest = viewModel::testMultimodalConnection,
                )
            }

            Button(
                onClick = { viewModel.save { navController.popBackStack() } },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text("保存方案") }

            SnackbarHost(hostState = snackbarHostState) { data -> Snackbar(snackbarData = data) }
        }
    }
}

@Composable
private fun ModelConfigEditor(
    title: String,
    config: ModelConfig,
    apiKey: String,
    onConfigChange: (ModelConfig) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTest: () -> Unit,
) {
    var showPreset by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)

            OutlinedButton(onClick = { showPreset = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (config.model.isNotBlank()) "选择预设（当前：${config.model}）" else "从预设目录选择模型")
            }

            OutlinedTextField(
                value = config.apiUrl,
                onValueChange = { onConfigChange(config.copy(apiUrl = it)) },
                label = { Text("接口地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = config.model,
                onValueChange = { onConfigChange(config.copy(model = it)) },
                label = { Text("模型名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("接口密钥（加密存储）") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = if (config.inputPricePerMToken == 0.0) "" else config.inputPricePerMToken.toString(),
                    onValueChange = { onConfigChange(config.copy(inputPricePerMToken = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("输入价/百万") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = if (config.outputPricePerMToken == 0.0) "" else config.outputPricePerMToken.toString(),
                    onValueChange = { onConfigChange(config.copy(outputPricePerMToken = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("输出价/百万") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = if (config.cachedPricePerMToken == 0.0) "" else config.cachedPricePerMToken.toString(),
                    onValueChange = { onConfigChange(config.copy(cachedPricePerMToken = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("缓存价/百万") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("价格单位：元 / 每百万 token（取自内置预设或自定义）", style = MaterialTheme.typography.bodySmall)

            Button(onClick = onTest, modifier = Modifier.fillMaxWidth()) { Text("测试连接") }
        }
    }

    if (showPreset) {
        var expanded by remember { mutableStateOf(setOf<String>()) }
        AlertDialog(
            onDismissRequest = { showPreset = false },
            confirmButton = {},
            title = { Text("选择预设模型（按品牌）") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (brand in PRESET_BRANDS) {
                        item(key = "brand_${brand.brandKey}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expanded = if (expanded.contains(brand.brandKey)) {
                                            expanded - brand.brandKey
                                        } else {
                                            expanded + brand.brandKey
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(brand.brand, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text(if (expanded.contains(brand.brandKey)) "▾" else "▸")
                            }
                        }
                        if (expanded.contains(brand.brandKey)) {
                            items(brand.models, key = { "${brand.brandKey}_${it.model}" }) { p ->
                                OutlinedButton(
                                    onClick = {
                                        onConfigChange(
                                            config.copy(
                                                model = p.model,
                                                apiUrl = p.defaultApiUrl,
                                                apiFormat = p.apiFormat,
                                                inputPricePerMToken = p.inputPricePerMToken,
                                                outputPricePerMToken = p.outputPricePerMToken,
                                                cachedPricePerMToken = p.cachedPricePerMToken,
                                            ),
                                        )
                                        showPreset = false
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 2.dp),
                                ) {
                                    Text("${p.model}${if (p.multimodal) "（多模态）" else ""}")
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}
