package com.dripsync.shared.domain.model

import java.time.LocalTime

/**
 * 理想的な水分摂取スケジュールを定義するオブジェクト
 *
 * 科学的知見に基づく推奨タイミング:
 * - 起床直後: 就寝中の脱水補充
 * - 食事時: 自然な摂取タイミング
 * - 入浴前後: 脱水予防・補充
 * - 就寝前: 夜間脱水予防（少量に抑える）
 */
object IdealHydrationSchedule {

    /**
     * 時刻と理想累積摂取量のペア
     * 累積値は1500ml/日を基準としている
     */
    val schedule: List<Pair<LocalTime, Int>> = listOf(
        LocalTime.of(7, 0) to 200,      // 起床直後: 200ml
        LocalTime.of(8, 0) to 350,      // 朝食時: +150ml
        LocalTime.of(10, 0) to 550,     // 午前の休憩: +200ml
        LocalTime.of(12, 0) to 700,     // 昼食時: +150ml
        LocalTime.of(15, 0) to 900,     // 午後の休憩: +200ml
        LocalTime.of(18, 0) to 1050,    // 夕食時: +150ml
        LocalTime.of(19, 30) to 1200,   // 入浴前: +150ml
        LocalTime.of(20, 30) to 1350,   // 入浴後: +150ml
        LocalTime.of(22, 0) to 1450     // 就寝前: +100ml
    )

    val startTime: LocalTime = LocalTime.of(6, 0)
    val endTime: LocalTime = LocalTime.of(23, 0)

    /**
     * 指定時刻での理想累積摂取量を線形補間で計算
     *
     * @param time 現在時刻
     * @return 理想累積摂取量（ml）
     */
    fun getIdealAmountAt(time: LocalTime): Int {
        // 開始時刻より前は0
        if (time.isBefore(schedule.first().first)) {
            return 0
        }

        // 終了時刻以降は最終値
        if (!time.isBefore(schedule.last().first)) {
            return schedule.last().second
        }

        // 線形補間
        for (i in 0 until schedule.size - 1) {
            val (t1, v1) = schedule[i]
            val (t2, v2) = schedule[i + 1]

            if (!time.isBefore(t1) && time.isBefore(t2)) {
                val totalSeconds = t2.toSecondOfDay() - t1.toSecondOfDay()
                val elapsedSeconds = time.toSecondOfDay() - t1.toSecondOfDay()
                val ratio = elapsedSeconds.toFloat() / totalSeconds
                return v1 + ((v2 - v1) * ratio).toInt()
            }
        }

        return schedule.last().second
    }

    /**
     * 目標値に合わせてスケールした理想累積摂取量を計算
     *
     * @param time 現在時刻
     * @param goalMl 1日の目標摂取量
     * @return スケールされた理想累積摂取量（ml）
     */
    fun getIdealAmountAt(time: LocalTime, goalMl: Int): Int {
        val baseAmount = getIdealAmountAt(time)
        val baseGoal = schedule.last().second // 1450ml
        return (baseAmount.toFloat() * goalMl / baseGoal).toInt()
    }

    /**
     * グラフ描画用に時間ポイントのリストを生成
     *
     * @param intervalMinutes ポイント間隔（分）
     * @param goalMl 1日の目標摂取量
     * @return 時刻と理想累積量のペアのリスト
     */
    fun generateIdealCurvePoints(
        intervalMinutes: Int = 30,
        goalMl: Int = 1500
    ): List<Pair<LocalTime, Int>> {
        val points = mutableListOf<Pair<LocalTime, Int>>()
        var current = startTime

        while (!current.isAfter(endTime)) {
            points.add(current to getIdealAmountAt(current, goalMl))
            current = current.plusMinutes(intervalMinutes.toLong())
        }

        return points
    }
}
