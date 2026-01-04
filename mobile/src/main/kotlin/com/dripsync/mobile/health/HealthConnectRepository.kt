package com.dripsync.mobile.health

import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import com.dripsync.shared.data.model.HydrationRecord as LocalHydrationRecord
import com.dripsync.shared.data.repository.HydrationRepository
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Connectとの水分データ同期を管理
 */
@Singleton
class HealthConnectRepository @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val hydrationRepository: HydrationRepository
) {
    /**
     * 未同期のローカル記録をHealth Connectに書き込む
     */
    suspend fun syncToHealthConnect(): SyncResult {
        val client = healthConnectManager.getClient() ?: return SyncResult.NotAvailable

        if (!healthConnectManager.hasAllPermissions()) {
            return SyncResult.PermissionRequired
        }

        val unsyncedRecords = hydrationRepository.getUnsyncedRecords()
        if (unsyncedRecords.isEmpty()) {
            return SyncResult.Success(synced = 0)
        }

        var syncedCount = 0
        val errors = mutableListOf<String>()

        for (record in unsyncedRecords) {
            try {
                val healthConnectRecord = createHealthConnectRecord(record)
                val response = client.insertRecords(listOf(healthConnectRecord))

                if (response.recordIdsList.isNotEmpty()) {
                    val healthConnectId = response.recordIdsList.first().id
                    hydrationRepository.markAsSynced(record.id, healthConnectId)
                    syncedCount++
                }
            } catch (e: Exception) {
                errors.add("${record.id}: ${e.message}")
            }
        }

        return if (errors.isEmpty()) {
            SyncResult.Success(synced = syncedCount)
        } else {
            SyncResult.PartialSuccess(synced = syncedCount, errors = errors)
        }
    }

    /**
     * Health Connectから水分記録を読み込む
     */
    suspend fun readFromHealthConnect(
        startTime: Instant,
        endTime: Instant
    ): List<HydrationRecord> {
        val client = healthConnectManager.getClient() ?: return emptyList()

        if (!healthConnectManager.hasAllPermissions()) {
            return emptyList()
        }

        return try {
            val request = ReadRecordsRequest(
                recordType = HydrationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            client.readRecords(request).records
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * ローカル記録をHealth Connect形式に変換
     */
    private fun createHealthConnectRecord(record: LocalHydrationRecord): HydrationRecord {
        val zoneId = ZoneId.systemDefault()
        val startTime = record.recordedAt
        val endTime = record.recordedAt.plusSeconds(1)

        return HydrationRecord(
            startTime = startTime,
            startZoneOffset = zoneId.rules.getOffset(startTime),
            endTime = endTime,
            endZoneOffset = zoneId.rules.getOffset(endTime),
            volume = Volume.milliliters(record.amountMl.toDouble())
        )
    }
}

sealed class SyncResult {
    data class Success(val synced: Int) : SyncResult()
    data class PartialSuccess(val synced: Int, val errors: List<String>) : SyncResult()
    data object NotAvailable : SyncResult()
    data object PermissionRequired : SyncResult()
}
