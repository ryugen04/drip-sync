package com.dripsync.wear.ui.settings

import com.dripsync.wear.BuildConfig
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Text
import com.dripsync.wear.R

// カラーパレット（HomeScreenと共通）
private val CyanBright = Color(0xFF00E5FF)
private val BackgroundDark = Color(0xFF0D1520)
private val ButtonBackground = Color(0xFF1A2535)
private val TextGray = Color(0xFF5A6678)
private val TextLightGray = Color(0xFF8A9AAA)
private val RingBackground = Color(0xFF2A3545)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val presets by viewModel.presets.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // タイトル
        Text(
            text = "Settings",
            color = CyanBright,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        // サブタイトル
        Text(
            text = "Preset amounts",
            color = TextLightGray,
            fontSize = 9.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        PresetSettingItem(
            amountMl = presets.preset1Ml,
            onIncrease = { viewModel.updatePreset(0, presets.preset1Ml + 50) },
            onDecrease = { viewModel.updatePreset(0, maxOf(50, presets.preset1Ml - 50)) }
        )

        PresetSettingItem(
            amountMl = presets.preset2Ml,
            onIncrease = { viewModel.updatePreset(1, presets.preset2Ml + 50) },
            onDecrease = { viewModel.updatePreset(1, maxOf(50, presets.preset2Ml - 50)) }
        )

        PresetSettingItem(
            amountMl = presets.preset3Ml,
            onIncrease = { viewModel.updatePreset(2, presets.preset3Ml + 50) },
            onDecrease = { viewModel.updatePreset(2, maxOf(50, presets.preset3Ml - 50)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // バージョン表示
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            color = TextGray,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun PresetSettingItem(
    amountMl: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    // アイコンを量に応じて選択
    val iconRes = when {
        amountMl <= 200 -> R.drawable.ic_coffee
        amountMl <= 500 -> R.drawable.ic_glass
        else -> R.drawable.ic_bottle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // マイナスボタン
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(ButtonBackground)
                .clickable(onClick = onDecrease),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                color = CyanBright,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // アイコン
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            colorFilter = ColorFilter.tint(TextGray)
        )

        // 量表示
        Text(
            text = "${amountMl}ml",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(60.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // プラスボタン
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(ButtonBackground)
                .clickable(onClick = onIncrease),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = CyanBright,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
