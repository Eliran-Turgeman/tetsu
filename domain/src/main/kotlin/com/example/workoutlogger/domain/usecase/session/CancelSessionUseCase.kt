package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.repository.SessionRepository
import javax.inject.Inject

/** Cancels an in-progress session. */
class CancelSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: Long) {
        sessionRepository.cancelSession(sessionId)
    }
}
