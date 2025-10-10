package com.example.workoutlogger

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.workoutlogger.notifications.AchievementNotificationHelper
import com.example.workoutlogger.notifications.AchievementNotificationsObserver
import com.example.workoutlogger.notifications.AchievementsWorkScheduler
import com.example.workoutlogger.notifications.WorkoutNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WorkoutLoggerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: WorkoutNotificationHelper
    @Inject lateinit var achievementsWorkScheduler: AchievementsWorkScheduler
    @Inject lateinit var achievementNotificationHelper: AchievementNotificationHelper
    @Inject lateinit var achievementNotificationsObserver: AchievementNotificationsObserver

    override fun onCreate() {
        super.onCreate()
        notificationHelper.ensureChannel()
        achievementNotificationHelper.ensureChannel()
        achievementsWorkScheduler.ensureScheduled()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
