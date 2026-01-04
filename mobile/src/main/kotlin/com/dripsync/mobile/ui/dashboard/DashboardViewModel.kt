package com.dripsync.mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dripsync.mobile.health.HealthConnectRepository
import com.dripsync.mobile.sync.DataLayerRepository
import com.dripsync.mobile.ui.components.DailyChartData
import com.dripsync.shared.data.model.BeverageType
import com.dripsync.shared.data.model.SourceDevice
import java.time.Instant
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.shared.domain.model.Hydration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val todayTotalMl: Int = 0,
    val dailyGoalMl: Int = 2000,
    val weeklyData: List<DailyChartData> = emptyList(),
    val presets: PresetSettings = PresetSettings(),
    val todayRecords: List<Hydration> = emptyList(),
    val isLoading: Boolean = true
) {
    val progressPercent: Int
        get() = if (dailyGoalMl > 0) {
            (todayTotalMl.toFloat() / dailyGoalMl * 100).toInt()
        } else 0

    val progressRatio: Float
        get() = if (dailyGoalMl > 0) {
            (todayTotalMl.toFloat() / dailyGoalMl).coerceAtMost(1f)
        } else 0f

    val displayTotal: String
        get() = if (todayTotalMl >= 1000) {
            String.format("%.1fL", todayTotalMl / 1000f)
        } else {
            "${todayTotalMl}ml"
        }

    val displayGoal: String
        get() = if (dailyGoalMl >= 1000) {
            String.format("%.1fL", dailyGoalMl / 1000f)
        } else {
            "${dailyGoalMl}ml"
        }

    val remainingMl: Int
        get() = maxOf(0, dailyGoalMl - todayTotalMl)

    val isGoalAchieved: Boolean
        get() = todayTotalMl >= dailyGoalMl
}

sealed class DashboardEvent {
    data class RecordSuccess(val amountMl: Int) : DashboardEvent()
    data object RecordFailure : DashboardEvent()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthConnectRepository: HealthConnectRepository,
    private val dataLayerRepository: DataLayerRepository
) : ViewModel() {

    private val _event = MutableSharedFlow<DashboardEvent>()
    val event: SharedFlow<DashboardEvent> = _event.asSharedFlow()

    private val _weeklyData = MutableStateFlow<List<DailyChartData>>(emptyList())

    val uiState: StateFlow<DashboardUiState> = combine(
        hydrationRepository.observeTodayTotal(),
        hydrationRepository.observeTodayRecords(),
        userPreferencesRepository.observePreferences(),
        _weeklyData
    ) { todayTotal, todayRecords, preferences, weeklyData ->
        DashboardUiState(
            todayTotalMl = todayTotal,
            dailyGoalMl = preferences.dailyGoalMl,
            weeklyData = weeklyData,
            presets = preferences.presets,
            todayRecords = todayRecords.sortedByDescending { it.recordedAt },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        loadWeeklyData()
    }

    fun recordHydration(amountMl: Int) {
        viewModelScope.launch {
            try {
                val recordId = hydrationRepository.recordHydration(
                    amountMl = amountMl,
                    sourceDevice = SourceDevice.MOBILE
                )
                _event.emit(DashboardEvent.RecordSuccess(amountMl))
                // 週間データを更新
                loadWeeklyData()
                // Health Connectに同期
                healthConnectRepository.syncToHealthConnect()
                // Wearに同期
                try {
                    dataLayerRepository.syncHydrationRecord(
                        recordId = recordId,
                        amountMl = amountMl,
                        beverageType = BeverageType.WATER,
                        recordedAt = Instant.now(),
                        sourceDevice = SourceDevice.MOBILE
                    )
                } catch (e: Exception) {
                    // Wear未接続時はエラーを無視
                }
            } catch (e: Exception) {
                _event.emit(DashboardEvent.RecordFailure)
            }
        }
    }

    private fun loadWeeklyData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val weekAgo = today.minusDays(6)
            val goalMl = userPreferencesRepository.getPreferences().dailyGoalMl
            val records = hydrationRepository.getRecordsByDateRange(weekAgo, today)

            // 日付でグループ化して合計を計算
            val dailyTotals = records.groupBy { it.recordedDate }
                .mapValues { entry -> entry.value.sumOf { it.amountMl } }

            // 過去7日分のデータを生成
            val data = (0..6).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                DailyChartData(
                    date = date,
                    amountMl = dailyTotals[date] ?: 0,
                    goalMl = goalMl
                )
            }.reversed()

            _weeklyData.value = data
        }
    }
}
