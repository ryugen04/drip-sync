package com.dripsync.shared.util

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DateTimeUtilsTest {

    private lateinit var dateTimeUtils: DateTimeUtils

    @Before
    fun setup() {
        dateTimeUtils = DateTimeUtils()
    }

    @Test
    fun `startOfDay returns midnight of given date`() {
        val date = LocalDate.of(2024, 6, 15)
        val startMillis = dateTimeUtils.startOfDay(date)

        val instant = Instant.ofEpochMilli(startMillis)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()

        assertThat(localDateTime.toLocalDate()).isEqualTo(date)
        assertThat(localDateTime.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT)
    }

    @Test
    fun `endOfDay returns midnight of next day`() {
        val date = LocalDate.of(2024, 6, 15)
        val endMillis = dateTimeUtils.endOfDay(date)

        val instant = Instant.ofEpochMilli(endMillis)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()

        assertThat(localDateTime.toLocalDate()).isEqualTo(date.plusDays(1))
        assertThat(localDateTime.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT)
    }

    @Test
    fun `getDateRange returns correct start and end`() {
        val date = LocalDate.of(2024, 6, 15)
        val (start, end) = dateTimeUtils.getDateRange(date)

        assertThat(start).isLessThan(end)
        assertThat(end - start).isEqualTo(24 * 60 * 60 * 1000L) // 24時間
    }

    @Test
    fun `toLocalDate converts Instant correctly`() {
        val date = LocalDate.of(2024, 6, 15)
        val instant = date.atTime(12, 30).atZone(ZoneId.systemDefault()).toInstant()

        val result = dateTimeUtils.toLocalDate(instant)

        assertThat(result).isEqualTo(date)
    }

    @Test
    fun `toInstant converts LocalDate correctly`() {
        val date = LocalDate.of(2024, 6, 15)
        val time = LocalTime.of(14, 30)

        val result = dateTimeUtils.toInstant(date, time)

        val localDateTime = result.atZone(ZoneId.systemDefault()).toLocalDateTime()
        assertThat(localDateTime.toLocalDate()).isEqualTo(date)
        assertThat(localDateTime.toLocalTime()).isEqualTo(time)
    }

    @Test
    fun `toInstant with default time uses midnight`() {
        val date = LocalDate.of(2024, 6, 15)

        val result = dateTimeUtils.toInstant(date)

        val localDateTime = result.atZone(ZoneId.systemDefault()).toLocalDateTime()
        assertThat(localDateTime.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT)
    }
}
