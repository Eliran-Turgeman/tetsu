package com.example.workoutlogger.domain.importexport

import com.example.workoutlogger.domain.model.PreviousPerformance
import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.repository.SessionRepository
import com.example.workoutlogger.domain.usecase.importexport.ImportWorkoutCsvUseCase
import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportWorkoutCsvUseCaseTest {

    @Test
    fun `imports strong csv into sessions`() = runBlocking {
        val csv = """
            Date,Workout Name,Exercise Name,Set Order,Weight,Weight Unit,Reps,RPE,Distance,Distance Unit,Seconds,Notes,Workout Notes,Workout Duration
            2024-05-01 07:00:00,Morning Strength,Bench Press,1,80,kg,5,,,,,First set,,1h 0m
            2024-05-01 07:00:00,Morning Strength,Bench Press,2,75,kg,6,,,,,Second set,,1h 0m
        """.trimIndent()
        val repository = FakeSessionRepository()
        val useCase = ImportWorkoutCsvUseCase(repository)

        val result = useCase(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(1, result.workouts)
        assertEquals(2, result.sets)
        assertEquals(1, repository.importedSessions.size)
        val session = repository.importedSessions.first()
        assertEquals("Morning Strength", session.workoutNameSnapshot)
        assertEquals(1, session.exercises.size)
        val sets = session.exercises.first().sets
        assertEquals(2, sets.size)
        assertEquals(80.0, sets.first().loggedWeight)
        assertEquals(75.0, sets[1].loggedWeight)
    }

    private class FakeSessionRepository : SessionRepository {
        var importedSessions: List<WorkoutSession> = emptyList()

        override fun observeActiveSession(): Flow<WorkoutSession?> = emptyFlow()
        override fun observeSession(sessionId: Long): Flow<WorkoutSession?> = emptyFlow()
        override fun observeSessionsByDateRange(startDate: kotlinx.datetime.LocalDate, endDate: kotlinx.datetime.LocalDate): Flow<List<WorkoutSession>> = emptyFlow()
        override suspend fun startSessionFromWorkout(workoutId: Long, startedAt: Instant): WorkoutSession = error("Not used")
        override suspend fun startAdHocSession(name: String, startedAt: Instant): WorkoutSession = error("Not used")
        override suspend fun upsertSessionExercise(sessionId: Long, exercise: SessionExercise): Long = error("Not used")
        override suspend fun updateSessionExerciseOrder(sessionId: Long, exerciseIdsInOrder: List<Long>) = Unit
        override suspend fun deleteSessionExercise(exerciseId: Long) = Unit
        override suspend fun upsertSetLog(exerciseId: Long, setLog: SessionSetLog): Long = error("Not used")
        override suspend fun deleteSetLog(setLogId: Long) = Unit
        override suspend fun finishSession(sessionId: Long, endedAt: Instant) = Unit
        override suspend fun cancelSession(sessionId: Long) = Unit
        override suspend fun getPreviousPerformance(exerciseName: String, before: Instant): PreviousPerformance? = null
        override suspend fun getAllSessions(): List<WorkoutSession> = emptyList()
        override suspend fun importSessions(sessions: List<WorkoutSession>) {
            importedSessions = sessions
        }
    }
}
