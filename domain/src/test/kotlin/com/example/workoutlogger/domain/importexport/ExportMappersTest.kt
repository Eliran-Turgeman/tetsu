package com.example.workoutlogger.domain.importexport

import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WorkoutStatus
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportMappersTest {

    @Test
    fun `superset information is appended to notes`() {
        val session = WorkoutSession(
            id = 1L,
            workoutId = null,
            workoutNameSnapshot = "Push Day",
            startedAt = Instant.parse("2024-06-01T10:00:00Z"),
            endedAt = Instant.parse("2024-06-01T11:00:00Z"),
            status = WorkoutStatus.COMPLETED,
            exercises = listOf(
                SessionExercise(
                    id = 1L,
                    sessionId = 1L,
                    position = 0,
                    supersetGroupId = "A",
                    exerciseName = "Bench Press",
                    sets = listOf(
                        SessionSetLog(
                            id = 1L,
                            sessionExerciseId = 1L,
                            setIndex = 0,
                            loggedReps = 5,
                            loggedWeight = 100.0,
                            unit = WeightUnit.KG,
                            note = "paused"
                        )
                    )
                ),
                SessionExercise(
                    id = 2L,
                    sessionId = 1L,
                    position = 1,
                    supersetGroupId = "A",
                    exerciseName = "Pull Ups",
                    sets = listOf(
                        SessionSetLog(
                            id = 2L,
                            sessionExerciseId = 2L,
                            setIndex = 0,
                            loggedReps = 8,
                            loggedWeight = null,
                            unit = WeightUnit.KG,
                            note = null
                        )
                    )
                )
            )
        )

        val rows = session.toExportRows()
        val benchRow = rows.first()
        assertEquals("Bench Press", benchRow.exerciseName)
        assertTrue { benchRow.notes!!.contains("Superset with: Pull Ups") }
        assertTrue { benchRow.notes!!.contains("paused") }
    }
}
