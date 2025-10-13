package com.example.workoutlogger.domain.repository

import com.example.workoutlogger.domain.model.PreviousPerformance
import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

interface SessionRepository {
    fun observeActiveSession(): Flow<WorkoutSession?>

    fun observeSession(sessionId: Long): Flow<WorkoutSession?>

    fun observeSessionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkoutSession>>

    suspend fun startSessionFromWorkout(workoutId: Long, startedAt: Instant): WorkoutSession

    suspend fun startAdHocSession(name: String, startedAt: Instant): WorkoutSession

    suspend fun upsertSessionExercise(sessionId: Long, exercise: SessionExercise): Long

    suspend fun updateSessionExerciseOrder(sessionId: Long, exerciseIdsInOrder: List<Long>)

    suspend fun deleteSessionExercise(exerciseId: Long)

    suspend fun upsertSetLog(exerciseId: Long, setLog: SessionSetLog): Long

    suspend fun deleteSetLog(setLogId: Long)

    suspend fun finishSession(sessionId: Long, endedAt: Instant)

    suspend fun cancelSession(sessionId: Long)

    suspend fun getPreviousPerformance(exerciseName: String, before: Instant): PreviousPerformance?

    suspend fun getAllSessions(): List<WorkoutSession>

    suspend fun importSessions(sessions: List<WorkoutSession>)
}
