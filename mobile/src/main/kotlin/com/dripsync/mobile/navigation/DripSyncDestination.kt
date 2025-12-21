package com.dripsync.mobile.navigation

/**
 * アプリ内のナビゲーション先を定義
 */
sealed class DripSyncDestination(val route: String) {
    // BottomNav destinations
    data object Dashboard : DripSyncDestination("dashboard")
    data object History : DripSyncDestination("history")
    data object Settings : DripSyncDestination("settings")
}
