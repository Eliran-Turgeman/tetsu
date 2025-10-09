package com.example.workoutlogger.domain.usecase.heatmap

import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WorkoutStatus
import com.example.workoutlogger.domain.repository.SessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlin.time.Duration.Companion.hours

class ObserveHeatmapUseCaseTest {

    private val sessionsFlow = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val repository = object : SessionRepository {
        override fun observeActiveSession(): Flow<WorkoutSession?> = MutableStateFlow(null)
        override fun observeSession(sessionId: Long): Flow<WorkoutSession?> = MutableStateFlow(null)
        override fun observeSessionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkoutSession>> = sessionsFlow
        override suspend fun startSessionFromTemplate(templateId: Long, startedAt: Instant): WorkoutSession = error("Not needed")
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
        val timeZone = TimeZone.UTC
        val today = LocalDate(2024, 5, 1)
        val sessionDay = today.minus(DatePeriod(days = 1))
        sessionsFlow.value = listOf(
            WorkoutSession(
                id = 1,
                templateId = 1,
                templateNameSnapshot = "Push",
                startedAt = sessionDay.atStartOfDayIn(timeZone),
                endedAt = sessionDay.atStartOfDayIn(timeZone) + 1.hours,
                status = WorkoutStatus.COMPLETED,
                exercises = emptyList()
            )
        )

        val entries = useCase(timeZone).first()
        val marked = entries.firstOrNull { it.date == sessionDay }
        assertEquals(true, marked?.hasCompletedSession)
    }
}
