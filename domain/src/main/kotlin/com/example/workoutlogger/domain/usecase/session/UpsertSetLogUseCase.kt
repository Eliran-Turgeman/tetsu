package com.example.workoutlogger.domain.usecase.session

import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.repository.SessionRepository
import javax.inject.Inject

/** Writes a set log, either appending or updating. */
class UpsertSetLogUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(exerciseId: Long, setLog: SessionSetLog): Long {
        return sessionRepository.upsertSetLog(exerciseId, setLog)
    }
}
