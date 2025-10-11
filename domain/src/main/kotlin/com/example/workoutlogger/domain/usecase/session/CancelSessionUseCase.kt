package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.repository.SessionRepository
import com.example.workoutlogger.domain.usecase.achievements.EvaluateAchievementsUseCase
import javax.inject.Inject

/** Cancels an in-progress session. */
class CancelSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase
) {
    suspend operator fun invoke(sessionId: Long) {
        sessionRepository.cancelSession(sessionId)
        evaluateAchievementsUseCase()
    }
}
