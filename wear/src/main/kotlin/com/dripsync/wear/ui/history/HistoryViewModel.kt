package com.dripsync.wear.ui.history

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.tiles.TileService
import com.dripsync.shared.data.preferences.PresetSettings
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.shared.domain.model.Hydration
import com.dripsync.wear.sync.DataLayerRepository
import com.dripsync.wear.tile.HydrationTileService
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

data class HistoryUiState(
    val records: List<Hydration> = emptyList(),
    val presets: PresetSettings = PresetSettings(),
    val isLoading: Boolean = true
)

sealed class HistoryEvent {
    data object DeleteSuccess : HistoryEvent()
    data object DeleteFailure : HistoryEvent()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val application: Application,
    private val hydrationRepository: HydrationRepository,
    private val dataLayerRepository: DataLayerRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _event = MutableSharedFlow<HistoryEvent>()
    val event: SharedFlow<HistoryEvent> = _event.asSharedFlow()

    private val _records = MutableStateFlow<List<Hydration>>(emptyList())

    val uiState: StateFlow<HistoryUiState> = combine(
        _records,
        userPreferencesRepository.observePresets()
    ) { records, presets ->
        HistoryUiState(
            records = records.sortedByDescending { it.recordedAt },
            presets = presets,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val weekAgo = today.minusDays(6)
            hydrationRepository.observeRecordsByDateRange(weekAgo, today).collect { records ->
                _records.value = records
            }
        }
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            try {
                hydrationRepository.deleteRecord(id)
                // タイルを更新
                TileService.getUpdater(application)
                    .requestUpdate(HydrationTileService::class.java)
                _event.emit(HistoryEvent.DeleteSuccess)
                // Mobileに削除を同期
                try {
                    dataLayerRepository.syncDeleteRecord(id)
                } catch (e: Exception) {
                    // Mobile未接続時はエラーを無視
                }
            } catch (e: Exception) {
                _event.emit(HistoryEvent.DeleteFailure)
            }
        }
    }
}
