package com.example.workoutlogger.domain.usecase.importexport

import com.example.workoutlogger.domain.importexport.model.ImportResult
import com.example.workoutlogger.domain.importexport.model.TetsuBackup
import com.example.workoutlogger.domain.importexport.toSessions
import com.example.workoutlogger.domain.repository.SessionRepository
import java.io.InputStream
import javax.inject.Inject
import kotlinx.serialization.decodeFromStream
import kotlinx.serialization.json.Json

class ImportWorkoutJsonUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val json: Json
) {
    suspend operator fun invoke(inputStream: InputStream): ImportResult {
        val backup = json.decodeFromStream(TetsuBackup.serializer(), inputStream)
        require(backup.version == 1) { "Unsupported backup version ${backup.version}" }
        val sessions = backup.toSessions()
        sessionRepository.importSessions(sessions)
        val setCount = sessions.sumOf { session -> session.exercises.sumOf { it.sets.size } }
        return ImportResult(workouts = sessions.size, sets = setCount)
    }
}
