package com.chatassist.presentation.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatassist.domain.model.PermissionState
import com.chatassist.presentation.overlay.OverlayState
import com.chatassist.service.CaptureForegroundService

/**
 * 首页：状态 + 权限引导（回复建议在悬浮窗生成，首页不内嵌冗余卡片，v0.19 移除）。
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val perm by viewModel.permissionSummary.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(text = "ChatAssist", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "本地优先的聊天辅助（点悬浮窗「生成」时抓取当前屏幕）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { StatusCard(state.isApiConfigured, state.apiUrl, state.model) }
            if (state.privacyMode) item { PrivacyModeBanner() }
            item { PermissionGuide(viewModel, perm) }
        }
    }
}

@Composable
private fun StatusCard(isConfigured: Boolean, apiUrl: String, model: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConfigured) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isConfigured) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (isConfigured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = if (isConfigured) "接口已配置" else "未配置接口",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (isConfigured) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "接口地址：$apiUrl", style = MaterialTheme.typography.bodySmall)
                Text(text = "模型：$model", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PrivacyModeBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Text(
            text = "🔒 隐私模式已开启 — 不会写入任何历史记录",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun PermissionGuide(viewModel: MainViewModel, perm: PermissionSummary) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val pkg = context.packageName

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshPermissions() }
    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshPermissions() }
    val a11yLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshPermissions() }
    val projLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val intent = Intent(context, CaptureForegroundService::class.java).apply {
                putExtra(CaptureForegroundService.EXTRA_RESULT_CODE, res.resultCode)
                putExtra(CaptureForegroundService.EXTRA_DATA, res.data)
            }
            context.startForegroundService(intent)
        }
        viewModel.refreshPermissions()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "权限引导", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                label = "通知",
                state = perm.notification,
                onClick = { notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
            )
            PermissionRow(
                label = "悬浮窗",
                state = perm.overlay,
                onClick = {
                    overlayLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.fromParts("package", pkg, null),
                        ),
                    )
                },
            )
            PermissionRow(
                label = "无障碍（主通道）",
                state = perm.accessibility,
                onClick = { a11yLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            )
            PermissionRow(
                label = "屏幕录制（兜底）",
                state = perm.projection,
                onClick = {
                    val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    projLauncher.launch(mgr.createScreenCaptureIntent())
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            val overlayOn by viewModel.overlayState.collectAsStateWithLifecycle()
            Button(
                onClick = {
                    if (perm.overlay is PermissionState.Granted) {
                        viewModel.toggleOverlay()
                    } else {
                        overlayLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.fromParts("package", pkg, null),
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        perm.overlay !is PermissionState.Granted -> "去开启悬浮窗权限"
                        overlayOn !is OverlayState.Collapsed -> "隐藏悬浮窗"
                        else -> "显示悬浮窗"
                    },
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, state: PermissionState, onClick: () -> Unit) {
    val granted = state is PermissionState.Granted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Text(text = label, modifier = Modifier.weight(1f))
        Button(onClick = onClick, enabled = !granted) {
            Text(if (granted) "已授权" else "去开启")
        }
    }
}


