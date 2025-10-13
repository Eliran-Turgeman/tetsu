package com.example.workoutlogger.domain.usecase.heatmap

import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WorkoutStatus
import com.example.workoutlogger.domain.repository.SessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

class ObserveHeatmapUseCaseTest {

    private val timeZone = TimeZone.UTC
    private val sessionsFlow = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val repository = object : SessionRepository {
        override fun observeActiveSession(): Flow<WorkoutSession?> = MutableStateFlow(null)
        override fun observeSession(sessionId: Long): Flow<WorkoutSession?> = MutableStateFlow(null)
        override fun observeSessionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkoutSession>> = sessionsFlow
        override suspend fun startSessionFromWorkout(workoutId: Long, startedAt: Instant): WorkoutSession = error("Not needed")
        override suspend fun startAdHocSession(name: String, startedAt: Instant): WorkoutSession = error("Not needed")
        override suspend fun upsertSessionExercise(sessionId: Long, exercise: SessionExercise): Long = error("Not needed")
        override suspend fun updateSessionExerciseOrder(sessionId: Long, exerciseIdsInOrder: List<Long>) = Unit
        override suspend fun deleteSessionExercise(exerciseId: Long) = Unit
        override suspend fun upsertSetLog(exerciseId: Long, setLog: SessionSetLog): Long = 0
        override suspend fun deleteSetLog(setLogId: Long) = Unit
        override suspend fun finishSession(sessionId: Long, endedAt: Instant) = Unit
        override suspend fun cancelSession(sessionId: Long) = Unit
        override suspend fun getPreviousPerformance(exerciseName: String, before: Instant) = null
    }

    private val useCase = ObserveHeatmapUseCase(repository)

    @Test
    fun `marks days with completed sessions`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val sessionDay = today.minus(1, DateTimeUnit.DAY)
        sessionsFlow.value = listOf(
            completedSession(onDate = sessionDay)
        )

        val entries = useCase(timeZone).first()
        val marked = entries.firstOrNull { it.date == sessionDay }
        assertTrue(marked?.hasCompletedSession == true)
    }

    @Test
    fun `marks completion using ended date when available`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val startedDay = today.minus(1, DateTimeUnit.DAY)
        val session = completedSession(
            onDate = startedDay,
            endedAt = today.atStartOfDayIn(timeZone) + 1.hours
        )

        sessionsFlow.value = listOf(session)

        val entries = useCase(timeZone).first()
        val marked = entries.single { it.date == today }
        assertTrue(marked.hasCompletedSession)
        assertFalse(entries.single { it.date == startedDay }.hasCompletedSession)
    }

    @Test
    fun `ignores sessions that are not completed`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val sessionDay = today.minus(2, DateTimeUnit.DAY)

        sessionsFlow.value = listOf(
            completedSession(onDate = sessionDay, status = WorkoutStatus.ACTIVE)
        )

        val entries = useCase(timeZone).first()
        val marked = entries.single { it.date == sessionDay }
        assertFalse(marked.hasCompletedSession)
    }

    @Test
    fun `returns entries for rolling twelve months`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val outOfRangeDay = today.minus(13, DateTimeUnit.MONTH)
        val inRangeDay = today.minus(1, DateTimeUnit.MONTH)

        sessionsFlow.value = listOf(
            completedSession(onDate = outOfRangeDay),
            completedSession(onDate = inRangeDay)
        )

        val entries = useCase(timeZone).first()
        val expectedStart = today.minus(12, DateTimeUnit.MONTH)

        assertEquals(expectedStart, entries.first().date)
        assertEquals(today, entries.last().date)
        assertTrue(entries.single { it.date == inRangeDay }.hasCompletedSession)
        assertFalse(entries.any { it.date == outOfRangeDay })
    }

    private fun completedSession(
        onDate: LocalDate,
        endedAt: Instant? = onDate.atStartOfDayIn(timeZone) + 1.hours,
        status: WorkoutStatus = WorkoutStatus.COMPLETED
    ): WorkoutSession = WorkoutSession(
        id = 1,
        workoutId = 1,
        workoutNameSnapshot = "Push",
        startedAt = onDate.atStartOfDayIn(timeZone),
        endedAt = if (status == WorkoutStatus.COMPLETED) endedAt else null,
        status = status,
        exercises = emptyList()
    )
}
