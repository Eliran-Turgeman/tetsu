package com.example.workoutlogger.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderScheduler: WorkoutReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, -1L)
        val templateName = intent.getStringExtra(EXTRA_TEMPLATE_NAME)
        if (templateId <= 0 || templateName.isNullOrBlank()) return

        CoroutineScope(Dispatchers.Default).launch {
            reminderScheduler.handleTrigger(templateId, templateName)
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.example.workoutlogger.ACTION_REMINDER"
        const val EXTRA_TEMPLATE_ID = "extra_template_id"
        const val EXTRA_TEMPLATE_NAME = "extra_template_name"
    }
}
