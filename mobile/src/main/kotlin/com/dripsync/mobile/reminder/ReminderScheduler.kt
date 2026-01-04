package com.dripsync.mobile.reminder

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dripsync.shared.data.preferences.ReminderSettings
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun scheduleReminder(settings: ReminderSettings) {
        // 既存のワークをキャンセル
        workManager.cancelUniqueWork(ReminderWorker.WORK_NAME)

        // リマインダーが無効の場合はスケジュールしない
        if (!settings.isEnabled) {
            return
        }

        // WorkManagerの最小間隔は15分
        // ユーザー設定の間隔（1〜3時間）でスケジュール
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            settings.intervalHours.toLong(),
            TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelReminder() {
        workManager.cancelUniqueWork(ReminderWorker.WORK_NAME)
    }
}
