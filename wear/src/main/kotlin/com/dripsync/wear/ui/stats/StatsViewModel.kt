package com.dripsync.wear.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class DailyData(
    val date: LocalDate,
    val amountMl: Int,
    val goalMl: Int
) {
    val progressRatio: Float
        get() = if (goalMl > 0) (amountMl.toFloat() / goalMl).coerceAtMost(1f) else 0f
}

data class WeeklyData(
    val weekStartDate: LocalDate,
    val totalAmountMl: Int,
    val averageAmountMl: Int,
    val goalMl: Int
) {
    val progressRatio: Float
        get() = if (goalMl > 0) (averageAmountMl.toFloat() / goalMl).coerceAtMost(1f) else 0f
}

enum class StatsTab {
    DAILY, WEEKLY
}

data class StatsUiState(
    val selectedTab: StatsTab = StatsTab.DAILY,
    val dailyData: List<DailyData> = emptyList(),
    val weeklyData: List<WeeklyData> = emptyList(),
    val dailyGoalMl: Int = 2000,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        refreshStats()
    }

    fun selectTab(tab: StatsTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    // 画面表示時に呼び出して最新データを取得
    fun refreshStats() {
        viewModelScope.launch {
            val dailyGoal = userPreferencesRepository.observeDailyGoal().first()
            val today = LocalDate.now()

            // 過去7日間の日別データ
            val dailyData = (0 until 7).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val records = hydrationRepository.getRecordsByDateRange(date, date)
                val totalMl = records.sumOf { it.amountMl }
                DailyData(
                    date = date,
                    amountMl = totalMl,
                    goalMl = dailyGoal
                )
            }.reversed()

            // 過去4週間の週別データ
            val weeklyData = (0 until 4).map { weeksAgo ->
                val weekEnd = today.minusWeeks(weeksAgo.toLong())
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val weekStart = weekEnd.minusDays(6)
                val records = hydrationRepository.getRecordsByDateRange(weekStart, weekEnd)
                val totalMl = records.sumOf { it.amountMl }
                val daysWithRecords = records.groupBy { it.recordedDate }.size.coerceAtLeast(1)
                WeeklyData(
                    weekStartDate = weekStart,
                    totalAmountMl = totalMl,
                    averageAmountMl = totalMl / 7,
                    goalMl = dailyGoal
                )
            }.reversed()

            _uiState.value = StatsUiState(
                selectedTab = _uiState.value.selectedTab,
                dailyData = dailyData,
                weeklyData = weeklyData,
                dailyGoalMl = dailyGoal,
                isLoading = false
            )
        }
    }
}
