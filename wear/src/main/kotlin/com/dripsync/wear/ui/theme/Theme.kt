package com.dripsync.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

// 水をイメージしたカラーパレット
private val LightBlue = Color(0xFF03A9F4)
private val DarkBlue = Color(0xFF0288D1)
private val LightCyan = Color(0xFFB3E5FC)

private val WearColorPalette = Colors(
    primary = LightBlue,
    primaryVariant = DarkBlue,
    secondary = LightCyan,
    secondaryVariant = DarkBlue,
    background = Color.Black,
    surface = Color(0xFF1A1A1A),
    error = Color(0xFFCF6679),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

@Composable
fun DripSyncWearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearColorPalette,
        content = content
    )
}
