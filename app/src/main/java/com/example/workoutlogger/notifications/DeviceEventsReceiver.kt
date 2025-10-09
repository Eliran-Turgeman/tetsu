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
class DeviceEventsReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderScheduler: WorkoutReminderScheduler

    override fun onReceive(context: Context?, intent: Intent?) {
        CoroutineScope(Dispatchers.Default).launch {
            reminderScheduler.scheduleAll()
        }
    }
}
