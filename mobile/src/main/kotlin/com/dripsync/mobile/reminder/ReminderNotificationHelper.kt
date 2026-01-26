package com.dripsync.mobile.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dripsync.mobile.MainActivity
import com.dripsync.mobile.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ReminderNotification"
        const val CHANNEL_ID = "hydration_reminder"
        const val CHANNEL_NAME = "水分補給リマインダー"
        const val CHANNEL_DESCRIPTION = "目標未達成時に水分補給を促すリマインダー通知"
        const val NOTIFICATION_ID = 1001
    }

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showReminderNotification(currentMl: Int, goalMl: Int) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "通知権限がないためリマインダーを表示できません")
            return
        }

        val remaining = goalMl - currentMl
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dripsync_logo)
            .setContentTitle("水分補給のお知らせ")
            .setContentText("あと${formatAmount(remaining)}で目標達成です")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("今日の水分摂取量: ${formatAmount(currentMl)} / ${formatAmount(goalMl)}\nあと${formatAmount(remaining)}で目標達成です")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun formatAmount(ml: Int): String {
        return if (ml >= 1000) {
            String.format("%.1fL", ml / 1000f)
        } else {
            "${ml}ml"
        }
    }
}
