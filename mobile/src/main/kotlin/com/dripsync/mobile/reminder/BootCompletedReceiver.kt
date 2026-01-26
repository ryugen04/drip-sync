package com.dripsync.mobile.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dripsync.shared.data.preferences.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * デバイス再起動後にリマインダーを再スケジュールするReceiver
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.d(TAG, "デバイス再起動を検出、リマインダー設定を確認中")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = userPreferencesRepository.getReminderSettings()
                if (settings.isEnabled) {
                    reminderScheduler.scheduleReminder(settings)
                    Log.d(TAG, "リマインダーを再スケジュールしました: ${settings.intervalHours}時間間隔")
                } else {
                    Log.d(TAG, "リマインダーは無効のためスケジュールをスキップ")
                }
            } catch (e: Exception) {
                Log.e(TAG, "リマインダーの再スケジュールに失敗", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
