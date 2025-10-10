package com.example.workoutlogger.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.workoutlogger.MainActivity
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.achievements.AchievementEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val ACHIEVEMENT_CHANNEL_ID = "achievements"
private const val ACHIEVEMENT_NOTIFICATION_BASE = 2000

@Singleton
class AchievementNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ACHIEVEMENT_CHANNEL_ID,
                context.getString(R.string.notification_achievement_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_achievement_channel_description)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun showEvent(event: AchievementEvent) {
        when (event) {
            is AchievementEvent.Completed -> showAchievementUnlocked(event)
            is AchievementEvent.GoalDeadlineApproaching -> showGoalReminder(event)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showAchievementUnlocked(event: AchievementEvent.Completed) {
        val title = context.getString(R.string.notification_achievement_unlocked_title, event.title)
        val body = context.getString(R.string.notification_achievement_unlocked_body)
        showNotification(event.instanceId.hashCode(), title, body)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showGoalReminder(event: AchievementEvent.GoalDeadlineApproaching) {
        val formattedDeadline = DateFormat.format(
            context.getString(R.string.notification_goal_deadline_format),
            event.deadlineAt.toEpochMilliseconds()
        )
        val title = context.getString(R.string.notification_goal_deadline_title, event.title)
        val body = context.getString(R.string.notification_goal_deadline_body, formattedDeadline)
        showNotification(event.instanceId.hashCode(), title, body)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(idSeed: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            idSeed,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ACHIEVEMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            ACHIEVEMENT_NOTIFICATION_BASE + idSeed,
            notification
        )
    }
}
