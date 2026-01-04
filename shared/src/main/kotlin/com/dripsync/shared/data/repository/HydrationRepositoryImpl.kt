package com.dripsync.shared.data.repository

import com.dripsync.shared.data.local.HydrationDao
import com.dripsync.shared.data.model.BeverageType
import com.dripsync.shared.data.model.HydrationRecord
import com.dripsync.shared.data.model.SourceDevice
import com.dripsync.shared.domain.model.DailyHydrationSummary
import com.dripsync.shared.domain.model.Hydration
import com.dripsync.shared.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrationRepositoryImpl @Inject constructor(
    private val hydrationDao: HydrationDao,
    private val dateTimeUtils: DateTimeUtils
) : HydrationRepository {

    companion object {
        // デフォルトの1日の目標摂取量
        private const val DEFAULT_DAILY_GOAL_ML = 2000
    }

    // === 記録操作 ===

    override suspend fun recordHydration(
        amountMl: Int,
        beverageType: BeverageType,
        sourceDevice: SourceDevice,
        recordId: String?,
        recordedAt: Instant?
    ): String {
        val record = HydrationRecord(
            id = recordId ?: java.util.UUID.randomUUID().toString(),
            amountMl = amountMl,
            beverageType = beverageType,
            sourceDevice = sourceDevice,
            recordedAt = recordedAt ?: Instant.now()
        )
        hydrationDao.insert(record)
        return record.id
    }

    override suspend fun updateRecord(record: HydrationRecord) {
        hydrationDao.update(record)
    }

    override suspend fun deleteRecord(id: String) {
        hydrationDao.deleteById(id)
    }

    // === リアクティブ取得 ===

    override fun observeTodayRecords(): Flow<List<Hydration>> {
        val (start, end) = dateTimeUtils.getTodayRange()
        return hydrationDao.observeByDateRange(start, end)
            .map { records -> records.map { it.toDomain() } }
    }

    override fun observeTodayTotal(): Flow<Int> {
        val (start, end) = dateTimeUtils.getTodayRange()
        return hydrationDao.observeTotalByDateRange(start, end)
    }

    override fun observeDailySummary(date: LocalDate): Flow<DailyHydrationSummary> {
        val (start, end) = dateTimeUtils.getDateRange(date)
        return combine(
            hydrationDao.observeByDateRange(start, end),
            hydrationDao.observeTotalByDateRange(start, end)
        ) { records, total ->
            DailyHydrationSummary(
                date = date,
                totalAmountMl = total,
                goalMl = DEFAULT_DAILY_GOAL_ML,
                records = records.map { it.toDomain() }
            )
        }
    }

    override fun observeRecordsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<Hydration>> {
        val start = dateTimeUtils.startOfDay(startDate)
        val end = dateTimeUtils.endOfDay(endDate)
        return hydrationDao.observeByDateRange(start, end)
            .map { records -> records.map { it.toDomain() } }
    }

    // === ワンショット取得 ===

    override suspend fun getTodayRecords(): List<Hydration> {
        val (start, end) = dateTimeUtils.getTodayRange()
        return hydrationDao.getByDateRange(start, end).map { it.toDomain() }
    }

    override suspend fun getTodayTotal(): Int {
        val (start, end) = dateTimeUtils.getTodayRange()
        return hydrationDao.getTotalByDateRange(start, end)
    }

    override suspend fun getRecordById(id: String): Hydration? {
        return hydrationDao.getById(id)?.toDomain()
    }

    override suspend fun getRecordsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Hydration> {
        val start = dateTimeUtils.startOfDay(startDate)
        val end = dateTimeUtils.endOfDay(endDate)
        return hydrationDao.getByDateRange(start, end).map { it.toDomain() }
    }

    // === Health Connect 同期 ===

    override suspend fun getUnsyncedRecords(): List<HydrationRecord> {
        return hydrationDao.getUnsyncedRecords()
    }

    override suspend fun markAsSynced(id: String, healthConnectId: String) {
        hydrationDao.markAsSynced(id, healthConnectId)
    }

    override fun observeUnsyncedCount(): Flow<Int> {
        return hydrationDao.observeUnsyncedCount()
    }

    // === 変換 ===

    private fun HydrationRecord.toDomain(): Hydration {
        return Hydration(
            id = id,
            amountMl = amountMl,
            beverageType = beverageType,
            recordedAt = recordedAt,
            sourceDevice = sourceDevice,
            isSyncedToHealthConnect = syncedToHealthConnect
        )
    }
}
