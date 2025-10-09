package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.repository.SessionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

/** Starts an ad-hoc session without a template. */
class StartAdHocSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(name: String, startedAt: Instant = Clock.System.now()): WorkoutSession {
        require(name.isNotBlank()) { "Session name must not be blank" }
        return sessionRepository.startAdHocSession(name, startedAt)
    }
}
