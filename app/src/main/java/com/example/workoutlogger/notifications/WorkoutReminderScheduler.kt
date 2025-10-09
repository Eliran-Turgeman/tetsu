package com.example.workoutlogger.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.workoutlogger.domain.repository.WorkoutRepository
import com.example.workoutlogger.domain.usecase.schedule.CalculateNextScheduleRunUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val UNIQUE_WORK_PREFIX = "template_reminder_"

@Singleton
class WorkoutReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val alarmManager: AlarmManager,
    private val workoutRepository: WorkoutRepository,
    private val calculateNextScheduleRunUseCase: CalculateNextScheduleRunUseCase,
    private val notificationHelper: WorkoutNotificationHelper
) {

    suspend fun scheduleTemplate(templateId: Long) {
        val schedule = workoutRepository.getScheduleForWorkout(templateId)
        val workout = workoutRepository.getWorkout(templateId)
        if (schedule == null || workout == null || !schedule.enabled || schedule.daysOfWeek.isEmpty()) {
            cancel(templateId)
            return
        }
        val now = Clock.System.now()
        val nextInstant = calculateNextScheduleRunUseCase(
            daysOfWeek = schedule.daysOfWeek,
            hour = schedule.notifyHour,
            minute = schedule.notifyMinute,
            now = now
        ) ?: run {
            cancel(templateId)
            return
        }

        val delayMillis = (nextInstant.asEpochMillis() - now.asEpochMillis()).coerceAtLeast(0)
        val workRequest = OneTimeWorkRequestBuilder<TemplateReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    TemplateReminderWorker.KEY_TEMPLATE_ID to templateId,
                    TemplateReminderWorker.KEY_TEMPLATE_NAME to workout.name
                )
            )
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_PREFIX + templateId,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        scheduleAlarm(templateId, workout.name, nextInstant.asEpochMillis())
    }

    suspend fun cancel(templateId: Long) {
        workManager.cancelUniqueWork(UNIQUE_WORK_PREFIX + templateId)
        val pendingIntent = buildAlarmPendingIntent(templateId, null)
        alarmManager.cancel(pendingIntent)
    }

    suspend fun scheduleAll() {
        workoutRepository.observeSchedules().first().forEach { schedule ->
            if (schedule.enabled) {
                scheduleTemplate(schedule.workoutId)
            } else {
                cancel(schedule.workoutId)
            }
        }
    }

    suspend fun handleTrigger(templateId: Long, templateName: String) {
        notificationHelper.ensureChannel()
        notificationHelper.showWorkoutReminder(templateId, templateName)
        scheduleTemplate(templateId)
    }

    private fun scheduleAlarm(templateId: Long, templateName: String, triggerAtMillis: Long) {
        val pendingIntent = buildAlarmPendingIntent(templateId, templateName)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun buildAlarmPendingIntent(templateId: Long, templateName: String?): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_TEMPLATE_ID, templateId)
            if (templateName != null) {
                putExtra(ReminderAlarmReceiver.EXTRA_TEMPLATE_NAME, templateName)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            templateId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

private fun Instant.asEpochMillis(): Long =
    epochSeconds * 1_000 + nanosecondsOfSecond / 1_000_000
