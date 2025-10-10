package com.example.workoutlogger.domain.usecase.heatmap

import com.example.workoutlogger.domain.model.HeatmapEntry
import com.example.workoutlogger.domain.model.WorkoutStatus
import com.example.workoutlogger.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Produces a rolling 12-month heatmap summary with a boolean marker per day.
 */
class ObserveHeatmapUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(timeZone: TimeZone = TimeZone.currentSystemDefault()): Flow<List<HeatmapEntry>> {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val start = today.minus(12, DateTimeUnit.MONTH)
        val sessionsFlow = sessionRepository.observeSessionsByDateRange(start, today)
        return sessionsFlow.map { sessions ->
            val completedByDate = sessions
                .filter { it.status == WorkoutStatus.COMPLETED }
                .groupBy { it.endedAt?.toLocalDateTime(timeZone)?.date ?: it.startedAt.toLocalDateTime(timeZone).date }
            generateDateSequence(start, today).map { date ->
                HeatmapEntry(
                    date = date,
                    hasCompletedSession = completedByDate[date]?.isNotEmpty() == true
                )
            }.toList()
        }
    }

    private fun generateDateSequence(start: LocalDate, endInclusive: LocalDate): Sequence<LocalDate> = sequence {
        var current = start
        while (current <= endInclusive) {
            yield(current)
            current = current.plus(1, DateTimeUnit.DAY)
        }
    }
}
