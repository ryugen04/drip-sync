package com.dripsync.shared.data.repository

import com.dripsync.shared.data.model.BeverageType
import com.dripsync.shared.data.model.HydrationRecord
import com.dripsync.shared.data.model.SourceDevice
import com.dripsync.shared.domain.model.DailyHydrationSummary
import com.dripsync.shared.domain.model.Hydration
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HydrationRepository {

    // === 記録操作 ===

    suspend fun recordHydration(
        amountMl: Int,
        beverageType: BeverageType = BeverageType.WATER,
        sourceDevice: SourceDevice = SourceDevice.UNKNOWN
    ): String

    suspend fun updateRecord(record: HydrationRecord)

    suspend fun deleteRecord(id: String)

    // === リアクティブ取得 ===

    fun observeTodayRecords(): Flow<List<Hydration>>

    fun observeTodayTotal(): Flow<Int>

    fun observeDailySummary(date: LocalDate): Flow<DailyHydrationSummary>

    fun observeRecordsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Hydration>>

    // === ワンショット取得 ===

    suspend fun getTodayRecords(): List<Hydration>

    suspend fun getTodayTotal(): Int

    suspend fun getRecordById(id: String): Hydration?

    suspend fun getRecordsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Hydration>

    // === Health Connect 同期 ===

    suspend fun getUnsyncedRecords(): List<HydrationRecord>

    suspend fun markAsSynced(id: String, healthConnectId: String)

    fun observeUnsyncedCount(): Flow<Int>
}
