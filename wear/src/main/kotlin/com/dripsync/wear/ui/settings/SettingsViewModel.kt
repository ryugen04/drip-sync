package com.dripsync.wear.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.wear.sync.DataLayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dataLayerRepository: DataLayerRepository
) : ViewModel() {

    val presets: StateFlow<PresetSettings> = userPreferencesRepository
        .observePresets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PresetSettings()
        )

    val dailyGoal: StateFlow<Int> = userPreferencesRepository
        .observeDailyGoal()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 2000
        )

    init {
        syncWithMobile()
    }

    fun syncWithMobile() {
        viewModelScope.launch {
            try {
                dataLayerRepository.syncPreferences()
            } catch (e: Exception) {
                // Mobile未接続時はエラーを無視
            }
        }
    }

    fun updatePreset(index: Int, amountMl: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updatePreset(index, amountMl)
            try {
                dataLayerRepository.syncPreferences()
            } catch (e: Exception) {
                // Mobile未接続時はエラーを無視
            }
        }
    }

    fun updateDailyGoal(goalMl: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateDailyGoal(goalMl)
            try {
                dataLayerRepository.syncPreferences()
            } catch (e: Exception) {
                // Mobile未接続時はエラーを無視
            }
        }
    }
}
