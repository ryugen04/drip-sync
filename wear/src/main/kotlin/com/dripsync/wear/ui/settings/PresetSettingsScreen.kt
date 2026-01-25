package com.dripsync.wear.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.dripsync.shared.data.preferences.PresetSettings

@Composable
fun PresetSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "プリセット設定",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary
                )
            }

            item {
                PresetItem(
                    label = "ボタン 1",
                    amountMl = presets.preset1Ml,
                    onIncrease = { viewModel.updatePreset(0, presets.preset1Ml + 50) },
                    onDecrease = { viewModel.updatePreset(0, maxOf(50, presets.preset1Ml - 50)) }
                )
            }

            item {
                PresetItem(
                    label = "ボタン 2",
                    amountMl = presets.preset2Ml,
                    onIncrease = { viewModel.updatePreset(1, presets.preset2Ml + 50) },
                    onDecrease = { viewModel.updatePreset(1, maxOf(50, presets.preset2Ml - 50)) }
                )
            }

            item {
                PresetItem(
                    label = "ボタン 3",
                    amountMl = presets.preset3Ml,
                    onIncrease = { viewModel.updatePreset(2, presets.preset3Ml + 50) },
                    onDecrease = { viewModel.updatePreset(2, maxOf(50, presets.preset3Ml - 50)) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text("完了")
                }
            }
        }
    }
}

@Composable
private fun PresetItem(
    label: String,
    amountMl: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompactButton(
                onClick = onDecrease,
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text("-", style = MaterialTheme.typography.title2)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${amountMl}ml",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(80.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            CompactButton(
                onClick = onIncrease,
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text("+", style = MaterialTheme.typography.title2)
            }
        }
    }
}
