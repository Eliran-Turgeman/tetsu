package com.example.workoutlogger.domain.usecase.schedule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CalculateNextScheduleRunUseCaseTest {
    private val useCase = CalculateNextScheduleRunUseCase()
    private val zone = TimeZone.UTC

    @Test
    fun `returns today when time in future`() {
        val now = LocalDate(2024, 5, 1).atStartOfDayIn(zone) + 2.hours
        val result = useCase(
            daysOfWeek = setOf(DayOfWeek.WEDNESDAY),
            hour = 12,
            minute = 0,
            now = now,
            timeZone = zone
        )
        val expected = LocalDate(2024, 5, 1).atStartOfDayIn(zone) + 12.hours
        assertEquals(expected, result)
    }

    @Test
    fun `rolls forward to next scheduled day`() {
        val now = LocalDate(2024, 5, 1).atStartOfDayIn(zone) + 16.hours
        val result = useCase(
            daysOfWeek = setOf(DayOfWeek.THURSDAY),
            hour = 8,
            minute = 30,
            now = now,
            timeZone = zone
        )
        val expected = LocalDate(2024, 5, 2).atStartOfDayIn(zone) + 8.hours + 30.minutes
        assertEquals(expected, result)
    }

    @Test
    fun `returns null when no days`() {
        val now = Clock.System.now()
        val result = useCase(emptySet(), 8, 0, now)
        assertNull(result)
    }
}
