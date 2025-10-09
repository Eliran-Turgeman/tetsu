package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.repository.SessionRepository
import javax.inject.Inject

/** Persists the latest order of exercises inside a session. */
class UpdateExerciseOrderUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: Long, orderedIds: List<Long>) {
        sessionRepository.updateSessionExerciseOrder(sessionId, orderedIds)
    }
}
