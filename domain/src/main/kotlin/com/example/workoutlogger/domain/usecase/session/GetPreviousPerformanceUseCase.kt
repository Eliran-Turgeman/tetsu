package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.model.PreviousPerformance
import com.example.workoutlogger.domain.repository.SessionRepository
import kotlinx.datetime.Instant
import javax.inject.Inject

/** Fetches the most recent performance for a given exercise name before a timestamp. */
class GetPreviousPerformanceUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(exerciseName: String, before: Instant): PreviousPerformance? {
        return sessionRepository.getPreviousPerformance(exerciseName, before)
    }
}
