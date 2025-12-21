package com.dripsync.mobile.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dripsync.mobile.R
import kotlinx.coroutines.flow.collectLatest

private val CyanBright = Color(0xFF00E5FF)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // イベント処理
    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is SettingsEvent.SaveSuccess -> {
                    // 保存成功（通知不要）
                }
                is SettingsEvent.SaveFailure -> {
                    Toast.makeText(context, "保存に失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // 目標設定
            GoalSettingCard(
                currentGoalMl = uiState.dailyGoalMl,
                onGoalChange = { viewModel.updateDailyGoal(it) }
            )

            // プリセット設定
            PresetSettingsCard(
                preset1 = uiState.presets.preset1Ml,
                preset2 = uiState.presets.preset2Ml,
                preset3 = uiState.presets.preset3Ml,
                onPresetChange = { index, amount ->
                    viewModel.updatePreset(index, amount)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GoalSettingCard(
    currentGoalMl: Int,
    onGoalChange: (Int) -> Unit
) {
    var sliderValue by remember(currentGoalMl) {
        mutableFloatStateOf(currentGoalMl.toFloat())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_daily_goal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 現在の目標値表示
            Text(
                text = "${sliderValue.toInt()} ml",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CyanBright
            )

            Spacer(modifier = Modifier.height(8.dp))

            // スライダー
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onGoalChange(sliderValue.toInt()) },
                valueRange = 500f..5000f,
                steps = 44, // 100ml刻み
                colors = SliderDefaults.colors(
                    thumbColor = CyanBright,
                    activeTrackColor = CyanBright
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "500ml",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "5000ml",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun PresetSettingsCard(
    preset1: Int,
    preset2: Int,
    preset3: Int,
    onPresetChange: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_presets),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            PresetItem(
                label = stringResource(R.string.settings_preset_format, 1),
                amountMl = preset1,
                onDecrease = { onPresetChange(0, maxOf(50, preset1 - 50)) },
                onIncrease = { onPresetChange(0, minOf(2000, preset1 + 50)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PresetItem(
                label = stringResource(R.string.settings_preset_format, 2),
                amountMl = preset2,
                onDecrease = { onPresetChange(1, maxOf(50, preset2 - 50)) },
                onIncrease = { onPresetChange(1, minOf(2000, preset2 + 50)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PresetItem(
                label = stringResource(R.string.settings_preset_format, 3),
                amountMl = preset3,
                onDecrease = { onPresetChange(2, maxOf(50, preset3 - 50)) },
                onIncrease = { onPresetChange(2, minOf(2000, preset3 + 50)) }
            )
        }
    }
}

@Composable
private fun PresetItem(
    label: String,
    amountMl: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledIconButton(
                onClick = onDecrease,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "減らす"
                )
            }

            Text(
                text = "${amountMl}ml",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CyanBright
            )

            FilledIconButton(
                onClick = onIncrease,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "増やす"
                )
            }
        }
    }
}
