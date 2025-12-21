package com.dripsync.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val dailyGoalMl: Int = 2000,
    val presets: PresetSettings = PresetSettings(),
    val isLoading: Boolean = true
)

sealed class SettingsEvent {
    data object SaveSuccess : SettingsEvent()
    data object SaveFailure : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _event = MutableSharedFlow<SettingsEvent>()
    val event: SharedFlow<SettingsEvent> = _event.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = userPreferencesRepository
        .observePreferences()
        .map { prefs ->
            SettingsUiState(
                dailyGoalMl = prefs.dailyGoalMl,
                presets = prefs.presets,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )

    fun updateDailyGoal(goalMl: Int) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateDailyGoal(goalMl)
                _event.emit(SettingsEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(SettingsEvent.SaveFailure)
            }
        }
    }

    fun updatePreset(index: Int, amountMl: Int) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updatePreset(index, amountMl)
                _event.emit(SettingsEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(SettingsEvent.SaveFailure)
            }
        }
    }
}
