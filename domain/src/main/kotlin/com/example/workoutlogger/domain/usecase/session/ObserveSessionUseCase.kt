package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams updates for a specific session id. */
class ObserveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(sessionId: Long): Flow<WorkoutSession?> =
        sessionRepository.observeSession(sessionId)
}
