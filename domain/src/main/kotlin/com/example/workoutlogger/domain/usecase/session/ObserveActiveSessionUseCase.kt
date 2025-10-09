package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits the currently active workout session, if any. */
class ObserveActiveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(): Flow<WorkoutSession?> = sessionRepository.observeActiveSession()
}
