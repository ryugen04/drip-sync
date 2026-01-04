package com.dripsync.mobile.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.shared.domain.model.HourlyHydrationPoint
import com.dripsync.shared.domain.model.IdealHydrationSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

enum class StatsTab {
    DAILY, WEEKLY
}

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
    val selectedTab: StatsTab = StatsTab.DAILY,
    val todayTotal: Int = 0,
    val dailyGoalMl: Int = 1500,
    val weeklyData: List<DayStats> = emptyList(),
    val hourlyData: List<HourlyHydrationPoint> = emptyList(),
    val currentStreak: Int = 0,
    val isLoading: Boolean = true
) {
    val weeklyTotal: Int
        get() = weeklyData.sumOf { it.amountMl }

    val weeklyTotalLiters: String
        get() = String.format("%.1f", weeklyTotal / 1000f)

    val weeklyAverageMl: Int
        get() = if (weeklyData.isNotEmpty()) weeklyTotal / weeklyData.size else 0

    val progressPercent: Int
        get() = if (dailyGoalMl > 0) {
            ((todayTotal.toFloat() / dailyGoalMl) * 100).toInt().coerceAtMost(100)
        } else 0

    val isGoalAchieved: Boolean
        get() = todayTotal >= dailyGoalMl
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val weekAgo = today.minusDays(6)

    private val _selectedTab = MutableStateFlow(StatsTab.DAILY)

    private val weeklyRecordsFlow = hydrationRepository.observeRecordsByDateRange(weekAgo, today)
        .onStart { emit(emptyList()) }
    private val todayRecordsFlow = hydrationRepository.observeTodayRecords()
        .onStart { emit(emptyList()) }
    private val todayTotalFlow = hydrationRepository.observeTodayTotal()
        .onStart { emit(0) }
    private val preferencesFlow = userPreferencesRepository.observePreferences()

    val uiState: StateFlow<StatsUiState> = combine(
        _selectedTab,
        todayTotalFlow,
        preferencesFlow,
        weeklyRecordsFlow,
        todayRecordsFlow
    ) { selectedTab, todayTotal, preferences, weeklyRecords, todayRecords ->
        val goalMl = preferences.dailyGoalMl

        // 日付でグループ化して合計を計算
        val dailyTotals = weeklyRecords.groupBy { it.recordedDate }
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

        // 時間帯ごとの累積データを計算
        val hourlyData = calculateHourlyData(todayRecords, goalMl)

        StatsUiState(
            selectedTab = selectedTab,
            todayTotal = todayTotal,
            dailyGoalMl = goalMl,
            weeklyData = weeklyData,
            hourlyData = hourlyData,
            currentStreak = streak,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )

    fun selectTab(tab: StatsTab) {
        _selectedTab.value = tab
    }

    private fun calculateHourlyData(
        records: List<com.dripsync.shared.domain.model.Hydration>,
        goalMl: Int
    ): List<HourlyHydrationPoint> {
        val zoneId = ZoneId.systemDefault()
        val now = LocalTime.now()
        val points = mutableListOf<HourlyHydrationPoint>()

        // 30分間隔でポイントを生成（6:00〜23:00）
        var currentTime = IdealHydrationSchedule.startTime

        // 各記録の時刻と量を取得
        val recordsByTime = records.map { record ->
            val time = record.recordedAt.atZone(zoneId).toLocalTime()
            Pair(time, record.amountMl)
        }.sortedBy { it.first }

        while (!currentTime.isAfter(IdealHydrationSchedule.endTime)) {
            // この時刻までの累積実績を計算
            val cumulativeActual = recordsByTime
                .filter { !it.first.isAfter(currentTime) }
                .sumOf { it.second }

            val idealAmount = IdealHydrationSchedule.getIdealAmountAt(currentTime, goalMl)

            points.add(
                HourlyHydrationPoint(
                    time = currentTime,
                    actualCumulative = cumulativeActual,
                    idealCumulative = idealAmount
                )
            )

            currentTime = currentTime.plusMinutes(30)
        }

        // 現在時刻以降のポイントは実績を現在値で固定
        return points.map { point ->
            if (point.time.isAfter(now)) {
                point.copy(actualCumulative = points.lastOrNull { !it.time.isAfter(now) }?.actualCumulative ?: 0)
            } else {
                point
            }
        }
    }
}
