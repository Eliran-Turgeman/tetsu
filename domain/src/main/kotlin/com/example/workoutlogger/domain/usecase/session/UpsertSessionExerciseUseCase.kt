package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.repository.SessionRepository
import javax.inject.Inject

/** Adds a new exercise to a session or updates an existing one. */
class UpsertSessionExerciseUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: Long, exercise: SessionExercise): Long {
        require(exercise.exerciseName.isNotBlank()) { "Exercise name cannot be blank" }
        return sessionRepository.upsertSessionExercise(sessionId, exercise)
    }
}
