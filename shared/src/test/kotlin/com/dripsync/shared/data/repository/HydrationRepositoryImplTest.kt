package com.dripsync.shared.data.repository

import app.cash.turbine.test
import com.dripsync.shared.data.local.HydrationDao
import com.dripsync.shared.data.model.BeverageType
import com.dripsync.shared.data.model.HydrationRecord
import com.dripsync.shared.data.model.SourceDevice
import com.dripsync.shared.util.DateTimeUtils
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HydrationRepositoryImplTest {

    private lateinit var hydrationDao: HydrationDao
    private lateinit var dateTimeUtils: DateTimeUtils
    private lateinit var repository: HydrationRepositoryImpl

    private val todayStart = 1718409600000L // 2024-06-15 00:00:00
    private val todayEnd = 1718496000000L   // 2024-06-16 00:00:00

    @Before
    fun setup() {
        hydrationDao = mockk(relaxed = true)
        dateTimeUtils = mockk()

        every { dateTimeUtils.getTodayRange() } returns Pair(todayStart, todayEnd)
        every { dateTimeUtils.getDateRange(any()) } answers {
            val date = firstArg<LocalDate>()
            val start = date.toEpochDay() * 86400000L
            Pair(start, start + 86400000L)
        }
        every { dateTimeUtils.startOfDay(any()) } answers {
            val date = firstArg<LocalDate>()
            date.toEpochDay() * 86400000L
        }
        every { dateTimeUtils.endOfDay(any()) } answers {
            val date = firstArg<LocalDate>()
            (date.toEpochDay() + 1) * 86400000L
        }

        repository = HydrationRepositoryImpl(hydrationDao, dateTimeUtils)
    }

    @Test
    fun `recordHydration inserts record and returns id`() = runTest {
        val recordSlot = slot<HydrationRecord>()
        coEvery { hydrationDao.insert(capture(recordSlot)) } returns Unit

        val id = repository.recordHydration(
            amountMl = 250,
            beverageType = BeverageType.WATER,
            sourceDevice = SourceDevice.MOBILE
        )

        assertThat(id).isNotEmpty()
        assertThat(recordSlot.captured.amountMl).isEqualTo(250)
        assertThat(recordSlot.captured.beverageType).isEqualTo(BeverageType.WATER)
        assertThat(recordSlot.captured.sourceDevice).isEqualTo(SourceDevice.MOBILE)
    }

    @Test
    fun `recordHydration uses default values`() = runTest {
        val recordSlot = slot<HydrationRecord>()
        coEvery { hydrationDao.insert(capture(recordSlot)) } returns Unit

        repository.recordHydration(amountMl = 200)

        assertThat(recordSlot.captured.beverageType).isEqualTo(BeverageType.WATER)
        assertThat(recordSlot.captured.sourceDevice).isEqualTo(SourceDevice.UNKNOWN)
    }

    @Test
    fun `deleteRecord calls dao deleteById`() = runTest {
        val testId = "test-id-123"
        coEvery { hydrationDao.deleteById(testId) } returns Unit

        repository.deleteRecord(testId)

        coVerify { hydrationDao.deleteById(testId) }
    }

    @Test
    fun `observeTodayTotal emits correct values`() = runTest {
        every { hydrationDao.observeTotalByDateRange(todayStart, todayEnd) } returns flowOf(500, 750, 1000)

        repository.observeTodayTotal().test {
            assertThat(awaitItem()).isEqualTo(500)
            assertThat(awaitItem()).isEqualTo(750)
            assertThat(awaitItem()).isEqualTo(1000)
            awaitComplete()
        }
    }

    @Test
    fun `observeTodayRecords maps records to domain`() = runTest {
        val testRecord = HydrationRecord(
            id = "test-1",
            amountMl = 250,
            beverageType = BeverageType.COFFEE,
            sourceDevice = SourceDevice.WEAR,
            recordedAt = Instant.ofEpochMilli(todayStart + 3600000)
        )
        every { hydrationDao.observeByDateRange(todayStart, todayEnd) } returns flowOf(listOf(testRecord))

        repository.observeTodayRecords().test {
            val records = awaitItem()
            assertThat(records).hasSize(1)
            assertThat(records[0].id).isEqualTo("test-1")
            assertThat(records[0].amountMl).isEqualTo(250)
            assertThat(records[0].beverageType).isEqualTo(BeverageType.COFFEE)
            awaitComplete()
        }
    }

    @Test
    fun `getTodayTotal returns sum from dao`() = runTest {
        coEvery { hydrationDao.getTotalByDateRange(todayStart, todayEnd) } returns 1500

        val total = repository.getTodayTotal()

        assertThat(total).isEqualTo(1500)
    }

    @Test
    fun `getRecordById returns null when not found`() = runTest {
        coEvery { hydrationDao.getById("non-existent") } returns null

        val result = repository.getRecordById("non-existent")

        assertThat(result).isNull()
    }

    @Test
    fun `getRecordById returns mapped domain when found`() = runTest {
        val testRecord = HydrationRecord(
            id = "test-1",
            amountMl = 350,
            beverageType = BeverageType.TEA
        )
        coEvery { hydrationDao.getById("test-1") } returns testRecord

        val result = repository.getRecordById("test-1")

        assertThat(result).isNotNull()
        assertThat(result?.amountMl).isEqualTo(350)
        assertThat(result?.beverageType).isEqualTo(BeverageType.TEA)
    }

    @Test
    fun `getUnsyncedRecords returns dao result`() = runTest {
        val unsyncedRecords = listOf(
            HydrationRecord(id = "1", amountMl = 100),
            HydrationRecord(id = "2", amountMl = 200)
        )
        coEvery { hydrationDao.getUnsyncedRecords() } returns unsyncedRecords

        val result = repository.getUnsyncedRecords()

        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo("1")
    }

    @Test
    fun `markAsSynced calls dao with correct params`() = runTest {
        coEvery { hydrationDao.markAsSynced("test-id", "hc-id-123") } returns Unit

        repository.markAsSynced("test-id", "hc-id-123")

        coVerify { hydrationDao.markAsSynced("test-id", "hc-id-123") }
    }

    @Test
    fun `observeUnsyncedCount returns dao flow`() = runTest {
        every { hydrationDao.observeUnsyncedCount() } returns flowOf(5, 3, 0)

        repository.observeUnsyncedCount().test {
            assertThat(awaitItem()).isEqualTo(5)
            assertThat(awaitItem()).isEqualTo(3)
            assertThat(awaitItem()).isEqualTo(0)
            awaitComplete()
        }
    }
}
