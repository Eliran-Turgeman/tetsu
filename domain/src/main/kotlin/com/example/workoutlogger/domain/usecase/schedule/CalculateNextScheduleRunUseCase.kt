package com.example.workoutlogger.domain.usecase.schedule

import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * Calculates the next scheduled notification instant for a template schedule.
 */
class CalculateNextScheduleRunUseCase @Inject constructor() {

    operator fun invoke(
        daysOfWeek: Set<DayOfWeek>,
        hour: Int,
        minute: Int,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Instant? {
        if (daysOfWeek.isEmpty()) return null
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }

        val today = now.toLocalDateTime(timeZone).date

        // Walk upcoming days (two-week safety window) until we find a future occurrence.
        for (offset in 0..14) {
            val date = today.plus(offset, DateTimeUnit.DAY)
            if (date.dayOfWeek !in daysOfWeek) continue
            val candidate = date.atTime(hour, minute).toInstant(timeZone)
            if (candidate >= now) {
                return candidate
            }
        }

        // Fallback: pick the soonest scheduled day in the next cycle.
        val daysAhead = daysOfWeek.minOf { day ->
            val diff = (day.ordinal - today.dayOfWeek.ordinal + 7) % 7
            if (diff == 0) 7 else diff
        }
        val nextDate = today.plus(daysAhead, DateTimeUnit.DAY)
        return nextDate.atTime(hour, minute).toInstant(timeZone)
    }
}
