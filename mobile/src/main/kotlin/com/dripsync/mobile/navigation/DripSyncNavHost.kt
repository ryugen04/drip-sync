package com.dripsync.mobile.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dripsync.mobile.ui.dashboard.DashboardScreen
import com.dripsync.mobile.ui.settings.SettingsScreen
import com.dripsync.mobile.ui.stats.StatsScreen

// カラーパレット
private val CyanBright = Color(0xFF06B6D4)
private val NavBarBackground = Color(0xFF0F172A)
private val GrayText = Color(0xFF64748B)

@Composable
fun DripSyncNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NavBarBackground,
        bottomBar = {
            NavigationBar(
                containerColor = NavBarBackground
            ) {
                BottomNavItem.entries.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == item.destination.route
                    } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.labelResId)
                            )
                        },
                        label = { Text(stringResource(item.labelResId)) },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanBright,
                            selectedTextColor = CyanBright,
                            unselectedIconColor = GrayText,
                            unselectedTextColor = GrayText,
                            indicatorColor = CyanBright.copy(alpha = 0.15f)
                        ),
                        onClick = {
                            navController.navigate(item.destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DripSyncDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(DripSyncDestination.Dashboard.route) {
                DashboardScreen()
            }
            composable(DripSyncDestination.Stats.route) {
                StatsScreen()
            }
            composable(DripSyncDestination.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
