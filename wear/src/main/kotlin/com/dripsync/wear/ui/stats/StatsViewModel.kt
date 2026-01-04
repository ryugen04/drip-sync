package com.dripsync.wear.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.shared.domain.model.HourlyHydrationPoint
import com.dripsync.shared.domain.model.IdealHydrationSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class StatsUiState(
    val hourlyData: List<HourlyHydrationPoint> = emptyList(),
    val dailyGoalMl: Int = 1500,
    val todayTotalMl: Int = 0,
    val isLoading: Boolean = true
) {
    val progressPercent: Int
        get() = if (dailyGoalMl > 0) {
            ((todayTotalMl.toFloat() / dailyGoalMl) * 100).toInt().coerceAtMost(100)
        } else 0

    val isGoalAchieved: Boolean
        get() = todayTotalMl >= dailyGoalMl
}

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

    fun refreshStats() {
        viewModelScope.launch {
            val dailyGoal = userPreferencesRepository.observeDailyGoal().first()
            val today = LocalDate.now()
            val records = hydrationRepository.getRecordsByDateRange(today, today)
            val todayTotal = records.sumOf { it.amountMl }

            // 時間帯ごとの累積データを計算
            val hourlyData = calculateHourlyData(records, dailyGoal)

            _uiState.value = StatsUiState(
                hourlyData = hourlyData,
                dailyGoalMl = dailyGoal,
                todayTotalMl = todayTotal,
                isLoading = false
            )
        }
    }

    private fun calculateHourlyData(
        records: List<com.dripsync.shared.domain.model.Hydration>,
        goalMl: Int
    ): List<HourlyHydrationPoint> {
        val zoneId = ZoneId.systemDefault()
        val now = LocalTime.now()
        val points = mutableListOf<HourlyHydrationPoint>()

        // 30分間隔でポイントを生成（6:00〜22:00）
        var currentTime = IdealHydrationSchedule.startTime
        var cumulativeActual = 0

        // 各記録の時刻と量を取得
        val recordsByTime = records.map { record ->
            val time = record.recordedAt.atZone(zoneId).toLocalTime()
            Pair(time, record.amountMl)
        }.sortedBy { it.first }

        while (!currentTime.isAfter(IdealHydrationSchedule.endTime)) {
            // この時刻までの累積実績を計算
            cumulativeActual = recordsByTime
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
        val currentPoints = points.map { point ->
            if (point.time.isAfter(now)) {
                point.copy(actualCumulative = points.lastOrNull { !it.time.isAfter(now) }?.actualCumulative ?: 0)
            } else {
                point
            }
        }

        return currentPoints
    }
}
