package com.dripsync.mobile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.shared.data.preferences.UserPreferencesRepository
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
import java.time.YearMonth
import javax.inject.Inject

data class DaySummary(
    val date: LocalDate,
    val totalMl: Int,
    val goalMl: Int
) {
    val progress: Float
        get() = if (goalMl > 0) (totalMl.toFloat() / goalMl).coerceAtMost(1f) else 0f

    val isGoalAchieved: Boolean
        get() = totalMl >= goalMl
}

data class HistoryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val daySummaries: Map<LocalDate, DaySummary> = emptyMap(),
    val selectedDayRecords: List<Hydration> = emptyList(),
    val dailyGoalMl: Int = 2000,
    val isLoading: Boolean = true
) {
    val selectedDaySummary: DaySummary?
        get() = daySummaries[selectedDate]

    val selectedDayTotal: Int
        get() = selectedDaySummary?.totalMl ?: 0
}

sealed class HistoryEvent {
    data object DeleteSuccess : HistoryEvent()
    data object DeleteFailure : HistoryEvent()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _event = MutableSharedFlow<HistoryEvent>()
    val event: SharedFlow<HistoryEvent> = _event.asSharedFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _daySummaries = MutableStateFlow<Map<LocalDate, DaySummary>>(emptyMap())
    private val _selectedDayRecords = MutableStateFlow<List<Hydration>>(emptyList())

    val uiState: StateFlow<HistoryUiState> = combine(
        _selectedDate,
        _currentMonth,
        _daySummaries,
        _selectedDayRecords,
        userPreferencesRepository.observeDailyGoal()
    ) { selectedDate, currentMonth, summaries, records, goalMl ->
        HistoryUiState(
            selectedDate = selectedDate,
            currentMonth = currentMonth,
            daySummaries = summaries,
            selectedDayRecords = records.sortedByDescending { it.recordedAt },
            dailyGoalMl = goalMl,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    init {
        loadMonthData(_currentMonth.value)
        loadDayRecords(_selectedDate.value)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        loadDayRecords(date)
    }

    fun changeMonth(month: YearMonth) {
        _currentMonth.value = month
        loadMonthData(month)
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            try {
                hydrationRepository.deleteRecord(id)
                _event.emit(HistoryEvent.DeleteSuccess)
                // データを再読み込み
                loadDayRecords(_selectedDate.value)
                loadMonthData(_currentMonth.value)
            } catch (e: Exception) {
                _event.emit(HistoryEvent.DeleteFailure)
            }
        }
    }

    private fun loadMonthData(month: YearMonth) {
        viewModelScope.launch {
            val startDate = month.atDay(1)
            val endDate = month.atEndOfMonth()
            val goalMl = userPreferencesRepository.getPreferences().dailyGoalMl
            val records = hydrationRepository.getRecordsByDateRange(startDate, endDate)

            // 日付ごとに集計
            val summaries = records
                .groupBy { it.recordedDate }
                .mapValues { (date, dayRecords) ->
                    DaySummary(
                        date = date,
                        totalMl = dayRecords.sumOf { it.amountMl },
                        goalMl = goalMl
                    )
                }

            _daySummaries.value = summaries
        }
    }

    private fun loadDayRecords(date: LocalDate) {
        viewModelScope.launch {
            val records = hydrationRepository.getRecordsByDateRange(date, date)
            _selectedDayRecords.value = records
        }
    }
}
