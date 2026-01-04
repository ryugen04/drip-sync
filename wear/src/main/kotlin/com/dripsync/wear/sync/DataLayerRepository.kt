package com.dripsync.wear.sync

import android.content.Context
import com.dripsync.shared.data.model.BeverageType
import com.dripsync.shared.data.model.SourceDevice
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.shared.sync.DataLayerPaths
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wear↔Mobile間のデータ同期を管理するリポジトリ
 */
@Singleton
class DataLayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hydrationRepository: HydrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val dataClient: DataClient by lazy {
        Wearable.getDataClient(context)
    }

    /**
     * 水分記録をMobileに同期
     */
    suspend fun syncHydrationRecord(
        recordId: String,
        amountMl: Int,
        beverageType: BeverageType,
        recordedAt: Instant,
        sourceDevice: SourceDevice
    ) {
        val putDataReq = PutDataMapRequest.create(
            "${DataLayerPaths.HYDRATION_RECORD_PATH}/$recordId"
        ).apply {
            dataMap.putString(DataLayerPaths.KEY_RECORD_ID, recordId)
            dataMap.putInt(DataLayerPaths.KEY_AMOUNT_ML, amountMl)
            dataMap.putString(DataLayerPaths.KEY_BEVERAGE_TYPE, beverageType.name)
            dataMap.putLong(DataLayerPaths.KEY_RECORDED_AT, recordedAt.toEpochMilli())
            dataMap.putString(DataLayerPaths.KEY_SOURCE_DEVICE, sourceDevice.name)
            // 同期用タイムスタンプ（Data Layer APIは同一データで変更通知しないため必須）
            dataMap.putLong(DataLayerPaths.KEY_SYNC_TIMESTAMP, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(putDataReq).await()
    }

    /**
     * 今日の全記録をMobileに同期（起動時・定期同期用）
     */
    suspend fun syncAllTodayRecords() {
        val todayRecords = hydrationRepository.getTodayRecords()
        todayRecords.forEach { record ->
            try {
                syncHydrationRecord(
                    recordId = record.id,
                    amountMl = record.amountMl,
                    beverageType = record.beverageType,
                    recordedAt = record.recordedAt,
                    sourceDevice = record.sourceDevice
                )
            } catch (e: Exception) {
                // 個別の同期エラーは無視して続行
            }
        }
    }

    /**
     * 設定をMobileに同期
     */
    suspend fun syncPreferences() {
        val preferences = userPreferencesRepository.getPreferences()

        val putDataReq = PutDataMapRequest.create(DataLayerPaths.PREFERENCES_PATH).apply {
            dataMap.putInt(DataLayerPaths.KEY_DAILY_GOAL_ML, preferences.dailyGoalMl)
            dataMap.putInt(DataLayerPaths.KEY_PRESET_1, preferences.presets.preset1Ml)
            dataMap.putInt(DataLayerPaths.KEY_PRESET_2, preferences.presets.preset2Ml)
            dataMap.putInt(DataLayerPaths.KEY_PRESET_3, preferences.presets.preset3Ml)
            // タイムスタンプを追加して変更を検知させる
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(putDataReq).await()
    }

    /**
     * Mobileからの水分記録を処理
     */
    suspend fun handleHydrationRecordFromMobile(eventInfo: DataEventInfo) {
        if (eventInfo.type != DataEvent.TYPE_CHANGED) return
        if (!eventInfo.path.startsWith(DataLayerPaths.HYDRATION_RECORD_PATH)) return

        val dataMap = DataMapItem.fromDataItem(eventInfo.dataItem).dataMap

        val recordId = dataMap.getString(DataLayerPaths.KEY_RECORD_ID) ?: return
        val amountMl = dataMap.getInt(DataLayerPaths.KEY_AMOUNT_ML)
        val beverageType = try {
            BeverageType.valueOf(dataMap.getString(DataLayerPaths.KEY_BEVERAGE_TYPE) ?: "WATER")
        } catch (e: Exception) {
            BeverageType.WATER
        }
        val recordedAt = Instant.ofEpochMilli(dataMap.getLong(DataLayerPaths.KEY_RECORDED_AT))
        val sourceDevice = try {
            SourceDevice.valueOf(dataMap.getString(DataLayerPaths.KEY_SOURCE_DEVICE) ?: "MOBILE")
        } catch (e: Exception) {
            SourceDevice.MOBILE
        }

        // 自分自身が送信したデータは無視
        if (sourceDevice == SourceDevice.WEAR) return

        // 既存の記録がなければ追加（同じrecordIdとrecordedAtを使用）
        val existing = hydrationRepository.getRecordById(recordId)
        if (existing == null) {
            hydrationRepository.recordHydration(
                amountMl = amountMl,
                beverageType = beverageType,
                sourceDevice = sourceDevice,
                recordId = recordId,
                recordedAt = recordedAt
            )
        }
    }

    /**
     * Mobileからの設定変更を処理
     */
    suspend fun handlePreferencesFromMobile(eventInfo: DataEventInfo) {
        if (eventInfo.type != DataEvent.TYPE_CHANGED) return
        if (eventInfo.path != DataLayerPaths.PREFERENCES_PATH) return

        val dataMap = DataMapItem.fromDataItem(eventInfo.dataItem).dataMap

        val dailyGoalMl = dataMap.getInt(DataLayerPaths.KEY_DAILY_GOAL_ML)
        val preset1 = dataMap.getInt(DataLayerPaths.KEY_PRESET_1)
        val preset2 = dataMap.getInt(DataLayerPaths.KEY_PRESET_2)
        val preset3 = dataMap.getInt(DataLayerPaths.KEY_PRESET_3)

        if (dailyGoalMl > 0) {
            userPreferencesRepository.updateDailyGoal(dailyGoalMl)
        }
        if (preset1 > 0 && preset2 > 0 && preset3 > 0) {
            userPreferencesRepository.updatePresets(preset1, preset2, preset3)
        }
    }
}
