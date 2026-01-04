package com.dripsync.shared.data.preferences

/**
 * リマインダー設定
 *
 * 目標未達成時に通知を送信するための設定
 */
data class ReminderSettings(
    val isEnabled: Boolean = false,
    val startHour: Int = 8,      // 通知開始時刻（時）
    val endHour: Int = 21,       // 通知終了時刻（時）
    val intervalHours: Int = 2   // 通知間隔（1, 2, 3時間）
) {
    /**
     * 現在時刻が通知可能な時間帯かどうか
     */
    fun isWithinActiveHours(currentHour: Int): Boolean {
        return currentHour in startHour until endHour
    }

    companion object {
        val INTERVAL_OPTIONS = listOf(1, 2, 3)
    }
}
