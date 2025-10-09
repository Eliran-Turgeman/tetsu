package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class ObserveSessionsInRangeUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(start: LocalDate, end: LocalDate): Flow<List<WorkoutSession>> =
        sessionRepository.observeSessionsByDateRange(start, end)
}
