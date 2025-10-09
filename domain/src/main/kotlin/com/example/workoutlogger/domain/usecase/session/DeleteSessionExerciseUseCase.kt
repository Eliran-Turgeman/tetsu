package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.repository.SessionRepository
import javax.inject.Inject

/** Deletes an exercise and associated set logs. */
class DeleteSessionExerciseUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(exerciseId: Long) {
        sessionRepository.deleteSessionExercise(exerciseId)
    }
}
