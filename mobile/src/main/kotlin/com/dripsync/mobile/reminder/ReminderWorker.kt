package com.dripsync.mobile.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import com.dripsync.shared.data.repository.HydrationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalTime

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val hydrationRepository: HydrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationHelper: ReminderNotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "hydration_reminder_worker"
    }

    override suspend fun doWork(): Result {
        val settings = userPreferencesRepository.getReminderSettings()

        // リマインダーが無効の場合は何もしない
        if (!settings.isEnabled) {
            return Result.success()
        }

        // 現在時刻が有効時間帯かチェック
        val currentHour = LocalTime.now().hour
        if (!settings.isWithinActiveHours(currentHour)) {
            return Result.success()
        }

        // 今日の摂取量と目標を取得
        val goal = userPreferencesRepository.getDailyGoal()
        val current = hydrationRepository.getTodayTotal()

        // 目標未達成の場合のみ通知
        if (current < goal) {
            notificationHelper.showReminderNotification(current, goal)
        }

        return Result.success()
    }
}
