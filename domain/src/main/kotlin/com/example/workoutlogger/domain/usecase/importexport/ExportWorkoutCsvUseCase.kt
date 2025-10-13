package com.example.workoutlogger.domain.usecase.importexport

import com.example.workoutlogger.domain.importexport.CsvProfile
import com.example.workoutlogger.domain.importexport.CsvWriter
import com.example.workoutlogger.domain.importexport.toExportRows
import com.example.workoutlogger.domain.repository.SessionRepository
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject

class ExportWorkoutCsvUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(profile: CsvProfile, outputStream: OutputStream) {
        val sessions = sessionRepository.getAllSessions()
        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            val csvWriter = CsvWriter(writer)
            csvWriter.writeRow(profile.header)
            sessions.flatMap { it.toExportRows() }
                .forEach { row ->
                    csvWriter.writeRow(profile.toFields(row))
                }
        }
    }
}
