package com.example.workoutlogger.domain.usecase.importexport

import com.example.workoutlogger.domain.importexport.toBackup
import com.example.workoutlogger.domain.repository.SessionRepository
import com.example.workoutlogger.domain.repository.SettingsRepository
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToStream
import kotlinx.serialization.json.Json

class ExportWorkoutJsonUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val json: Json
) {
    suspend operator fun invoke(outputStream: OutputStream) {
        val sessions = sessionRepository.getAllSessions()
        val defaultUnit = settingsRepository.defaultWeightUnit.first()
        val backup = sessions.toBackup(defaultUnit)
        json.encodeToStream(backup, outputStream)
    }
}
