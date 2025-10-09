package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.repository.SessionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Starts a workout session derived from a saved workout definition.
 */
class StartSessionFromWorkoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(workoutId: Long, startedAt: Instant = Clock.System.now()): WorkoutSession {
        return sessionRepository.startSessionFromWorkout(workoutId, startedAt)
    }
}
