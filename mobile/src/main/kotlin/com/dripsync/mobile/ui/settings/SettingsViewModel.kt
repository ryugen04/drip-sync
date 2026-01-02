package com.dripsync.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dripsync.mobile.health.HealthConnectManager
import com.dripsync.mobile.health.HealthConnectRepository
import com.dripsync.mobile.health.SyncResult
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.data.preferences.UserPreferencesRepository
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
import javax.inject.Inject

enum class HealthConnectStatus {
    NOT_INSTALLED,
    NOT_PERMITTED,
    CONNECTED
}

data class SettingsUiState(
    val dailyGoalMl: Int = 2000,
    val presets: PresetSettings = PresetSettings(),
    val healthConnectStatus: HealthConnectStatus = HealthConnectStatus.NOT_INSTALLED,
    val isLoading: Boolean = true
)

sealed class SettingsEvent {
    data object SaveSuccess : SettingsEvent()
    data object SaveFailure : SettingsEvent()
    data class SyncComplete(val result: SyncResult) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthConnectManager: HealthConnectManager,
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val _event = MutableSharedFlow<SettingsEvent>()
    val event: SharedFlow<SettingsEvent> = _event.asSharedFlow()

    private val _healthConnectStatus = MutableStateFlow(HealthConnectStatus.NOT_INSTALLED)

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.observePreferences(),
        _healthConnectStatus
    ) { prefs, healthStatus ->
        SettingsUiState(
            dailyGoalMl = prefs.dailyGoalMl,
            presets = prefs.presets,
            healthConnectStatus = healthStatus,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        checkHealthConnectStatus()
    }

    fun checkHealthConnectStatus() {
        viewModelScope.launch {
            _healthConnectStatus.value = when {
                !healthConnectManager.isAvailable() -> HealthConnectStatus.NOT_INSTALLED
                !healthConnectManager.hasAllPermissions() -> HealthConnectStatus.NOT_PERMITTED
                else -> HealthConnectStatus.CONNECTED
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            _healthConnectStatus.value = HealthConnectStatus.CONNECTED
            syncToHealthConnect()
        }
    }

    fun syncToHealthConnect() {
        viewModelScope.launch {
            val result = healthConnectRepository.syncToHealthConnect()
            _event.emit(SettingsEvent.SyncComplete(result))
        }
    }

    fun getHealthConnectManager() = healthConnectManager

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
