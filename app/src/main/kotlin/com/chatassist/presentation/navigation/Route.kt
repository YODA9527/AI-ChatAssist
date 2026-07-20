package com.chatassist.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Compose Navigation 路由定义。
 *
 * 顶层 Tab 用 sealed object，子页面用 object（带 path 参数）。
 */
sealed class Route(val path: String) {
    // 顶层 Tab
    data object Home : Route("home")
    data object Settings : Route("settings")
    data object History : Route("history")
    data object Usage : Route("usage")
    data object About : Route("about")

    // 子页面
    data object ApiConfig : Route("settings/api")
    data object PersonaList : Route("settings/persona")
    data object PersonaEditNew : Route("settings/persona/new")
    data object PersonaEdit : Route("settings/persona/{id}")
    data object ToneConfig : Route("settings/tone")
    data object ThemeConfig : Route("settings/theme")
    data object PrivacyConfig : Route("settings/privacy")
    data object ModelSchemes : Route("settings/schemes")
    data object ModelSchemeEditNew : Route("settings/schemes/new")
    data object ModelSchemeEdit : Route("settings/schemes/{id}")
}

/**
 * 顶层 Tab 与底部导航栏的映射。
 */
data class TopLevelTab(
    val route: Route,
    val label: String,
    val icon: ImageVector,
)

val TopLevelTabs: List<TopLevelTab> = listOf(
    TopLevelTab(Route.Home, "首页", Icons.Filled.Home),
    TopLevelTab(Route.Settings, "设置", Icons.Filled.Settings),
    TopLevelTab(Route.History, "历史", Icons.Filled.History),
    TopLevelTab(Route.Usage, "用量", Icons.Filled.BarChart),
    TopLevelTab(Route.About, "关于", Icons.Filled.Info),
)
