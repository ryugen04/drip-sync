package com.dripsync.shared.domain.model

import com.dripsync.shared.data.model.BeverageType
import com.dripsync.shared.data.model.SourceDevice
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * ドメイン層の水分摂取モデル
 */
data class Hydration(
    val id: String,
    val amountMl: Int,
    val beverageType: BeverageType,
    val recordedAt: Instant,
    val sourceDevice: SourceDevice,
    val isSyncedToHealthConnect: Boolean
) {
    // 記録日（ローカルタイムゾーン）
    val recordedDate: LocalDate
        get() = recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()

    // リットル単位での表示
    val amountLiters: Float
        get() = amountMl / 1000f

    // 表示用の量（例：200ml、1.5L）
    val displayAmount: String
        get() = if (amountMl >= 1000) {
            String.format("%.1fL", amountLiters)
        } else {
            "${amountMl}ml"
        }
}

/**
 * 1日の水分摂取サマリー
 */
data class DailyHydrationSummary(
    val date: LocalDate,
    val totalAmountMl: Int,
    val goalMl: Int,
    val records: List<Hydration>
) {
    // 目標達成率（0.0 - 1.0+）
    val progressRatio: Float
        get() = if (goalMl > 0) totalAmountMl.toFloat() / goalMl else 0f

    // 目標達成パーセント（0 - 100+）
    val progressPercent: Int
        get() = (progressRatio * 100).toInt()

    // 目標達成済みか
    val isGoalAchieved: Boolean
        get() = totalAmountMl >= goalMl

    // 残り必要量（目標未達成時のみ）
    val remainingMl: Int
        get() = maxOf(0, goalMl - totalAmountMl)

    // 残り必要量の表示
    val displayRemaining: String
        get() = if (remainingMl >= 1000) {
            String.format("%.1fL", remainingMl / 1000f)
        } else {
            "${remainingMl}ml"
        }
}
