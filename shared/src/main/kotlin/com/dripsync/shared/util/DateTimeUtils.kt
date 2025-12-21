package com.dripsync.shared.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateTimeUtils @Inject constructor() {

    private val zoneId: ZoneId
        get() = ZoneId.systemDefault()

    /**
     * 今日の日付範囲（ミリ秒）を取得
     */
    fun getTodayRange(): Pair<Long, Long> {
        return getDateRange(LocalDate.now())
    }

    /**
     * 指定日の日付範囲（ミリ秒）を取得
     */
    fun getDateRange(date: LocalDate): Pair<Long, Long> {
        val start = startOfDay(date)
        val end = endOfDay(date)
        return Pair(start, end)
    }

    /**
     * 指定日の開始時刻（ミリ秒）
     */
    fun startOfDay(date: LocalDate): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    /**
     * 指定日の終了時刻（ミリ秒）
     * 翌日の00:00:00.000（排他的境界）
     */
    fun endOfDay(date: LocalDate): Long {
        return date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    /**
     * Instant から LocalDate への変換
     */
    fun toLocalDate(instant: Instant): LocalDate {
        return instant.atZone(zoneId).toLocalDate()
    }

    /**
     * LocalDate + LocalTime から Instant への変換
     */
    fun toInstant(date: LocalDate, time: LocalTime = LocalTime.MIDNIGHT): Instant {
        return date.atTime(time).atZone(zoneId).toInstant()
    }
}
