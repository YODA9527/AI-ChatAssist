package com.chatassist.presentation.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.chatassist.domain.model.ThemeMode
import org.junit.Rule
import org.junit.Test

/**
 * Theme 截图测试（v0.14 全量测试，P1：Theme 视觉回归）。
 *
 * 用 Paparazzi（JVM 跑，无模拟器依赖）截图关键组件深/浅主题：
 * - 浅色主题：Material3 默认 Card + Button + OutlinedButton + 圆角 token
 * - 深色主题：同上
 * - 圆形头像（CircleShape 渲染）
 *
 * Paparazzi 不支持 Compose 动画，所以避免动效组件。
 * 截图为视觉回归基线（修改 Theme 后肉眼比一眼就能发现变化）。
 */
class ThemeSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun `浅色主题主屏预览`() {
        paparazzi.snapshot {
            ChatAssistTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DemoScreen()
                }
            }
        }
    }

    @Test
    fun `深色主题主屏预览`() {
        paparazzi.snapshot {
            ChatAssistTheme(themeMode = ThemeMode.DARK, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DemoScreen()
                }
            }
        }
    }

    @Test
    fun `浅色主题圆角 token 展示`() {
        paparazzi.snapshot {
            ChatAssistTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ShapesDemo()
                }
            }
        }
    }
}

/** 模拟主屏组件（用于截图基线）。 */
@Composable
private fun DemoScreen() {
    Column(
        modifier = Modifier.padding(24.dp),
    ) {
        Text(
            text = "ChatAssist",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "本地聊天辅助 · 屏幕文本提取 + 大模型建议回复",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        // 主按钮（实色）
        Button(
            onClick = {},
            shape = MaterialTheme.shapes.medium,
        ) {
            Text("生成回复建议")
        }
        Spacer(Modifier.height(12.dp))

        // 次按钮（描边）
        OutlinedButton(
            onClick = {},
            shape = MaterialTheme.shapes.medium,
        ) {
            Text("设置")
        }
        Spacer(Modifier.height(24.dp))

        // 建议卡片（用 primaryContainer 主题色填充）
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("建议回复 [1]", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text("在的，刚看到你的消息～你周末有空吗？")
            }
        }
        Spacer(Modifier.height(16.dp))

        // 头像 + 状态条
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
                    .padding(2.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("初", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text("正在为您生成回复…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 圆角 token 展示（8/12/16/20/30 dp）。 */
@Composable
private fun ShapesDemo() {
    Column(modifier = Modifier.padding(20.dp)) {
        Text("ChatAssistShapes 圆角 token", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))

        listOf(
            "extraSmall 8dp" to MaterialTheme.shapes.extraSmall,
            "small 12dp" to MaterialTheme.shapes.small,
            "medium 16dp" to MaterialTheme.shapes.medium,
            "large 20dp" to MaterialTheme.shapes.large,
            "extraLarge 30dp" to MaterialTheme.shapes.extraLarge,
        ).forEach { (label, shape) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .width(60.dp)
                        .height(40.dp),
                ) {}
                Spacer(Modifier.width(16.dp))
                Text(label, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
