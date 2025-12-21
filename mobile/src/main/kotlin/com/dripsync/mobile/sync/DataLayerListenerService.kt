package com.dripsync.mobile.sync

import com.dripsync.shared.sync.DataLayerPaths
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Wear OSからのデータ変更を監視するサービス
 */
@AndroidEntryPoint
class DataLayerListenerService : WearableListenerService() {

    @Inject
    lateinit var dataLayerRepository: DataLayerRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path ?: return@forEach

            serviceScope.launch {
                when {
                    path.startsWith(DataLayerPaths.HYDRATION_RECORD_PATH) -> {
                        dataLayerRepository.handleHydrationRecordFromWear(event)
                    }
                    path == DataLayerPaths.PREFERENCES_PATH -> {
                        dataLayerRepository.handlePreferencesFromWear(event)
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
