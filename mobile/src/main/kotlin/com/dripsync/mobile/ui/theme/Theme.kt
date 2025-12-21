package com.dripsync.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 水をイメージしたカラーパレット
private val LightBlue = Color(0xFF03A9F4)
private val DarkBlue = Color(0xFF0288D1)
private val LightCyan = Color(0xFFB3E5FC)

private val DarkColorScheme = darkColorScheme(
    primary = LightBlue,
    secondary = LightCyan,
    tertiary = DarkBlue,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

private val LightColorScheme = lightColorScheme(
    primary = DarkBlue,
    secondary = LightBlue,
    tertiary = LightCyan,
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun DripSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
