package com.dripsync.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.dripsync.wear.ui.home.HomeScreen
import com.dripsync.wear.ui.settings.PresetSettingsScreen
import com.dripsync.wear.ui.theme.DripSyncWearTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DripSyncWearTheme {
                DripSyncNavHost()
            }
        }
    }
}

object NavRoutes {
    const val HOME = "home"
    const val PRESET_SETTINGS = "preset_settings"
}

@Composable
fun DripSyncNavHost() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.PRESET_SETTINGS)
                }
            )
        }
        composable(NavRoutes.PRESET_SETTINGS) {
            PresetSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
