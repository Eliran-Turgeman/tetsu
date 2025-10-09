package com.example.workoutlogger.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TemplateReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderScheduler: WorkoutReminderScheduler,
    private val notificationHelper: WorkoutNotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val templateId = inputData.getLong(KEY_TEMPLATE_ID, -1L)
        val templateName = inputData.getString(KEY_TEMPLATE_NAME)
        if (templateId <= 0 || templateName.isNullOrBlank()) {
            return Result.failure()
        }
        notificationHelper.ensureChannel()
        notificationHelper.showWorkoutReminder(templateId, templateName)
        reminderScheduler.scheduleTemplate(templateId)
        return Result.success()
    }

    companion object {
        const val KEY_TEMPLATE_ID = "template_id"
        const val KEY_TEMPLATE_NAME = "template_name"
    }
}
