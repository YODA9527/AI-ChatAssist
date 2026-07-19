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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chatassist.presentation.about.AboutScreen
import com.chatassist.presentation.history.HistoryScreen
import com.chatassist.presentation.main.MainScreen
import com.chatassist.presentation.navigation.Route
import com.chatassist.presentation.navigation.TopLevelTabs
import com.chatassist.presentation.settings.SettingsScreen
import com.chatassist.presentation.theme.ChatAssistTheme

/**
 * 应用根 Composable。
 *
 * 由 MainActivity.setContent 调用。
 * 负责：主题 + 底部导航 + 4 个顶层 Tab 路由分发。
 */
@Composable
fun ChatAssistRoot(
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val themeMode by rootViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by rootViewModel.dynamicColor.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    ChatAssistTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Scaffold(
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
                    SettingsScreen()
                }
                composable(Route.History.path) {
                    HistoryScreen()
                }
                composable(Route.About.path) {
                    AboutScreen()
                }
            }
        }
    }
}
