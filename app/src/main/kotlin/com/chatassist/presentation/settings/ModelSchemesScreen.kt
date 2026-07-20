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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.chatassist.domain.model.ModelScheme
import com.chatassist.presentation.navigation.Route

/**
 * 模型配置方案列表页（v2.2 §13.4）：切换 / 编辑 / 删除 / 新增。
 */
@Composable
fun ModelSchemesScreen(
    modifier: Modifier = Modifier,
    viewModel: ModelSchemesViewModel = hiltViewModel(),
    navController: NavController,
) {
    val schemes by viewModel.schemes.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ModelScheme?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("模型配置方案", style = MaterialTheme.typography.headlineLarge)
                OutlinedButton(onClick = { navController.navigate(Route.ModelSchemeEditNew.path) }) {
                    Text("新增方案")
                }
            }

            if (schemes.isEmpty()) {
                Text(
                    "还没有任何方案。点右上角「新增方案」创建第一套（每套含文本模型 + 多模态模型）。",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(schemes, key = { it.id }) { scheme ->
                        SchemeRow(
                            scheme = scheme,
                            onActivate = { viewModel.setActive(scheme.id) },
                            onEdit = { navController.navigate(Route.ModelSchemeEdit.path.replace("{id}", scheme.id)) },
                            onDelete = { pendingDelete = scheme },
                        )
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        val s = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                Button(onClick = {
                    viewModel.delete(s.id)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = { OutlinedButton(onClick = { pendingDelete = null }) { Text("取消") } },
            title = { Text("删除方案") },
            text = { Text("确定删除「${s.name}」吗？该方案的密钥会一并清除，不可恢复。") },
        )
    }
}

@Composable
private fun SchemeRow(
    scheme: ModelScheme,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(scheme.name, style = MaterialTheme.typography.titleMedium)
                if (scheme.isActive) {
                    Text("当前生效", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                "文本：${scheme.textModel.model.ifBlank { "未配置" }} @ ${scheme.textModel.apiUrl.ifBlank { "未填接口" }}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "多模态：${scheme.multimodalModel.model.ifBlank { "未配置" }} @ ${scheme.multimodalModel.apiUrl.ifBlank { "未填接口" }}",
                style = MaterialTheme.typography.bodySmall,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!scheme.isActive) {
                    Button(onClick = onActivate, modifier = Modifier.weight(1f)) { Text("设为当前") }
                } else {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("编辑") }
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("删除") }
            }
        }
    }
}
