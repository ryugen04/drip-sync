package com.dripsync.wear.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.tiles.TileService
import com.dripsync.shared.data.model.BeverageType
import com.dripsync.shared.data.model.SourceDevice
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.wear.complication.HydrationComplicationService
import com.dripsync.wear.sync.DataLayerRepository
import java.time.Instant
import com.dripsync.wear.tile.HydrationTileService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

sealed class RecordEvent {
    data class Success(val amountMl: Int) : RecordEvent()
    data object Failure : RecordEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val hydrationRepository: HydrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dataLayerRepository: DataLayerRepository
) : ViewModel() {

    private val _recordEvent = MutableSharedFlow<RecordEvent>()
    val recordEvent: SharedFlow<RecordEvent> = _recordEvent.asSharedFlow()

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

    init {
        syncWithMobile()
    }

    /**
     * Mobileと同期（起動時・フォアグラウンド復帰時に呼び出し）
     */
    fun syncWithMobile() {
        viewModelScope.launch {
            try {
                dataLayerRepository.syncAllTodayRecords()
            } catch (e: Exception) {
                // 同期エラーは無視
            }
        }
    }

    fun recordHydration(amountMl: Int) {
        viewModelScope.launch {
            try {
                val recordId = hydrationRepository.recordHydration(
                    amountMl = amountMl,
                    sourceDevice = SourceDevice.WEAR
                )
                // Mobileに同期
                dataLayerRepository.syncHydrationRecord(
                    recordId = recordId,
                    amountMl = amountMl,
                    beverageType = BeverageType.WATER,
                    recordedAt = Instant.now(),
                    sourceDevice = SourceDevice.WEAR
                )
                // タイルを更新
                TileService.getUpdater(application)
                    .requestUpdate(HydrationTileService::class.java)
                // コンプリケーションを更新
                HydrationComplicationService.requestUpdate(application)
                _recordEvent.emit(RecordEvent.Success(amountMl))
            } catch (e: Exception) {
                _recordEvent.emit(RecordEvent.Failure)
            }
        }
    }
}
