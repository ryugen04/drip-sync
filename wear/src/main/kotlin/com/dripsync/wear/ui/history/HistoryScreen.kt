package com.dripsync.wear.ui.history

import android.app.Activity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.PaddingValues
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Text
import androidx.wear.widget.ConfirmationOverlay
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.domain.model.Hydration
import com.dripsync.shared.util.IconUtils
import com.dripsync.wear.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// カラーパレット（HomeScreenと共通）
private val CyanBright = Color(0xFF00E5FF)
private val BackgroundDark = Color(0xFF0D1520)
private val ButtonBackground = Color(0xFF1A2535)
private val TextGray = Color(0xFF5A6678)
private val TextLightGray = Color(0xFF8A9AAA)
private val DeleteRed = Color(0xFFFF5252)

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)

    // 削除イベントを監視
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            val activity = context as? Activity ?: return@collect
            val (type, message) = when (event) {
                is HistoryEvent.DeleteSuccess -> {
                    ConfirmationOverlay.SUCCESS_ANIMATION to "Deleted"
                }
                is HistoryEvent.DeleteFailure -> {
                    ConfirmationOverlay.FAILURE_ANIMATION to "Failed"
                }
            }
            ConfirmationOverlay()
                .setType(type)
                .setMessage(message)
                .showOn(activity)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // タイトル
        Text(
            text = "History",
            color = CyanBright,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.records.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No records",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        } else {
            // 日付ごとにグループ化
            val groupedRecords = uiState.records.groupBy { it.recordedDate }
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                autoCentering = null,
                contentPadding = PaddingValues(top = 0.dp, bottom = 16.dp)
            ) {
                groupedRecords.forEach { (date, records) ->
                    // 日付ヘッダー
                    item(key = "header_$date") {
                        DateHeader(
                            date = date,
                            today = today,
                            yesterday = yesterday
                        )
                    }

                    // その日の記録
                    items(
                        items = records,
                        key = { it.id }
                    ) { record ->
                        HistoryItem(
                            record = record,
                            presets = uiState.presets,
                            onDelete = { viewModel.deleteRecord(record.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    today: LocalDate,
    yesterday: LocalDate
) {
    val dateFormatter = DateTimeFormatter.ofPattern("M/d (E)")
    val displayText = when (date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> date.format(dateFormatter)
    }

    Text(
        text = displayText,
        color = TextLightGray,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun HistoryItem(
    record: Hydration,
    presets: PresetSettings,
    onDelete: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var showDeleteButton by remember { mutableStateOf(false) }

    val iconRes = when (IconUtils.getIconTypeForAmount(record.amountMl, presets)) {
        IconUtils.IconType.COFFEE -> R.drawable.ic_coffee
        IconUtils.IconType.GLASS -> R.drawable.ic_glass
        IconUtils.IconType.BOTTLE -> R.drawable.ic_bottle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ButtonBackground)
            .clickable { showDeleteButton = !showDeleteButton }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // アイコン
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(CyanBright.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                colorFilter = ColorFilter.tint(CyanBright)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (showDeleteButton) {
            // 削除ボタン表示時
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DeleteRed)
                    .clickable {
                        onDelete()
                        showDeleteButton = false
                    }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Delete",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // 通常表示
            Text(
                text = record.displayAmount,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // 時間
            Text(
                text = record.recordedAt
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(timeFormatter),
                color = TextLightGray,
                fontSize = 11.sp
            )
        }
    }
}
