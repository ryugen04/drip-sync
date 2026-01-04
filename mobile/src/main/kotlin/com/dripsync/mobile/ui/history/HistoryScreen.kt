package com.dripsync.mobile.ui.history

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dripsync.mobile.R
import com.dripsync.mobile.ui.components.RecordItem
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.flow.collectLatest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val CyanBright = Color(0xFF00E5FF)
private val SuccessGreen = Color(0xFF4CAF50)

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // イベント処理
    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is HistoryEvent.DeleteSuccess -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.history_delete_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is HistoryEvent.DeleteFailure -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.history_delete_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val currentMonth = uiState.currentMonth
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek) }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    // 月が変わったらデータを読み込み
    LaunchedEffect(calendarState.firstVisibleMonth.yearMonth) {
        viewModel.changeMonth(calendarState.firstVisibleMonth.yearMonth)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 月ナビゲーション
            MonthHeader(
                currentMonth = calendarState.firstVisibleMonth.yearMonth,
                onPreviousMonth = {
                    val newMonth = calendarState.firstVisibleMonth.yearMonth.minusMonths(1)
                    viewModel.changeMonth(newMonth)
                },
                onNextMonth = {
                    val newMonth = calendarState.firstVisibleMonth.yearMonth.plusMonths(1)
                    viewModel.changeMonth(newMonth)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 曜日ヘッダー
            DaysOfWeekHeader(daysOfWeek = daysOfWeek)

            // カレンダー
            HorizontalCalendar(
                state = calendarState,
                dayContent = { day ->
                    Day(
                        day = day,
                        isSelected = day.date == uiState.selectedDate,
                        daySummary = uiState.daySummaries[day.date],
                        onClick = {
                            if (day.position == DayPosition.MonthDate) {
                                viewModel.selectDate(day.date)
                            }
                        }
                    )
                },
                modifier = Modifier.height(280.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 選択日の情報
            SelectedDayInfo(
                date = uiState.selectedDate,
                totalMl = uiState.selectedDayTotal,
                goalMl = uiState.dailyGoalMl
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 記録リスト
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.selectedDayRecords.isEmpty()) {
                    item {
                        Text(
                            text = "この日の記録はありません",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(
                        items = uiState.selectedDayRecords,
                        key = { it.id }
                    ) { record ->
                        RecordItem(
                            record = record,
                            presets = uiState.presets,
                            onDelete = { viewModel.deleteRecord(record.id) },
                            showDeleteButton = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "前月"
            )
        }

        Text(
            text = "${currentMonth.year}年 ${currentMonth.monthValue}月",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "翌月"
            )
        }
    }
}

@Composable
private fun DaysOfWeekHeader(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        daysOfWeek.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    isSelected: Boolean,
    daySummary: DaySummary?,
    onClick: () -> Unit
) {
    val isToday = day.date == LocalDate.now()
    val isCurrentMonth = day.position == DayPosition.MonthDate

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> CyanBright
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = isCurrentMonth, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.Black
                    !isCurrentMonth -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    isToday -> CyanBright
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )

            // 達成インジケーター
            if (daySummary != null && isCurrentMonth) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (daySummary.isGoalAchieved) SuccessGreen
                            else CyanBright.copy(alpha = daySummary.progress.coerceAtLeast(0.3f))
                        )
                )
            }
        }
    }
}

@Composable
private fun SelectedDayInfo(
    date: LocalDate,
    totalMl: Int,
    goalMl: Int
) {
    val displayDate = if (date == LocalDate.now()) {
        "今日"
    } else {
        "${date.monthValue}/${date.dayOfMonth}"
    }

    val displayAmount = if (totalMl >= 1000) {
        String.format("%.1fL", totalMl / 1000f)
    } else {
        "${totalMl}ml"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "$displayAmount / ${goalMl}ml",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}
