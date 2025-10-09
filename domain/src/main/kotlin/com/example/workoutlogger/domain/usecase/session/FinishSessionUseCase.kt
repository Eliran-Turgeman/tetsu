package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.repository.SessionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

/** Marks a session as completed. */
class FinishSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: Long, endedAt: Instant = Clock.System.now()) {
        sessionRepository.finishSession(sessionId, endedAt)
    }
}
