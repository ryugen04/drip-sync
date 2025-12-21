package com.dripsync.shared.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.dripsync.shared.data.model.HydrationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationDao {

    // === 挿入・更新 ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HydrationRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HydrationRecord>)

    @Upsert
    suspend fun upsert(record: HydrationRecord)

    @Update
    suspend fun update(record: HydrationRecord)

    // === 削除 ===

    @Delete
    suspend fun delete(record: HydrationRecord)

    @Query("DELETE FROM hydration_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM hydration_records")
    suspend fun deleteAll()

    // === リアクティブクエリ（Flow） ===

    @Query("SELECT * FROM hydration_records ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<HydrationRecord>>

    @Query("SELECT * FROM hydration_records WHERE id = :id")
    fun observeById(id: String): Flow<HydrationRecord?>

    @Query(
        """
        SELECT * FROM hydration_records
        WHERE recordedAt >= :startMillis AND recordedAt < :endMillis
        ORDER BY recordedAt DESC
    """
    )
    fun observeByDateRange(startMillis: Long, endMillis: Long): Flow<List<HydrationRecord>>

    @Query(
        """
        SELECT COALESCE(SUM(amountMl), 0) FROM hydration_records
        WHERE recordedAt >= :startMillis AND recordedAt < :endMillis
    """
    )
    fun observeTotalByDateRange(startMillis: Long, endMillis: Long): Flow<Int>

    // === ワンショットクエリ ===

    @Query("SELECT * FROM hydration_records ORDER BY recordedAt DESC")
    suspend fun getAll(): List<HydrationRecord>

    @Query("SELECT * FROM hydration_records WHERE id = :id")
    suspend fun getById(id: String): HydrationRecord?

    @Query(
        """
        SELECT * FROM hydration_records
        WHERE recordedAt >= :startMillis AND recordedAt < :endMillis
        ORDER BY recordedAt DESC
    """
    )
    suspend fun getByDateRange(startMillis: Long, endMillis: Long): List<HydrationRecord>

    @Query(
        """
        SELECT COALESCE(SUM(amountMl), 0) FROM hydration_records
        WHERE recordedAt >= :startMillis AND recordedAt < :endMillis
    """
    )
    suspend fun getTotalByDateRange(startMillis: Long, endMillis: Long): Int

    // === Health Connect 同期用 ===

    @Query("SELECT * FROM hydration_records WHERE syncedToHealthConnect = 0")
    suspend fun getUnsyncedRecords(): List<HydrationRecord>

    @Query("UPDATE hydration_records SET syncedToHealthConnect = 1, healthConnectId = :healthConnectId WHERE id = :id")
    suspend fun markAsSynced(id: String, healthConnectId: String)

    @Query("SELECT COUNT(*) FROM hydration_records WHERE syncedToHealthConnect = 0")
    fun observeUnsyncedCount(): Flow<Int>
}
