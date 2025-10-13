package com.example.workoutlogger.domain.importexport

import com.example.workoutlogger.domain.importexport.model.BackupExercise
import com.example.workoutlogger.domain.importexport.model.BackupMeasurement
import com.example.workoutlogger.domain.importexport.model.BackupSet
import com.example.workoutlogger.domain.importexport.model.BackupUnits
import com.example.workoutlogger.domain.importexport.model.BackupWorkout
import com.example.workoutlogger.domain.importexport.model.TetsuBackup
import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WorkoutStatus
import java.util.UUID
import kotlinx.datetime.Clock

fun List<WorkoutSession>.toBackup(units: WeightUnit): TetsuBackup {
    val unitLabel = when (units) {
        WeightUnit.KG -> "kg"
        WeightUnit.LB -> "lb"
    }
    return TetsuBackup(
        exportedAt = Clock.System.now(),
        units = BackupUnits(weight = unitLabel),
        workouts = this.filter { it.status != WorkoutStatus.CANCELLED }
            .map { it.toBackupWorkout() }
    )
}

private fun WorkoutSession.toBackupWorkout(): BackupWorkout {
    val supersetLookup = exercises.filter { it.supersetGroupId != null }
        .groupBy { it.supersetGroupId!! }
        .mapValues { entry ->
            entry.value.map { it.exerciseName }
        }
    return BackupWorkout(
        id = id?.toString(),
        startedAt = startedAt,
        endedAt = endedAt,
        name = workoutNameSnapshot,
        notes = null,
        exercises = exercises.sortedBy { it.position }.map { exercise ->
            val partners = exercise.supersetGroupId
                ?.let { supersetLookup[it].orEmpty().filter { name -> name != exercise.exerciseName } }
                ?: emptyList()
            BackupExercise(
                name = exercise.exerciseName,
                isSupersetWith = partners,
                sets = exercise.sets.sortedBy { it.setIndex }.map { set ->
                    BackupSet(
                        order = set.setIndex + 1,
                        weight = set.loggedWeight?.let { weightValue ->
                            BackupMeasurement(value = weightValue, unit = set.unit.toLabel())
                        },
                        reps = set.loggedReps,
                        rpe = null,
                        durationSec = null,
                        distance = null,
                        notes = set.note
                    )
                }
            )
        }
    )
}

private fun WeightUnit.toLabel(): String = when (this) {
    WeightUnit.KG -> "kg"
    WeightUnit.LB -> "lb"
}

fun TetsuBackup.toSessions(): List<WorkoutSession> {
    val sessions = mutableListOf<WorkoutSession>()
    workouts.forEach { workout ->
        val supersetMapping = mutableMapOf<String, String>()
        val exercises = workout.exercises.mapIndexed { index, exercise ->
            val supersetGroupId = if (exercise.isSupersetWith.isNotEmpty()) {
                val key = (listOf(exercise.name) + exercise.isSupersetWith.sorted()).joinToString("|")
                supersetMapping.getOrPut(key) { UUID.randomUUID().toString() }
            } else {
                null
            }
            SessionExercise(
                id = null,
                sessionId = null,
                position = index,
                supersetGroupId = supersetGroupId,
                exerciseName = exercise.name,
                sets = exercise.sets.mapIndexed { setIndex, set ->
                    SessionSetLog(
                        id = null,
                        sessionExerciseId = null,
                        setIndex = setIndex,
                        targetRepsMin = null,
                        targetRepsMax = null,
                        loggedReps = set.reps,
                        loggedWeight = set.weight?.value,
                        unit = set.weight?.unit?.toWeightUnit() ?: WeightUnit.KG,
                        note = set.notes
                    )
                }
            )
        }
        sessions += WorkoutSession(
            id = null,
            workoutId = null,
            workoutNameSnapshot = workout.name,
            startedAt = workout.startedAt,
            endedAt = workout.endedAt,
            status = WorkoutStatus.COMPLETED,
            exercises = exercises
        )
    }
    return sessions
}

private fun String.toWeightUnit(): WeightUnit = when (lowercase()) {
    "lb", "lbs" -> WeightUnit.LB
    else -> WeightUnit.KG
}
