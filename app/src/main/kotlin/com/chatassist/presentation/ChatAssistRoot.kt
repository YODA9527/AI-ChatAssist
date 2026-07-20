package com.chatassist.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chatassist.presentation.about.AboutScreen
import com.chatassist.presentation.history.HistoryScreen
import com.chatassist.presentation.main.MainScreen
import com.chatassist.presentation.navigation.Route
import com.chatassist.presentation.navigation.TopLevelTabs
import com.chatassist.presentation.settings.ModelSchemeEditScreen
import com.chatassist.presentation.settings.ModelSchemesScreen
import com.chatassist.presentation.settings.PersonaEditScreen
import com.chatassist.presentation.settings.PersonaListScreen
import com.chatassist.presentation.settings.SettingsScreen
import com.chatassist.presentation.theme.ChatAssistTheme
import com.chatassist.presentation.usage.UsageScreen

/**
 * 应用根 Composable。
 *
 * 由 MainActivity.setContent 调用。
 * 负责：主题 + 底部导航 + 4 个顶层 Tab 路由分发 + 人设子页面。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatAssistRoot(
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val themeMode by rootViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by rootViewModel.dynamicColor.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    // v0.15：开启 testTag-as-resourceId，让 Macrobenchmark 可用 By.res("chatassist_root_layout") 等待
    val rootModifier = Modifier
        .semantics { testTagsAsResourceId = true }
        .testTag("chatassist_root_layout")
    ChatAssistTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Scaffold(
            modifier = rootModifier,
            bottomBar = {
                NavigationBar {
                    TopLevelTabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == tab.route.path } == true ||
                            currentRoute == tab.route.path
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route.path) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Route.Home.path,
                modifier = Modifier.padding(padding),
            ) {
                composable(Route.Home.path) {
                    MainScreen()
                }
                composable(Route.Settings.path) {
                    SettingsScreen(navController = navController)
                }
                composable(Route.History.path) {
                    HistoryScreen()
                }
                composable(Route.Usage.path) {
                    UsageScreen()
                }
                composable(Route.About.path) {
                    AboutScreen()
                }
                // v2.2 §13.4 模型配置方案
                composable(Route.ModelSchemes.path) {
                    ModelSchemesScreen(navController = navController)
                }
                composable(Route.ModelSchemeEditNew.path) {
                    ModelSchemeEditScreen(navController = navController, schemeId = null)
                }
                composable(
                    route = Route.ModelSchemeEdit.path,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { backStackEntry ->
                    ModelSchemeEditScreen(
                        navController = navController,
                        schemeId = backStackEntry.arguments?.getString("id"),
                    )
                }
                // v0.18 双向人设子页面
                composable(Route.PersonaList.path) {
                    PersonaListScreen(navController = navController)
                }
                composable(Route.PersonaEditNew.path) {
                    PersonaEditScreen(navController = navController, profileId = null)
                }
                composable(
                    route = Route.PersonaEdit.path,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { backStackEntry ->
                    PersonaEditScreen(
                        navController = navController,
                        profileId = backStackEntry.arguments?.getString("id"),
                    )
                }
            }
        }
    }
}
