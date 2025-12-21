package com.dripsync.wear.ui.history

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.tiles.TileService
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.shared.domain.model.Hydration
import com.dripsync.wear.tile.HydrationTileService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val records: List<Hydration> = emptyList(),
    val isLoading: Boolean = true
)

sealed class HistoryEvent {
    data object DeleteSuccess : HistoryEvent()
    data object DeleteFailure : HistoryEvent()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val application: Application,
    private val hydrationRepository: HydrationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<HistoryEvent>()
    val event: SharedFlow<HistoryEvent> = _event.asSharedFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            // 過去7日間の記録を監視
            val today = LocalDate.now()
            val weekAgo = today.minusDays(6)
            hydrationRepository.observeRecordsByDateRange(weekAgo, today).collect { records ->
                _uiState.value = HistoryUiState(
                    records = records.sortedByDescending { it.recordedAt },
                    isLoading = false
                )
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
            } catch (e: Exception) {
                _event.emit(HistoryEvent.DeleteFailure)
            }
        }
    }
}
