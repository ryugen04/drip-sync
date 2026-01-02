package com.dripsync.mobile.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class DayStats(
    val date: LocalDate,
    val amountMl: Int,
    val goalMl: Int
) {
    val percentage: Float
        get() = if (goalMl > 0) (amountMl.toFloat() / goalMl * 100).coerceAtMost(100f) else 0f

    val isCompleted: Boolean
        get() = amountMl >= goalMl

    val dayLabel: String
        get() = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())

    val dateLabel: String
        get() = "${date.monthValue}/${date.dayOfMonth}"
}

data class StatsUiState(
    val todayTotal: Int = 0,
    val dailyGoalMl: Int = 2000,
    val weeklyData: List<DayStats> = emptyList(),
    val currentStreak: Int = 0,
    val isLoading: Boolean = true
) {
    val weeklyTotal: Int
        get() = weeklyData.sumOf { it.amountMl }

    val weeklyTotalLiters: String
        get() = String.format("%.1f", weeklyTotal / 1000f)

    val weeklyAverageMl: Int
        get() = if (weeklyData.isNotEmpty()) weeklyTotal / weeklyData.size else 0
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val weekAgo = today.minusDays(6)

    // リアクティブに週間データを監視
    private val weeklyRecordsFlow = hydrationRepository.observeRecordsByDateRange(weekAgo, today)

    val uiState: StateFlow<StatsUiState> = combine(
        hydrationRepository.observeTodayTotal(),
        userPreferencesRepository.observePreferences(),
        weeklyRecordsFlow
    ) { todayTotal, preferences, records ->
        val goalMl = preferences.dailyGoalMl

        // 日付でグループ化して合計を計算
        val dailyTotals = records.groupBy { it.recordedDate }
            .mapValues { entry -> entry.value.sumOf { it.amountMl } }

        // 過去7日分のデータを生成
        val weeklyData = (0..6).map { index ->
            val date = weekAgo.plusDays(index.toLong())
            DayStats(
                date = date,
                amountMl = dailyTotals[date] ?: 0,
                goalMl = goalMl
            )
        }

        // 連続達成日数を計算
        var streak = 0
        var checkDate = today
        while (dailyTotals[checkDate] ?: 0 >= goalMl) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        StatsUiState(
            todayTotal = todayTotal,
            dailyGoalMl = goalMl,
            weeklyData = weeklyData,
            currentStreak = streak,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )
}
