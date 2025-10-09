package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.repository.SessionRepository
import javax.inject.Inject

/** Removes a set log entry. */
class DeleteSetLogUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(setLogId: Long) {
        sessionRepository.deleteSetLog(setLogId)
    }
}
