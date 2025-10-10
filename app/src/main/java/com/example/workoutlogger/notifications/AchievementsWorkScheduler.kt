package com.example.workoutlogger.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Singleton
class AchievementsWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {

    private val timeZone: TimeZone = TimeZone.currentSystemDefault()

    fun ensureScheduled() {
        val delayMillis = computeInitialDelay()
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()
        val request = PeriodicWorkRequestBuilder<EvaluateAchievementsWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            EvaluateAchievementsWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun computeInitialDelay(): Long {
        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(timeZone)
        val todayAtTwo = LocalDateTime(localDateTime.date, LocalTime(hour = 2, minute = 0))
        val firstRun = if (localDateTime < todayAtTwo) {
            todayAtTwo
        } else {
            val nextDate = localDateTime.date.plus(1, DateTimeUnit.DAY)
            LocalDateTime(nextDate, LocalTime(hour = 2, minute = 0))
        }
        val firstRunInstant = firstRun.toInstant(timeZone)
        val delay = firstRunInstant.toEpochMilliseconds() - now.toEpochMilliseconds()
        return delay.coerceAtLeast(MIN_DELAY_MILLIS)
    }

    companion object {
        private const val MIN_DELAY_MILLIS = 15L * 60L * 1000L
    }
}
