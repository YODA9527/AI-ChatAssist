@file:OptIn(ExperimentalMaterial3Api::class)

package com.chatassist.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chatassist.domain.model.PersonaProfile
import com.chatassist.presentation.navigation.Route

/**
 * 双向人设方案列表（v0.18，v2.2 §13.1）。
 *
 * 列出所有方案，支持：新建、进入编辑、设为当前（手动锁定）、设为默认、删除。
 */
@Composable
fun PersonaListScreen(
    navController: NavController,
    viewModel: PersonaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("人设方案") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { navController.navigate(Route.PersonaEditNew.path) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("新建人设方案")
            }

            when (val s = state) {
                is PersonaUiState.Loading -> Text("加载中…")
                is PersonaUiState.Loaded -> {
                    if (s.profiles.isEmpty()) {
                        Text(
                            "还没有人设方案。点上面的按钮新建一套，或绑定到不同 App 让它自动切换～",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        s.profiles.forEach { profile ->
                            PersonaListItem(
                                profile = profile,
                                isActive = profile.id == s.activePersonaId,
                                onOpen = { navController.navigate(Route.PersonaEdit.path.replace("{id}", profile.id)) },
                                onSetActive = { viewModel.setActive(profile.id) },
                                onSetDefault = { viewModel.setDefault(profile.id) },
                                onDelete = { viewModel.delete(profile.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonaListItem(
    profile: PersonaProfile,
    isActive: Boolean,
    onOpen: () -> Unit,
    onSetActive: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    profile.name.ifBlank { "（未命名方案）" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isActive) {
                        AssistChip(onClick = {}, label = { Text("当前生效") })
                    }
                    if (profile.isDefault) {
                        SuggestionChip(onClick = {}, label = { Text("默认") })
                    }
                }
            }

            val bindInfo = buildList {
                if (profile.boundPackages.isNotEmpty()) add("包名 ${profile.boundPackages.size} 个")
                if (profile.boundKeywords.isNotEmpty()) add("关键词 ${profile.boundKeywords.size} 个")
            }.joinToString(" · ")
            if (bindInfo.isNotBlank()) {
                Text(bindInfo, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSetActive) { Text("设为当前") }
                TextButton(onClick = onSetDefault) { Text("设为默认") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("删除") }
            }
        }
    }
}
