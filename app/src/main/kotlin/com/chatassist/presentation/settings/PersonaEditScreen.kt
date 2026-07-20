@file:OptIn(ExperimentalMaterial3Api::class)

package com.chatassist.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.chatassist.domain.model.CounterpartPersona
import com.chatassist.domain.model.PersonaProfile
import com.chatassist.domain.model.SelfPersona
import java.util.UUID

/**
 * 双向人设编辑页（v0.18，v2.2 §13.1）。
 *
 * [profileId] 为 null 表示新建（自动生成 UUID），否则编辑指定方案。
 * 双向字段全部可编辑：我的人设（5 项）+ 对方的人设（7 项）+ 自动切换绑定（包名/关键词）+ 默认开关。
 */
@Composable
fun PersonaEditScreen(
    navController: NavController,
    profileId: String?,
    viewModel: PersonaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editingId = remember(profileId) { profileId ?: UUID.randomUUID().toString() }
    val found = (state as? PersonaUiState.Loaded)?.profiles?.firstOrNull { it.id == profileId }

    var name by remember { mutableStateOf("") }
    var selfName by remember { mutableStateOf("") }
    var selfIdentity by remember { mutableStateOf("") }
    var selfStyle by remember { mutableStateOf("") }
    var selfTaboo by remember { mutableStateOf("") }
    var selfSignature by remember { mutableStateOf("") }
    var cpName by remember { mutableStateOf("") }
    var cpIdentity by remember { mutableStateOf("") }
    var cpRelation by remember { mutableStateOf("") }
    var cpStyle by remember { mutableStateOf("") }
    var cpPersonality by remember { mutableStateOf("") }
    var cpDynamic by remember { mutableStateOf("") }
    var cpGoal by remember { mutableStateOf("") }
    var cpLikes by remember { mutableStateOf("") }
    var cpDislikes by remember { mutableStateOf("") }
    var cpPetPeeves by remember { mutableStateOf("") }
    var cpHabits by remember { mutableStateOf("") }
    var boundPackages by remember { mutableStateOf("") }
    var boundKeywords by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }

    // 进入已存在方案时，把存储值同步进本地编辑态（仅首次/数据变化时）
    LaunchedEffect(found) {
        found?.let { p ->
            name = p.name
            selfName = p.self.name
            selfIdentity = p.self.identity
            selfStyle = p.self.speakingStyle
            selfTaboo = p.self.taboo
            selfSignature = p.self.signature
            cpName = p.counterpart.name
            cpIdentity = p.counterpart.identity
            cpRelation = p.counterpart.relationship
            cpStyle = p.counterpart.theirStyle
            cpPersonality = p.counterpart.theirPersonality
            cpDynamic = p.counterpart.dynamic
            cpGoal = p.counterpart.goal
            cpLikes = p.counterpart.likes
            cpDislikes = p.counterpart.dislikes
            cpPetPeeves = p.counterpart.petPeeves
            cpHabits = p.counterpart.habits
            boundPackages = p.boundPackages.joinToString(", ")
            boundKeywords = p.boundKeywords.joinToString(", ")
            isDefault = p.isDefault
        }
    }

    val isEditingExisting = profileId != null

    fun buildProfile(): PersonaProfile = PersonaProfile(
        id = editingId,
        name = name,
        self = SelfPersona(
            name = selfName,
            identity = selfIdentity,
            speakingStyle = selfStyle,
            taboo = selfTaboo,
            signature = selfSignature,
        ),
        counterpart = CounterpartPersona(
            name = cpName,
            identity = cpIdentity,
            relationship = cpRelation,
            theirStyle = cpStyle,
            theirPersonality = cpPersonality,
            dynamic = cpDynamic,
            goal = cpGoal,
            likes = cpLikes,
            dislikes = cpDislikes,
            petPeeves = cpPetPeeves,
            habits = cpHabits,
        ),
        boundPackages = boundPackages.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        boundKeywords = boundKeywords.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        isDefault = isDefault,
        lastUsedAt = found?.lastUsedAt ?: 0L,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditingExisting) "编辑人设" else "新建人设") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isEditingExisting) {
                        IconButton(
                            onClick = {
                                viewModel.delete(editingId)
                                navController.popBackStack()
                            },
                        ) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
                    }
                    IconButton(
                        onClick = {
                            viewModel.save(buildProfile())
                            navController.popBackStack()
                        },
                    ) { Icon(Icons.Filled.Check, contentDescription = "保存") }
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
            PersonaField("方案名称", name) { name = it }

            HorizontalDivider()
            Text("我的人设", style = MaterialTheme.typography.titleSmall)
            PersonaField("名字/昵称", selfName) { selfName = it }
            PersonaField("身份（我是谁）", selfIdentity) { selfIdentity = it }
            PersonaField("说话风格", selfStyle, multiLine = true) { selfStyle = it }
            PersonaField("禁忌/雷区", selfTaboo, multiLine = true) { selfTaboo = it }
            PersonaField("口头禅", selfSignature) { selfSignature = it }

            HorizontalDivider()
            Text("对方的人设", style = MaterialTheme.typography.titleSmall)
            PersonaField("名字/昵称", cpName) { cpName = it }
            PersonaField("身份", cpIdentity) { cpIdentity = it }
            PersonaField("与我的关系", cpRelation) { cpRelation = it }
            PersonaField("说话风格", cpStyle, multiLine = true) { cpStyle = it }
            PersonaField("性格", cpPersonality, multiLine = true) { cpPersonality = it }
            PersonaField("互动动态/现状", cpDynamic, multiLine = true) { cpDynamic = it }
            PersonaField("聊天目标", cpGoal, multiLine = true) { cpGoal = it }
            PersonaField("喜好", cpLikes, multiLine = true) { cpLikes = it }
            PersonaField("讨厌的东西", cpDislikes, multiLine = true) { cpDislikes = it }
            PersonaField("雷区/忌讳", cpPetPeeves, multiLine = true) { cpPetPeeves = it }
            PersonaField("口头习惯", cpHabits, multiLine = true) { cpHabits = it }

            HorizontalDivider()
            Text("自动切换绑定", style = MaterialTheme.typography.titleSmall)
            PersonaField("绑定 App 包名（逗号分隔，如 com.tencent.mm）", boundPackages, multiLine = true) { boundPackages = it }
            PersonaPackagePresetChips(
                boundPackages = boundPackages,
                onToggle = { pkg ->
                    val cur = boundPackages.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    boundPackages = if (pkg in cur) (cur - pkg).joinToString(", ") else (cur + pkg).joinToString(", ")
                },
            )
            PersonaField("绑定关键词（逗号分隔，如 宝贝、老公）", boundKeywords, multiLine = true) { boundKeywords = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("设为默认方案（无绑定命中时兜底）")
                Spacer(Modifier.weight(1f))
                Switch(checked = isDefault, onCheckedChange = { isDefault = it })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonaPackagePresetChips(
    boundPackages: String,
    onToggle: (String) -> Unit,
) {
    val selected = remember(boundPackages) {
        boundPackages.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    Text("常用 App 预设（点击切换）", style = MaterialTheme.typography.titleSmall)
    for ((group, presets) in PERSONA_PACKAGE_PRESET_GROUPS) {
        Text(group, style = MaterialTheme.typography.labelMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (p in presets) {
                FilterChip(
                    selected = p.pkg in selected,
                    onClick = { onToggle(p.pkg) },
                    label = { Text(p.label) },
                )
            }
        }
    }
}

@Composable
private fun PersonaField(
    label: String,
    value: String,
    multiLine: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = !multiLine,
        modifier = Modifier.fillMaxWidth(),
    )
}
