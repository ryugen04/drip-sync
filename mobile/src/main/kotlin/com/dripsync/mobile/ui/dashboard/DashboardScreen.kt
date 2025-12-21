package com.dripsync.mobile.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dripsync.mobile.R
import com.dripsync.mobile.ui.components.CircularProgress
import com.dripsync.mobile.ui.components.PresetButton
import com.dripsync.mobile.ui.components.RecordItem
import com.dripsync.mobile.ui.components.WeeklyChart
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // イベント処理
    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is DashboardEvent.RecordSuccess -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.record_success_format, event.amountMl),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is DashboardEvent.RecordFailure -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.record_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 円形プログレス
            item {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgress(
                    progress = uiState.progressRatio,
                    currentValue = uiState.displayTotal,
                    targetValue = uiState.displayGoal
                )
            }

            // 目標達成メッセージまたは残り量
            item {
                if (uiState.isGoalAchieved) {
                    Text(
                        text = stringResource(R.string.dashboard_goal_achieved),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.dashboard_remaining_format, uiState.remainingMl),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            // プリセットボタン
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PresetButton(
                        amountMl = uiState.presets.preset1Ml,
                        onClick = { viewModel.recordHydration(uiState.presets.preset1Ml) }
                    )
                    PresetButton(
                        amountMl = uiState.presets.preset2Ml,
                        onClick = { viewModel.recordHydration(uiState.presets.preset2Ml) }
                    )
                    PresetButton(
                        amountMl = uiState.presets.preset3Ml,
                        onClick = { viewModel.recordHydration(uiState.presets.preset3Ml) }
                    )
                }
            }

            // 週間グラフ
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "週間の記録",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    WeeklyChart(data = uiState.weeklyData)
                }
            }

            // 今日の記録
            item {
                Text(
                    text = "今日の記録",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            if (uiState.todayRecords.isEmpty()) {
                item {
                    Text(
                        text = "まだ記録がありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(
                    items = uiState.todayRecords,
                    key = { it.id }
                ) { record ->
                    RecordItem(
                        record = record,
                        showDeleteButton = false
                    )
                }
            }

            // 下部のスペース
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
