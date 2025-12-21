package com.dripsync.wear.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dripsync.shared.data.model.SourceDevice
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val todayTotalMl: Int = 0,
    val dailyGoalMl: Int = 2000,
    val presets: PresetSettings = PresetSettings()
) {
    val progressPercent: Int
        get() = if (dailyGoalMl > 0) {
            (todayTotalMl.toFloat() / dailyGoalMl * 100).toInt()
        } else 0

    val displayTotal: String
        get() = if (todayTotalMl >= 1000) {
            String.format("%.1fL", todayTotalMl / 1000f)
        } else {
            "${todayTotalMl}ml"
        }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        hydrationRepository.observeTodayTotal(),
        userPreferencesRepository.observePreferences()
    ) { todayTotal, preferences ->
        HomeUiState(
            todayTotalMl = todayTotal,
            dailyGoalMl = preferences.dailyGoalMl,
            presets = preferences.presets
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun recordHydration(amountMl: Int) {
        viewModelScope.launch {
            hydrationRepository.recordHydration(
                amountMl = amountMl,
                sourceDevice = SourceDevice.WEAR
            )
        }
    }
}
