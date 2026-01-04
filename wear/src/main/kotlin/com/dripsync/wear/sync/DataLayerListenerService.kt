package com.dripsync.wear.sync

import com.dripsync.shared.sync.DataLayerPaths
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DataEventの情報を保持するデータクラス
 * DataEventBufferは非同期処理前に無効化されるため、必要な情報を抽出して保持
 */
data class DataEventInfo(
    val type: Int,
    val path: String,
    val dataItem: DataItem
)

/**
 * Mobileからのデータ変更を監視するサービス
 */
@AndroidEntryPoint
class DataLayerListenerService : WearableListenerService() {

    @Inject
    lateinit var dataLayerRepository: DataLayerRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        // DataEventBufferはメソッド終了後に無効化されるため、先にデータを抽出
        val events = dataEvents.map { event ->
            DataEventInfo(
                type = event.type,
                path = event.dataItem.uri.path.orEmpty(),
                dataItem = event.dataItem.freeze() // freezeでコピーを作成
            )
        }

        events.forEach { eventInfo ->
            if (eventInfo.path.isEmpty()) return@forEach

            serviceScope.launch {
                when {
                    eventInfo.path.startsWith(DataLayerPaths.HYDRATION_RECORD_PATH) -> {
                        dataLayerRepository.handleHydrationRecordFromMobile(eventInfo)
                    }
                    eventInfo.path == DataLayerPaths.PREFERENCES_PATH -> {
                        dataLayerRepository.handlePreferencesFromMobile(eventInfo)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
