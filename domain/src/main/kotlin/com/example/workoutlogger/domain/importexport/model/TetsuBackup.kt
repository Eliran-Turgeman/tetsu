package com.example.workoutlogger.domain.importexport.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TetsuBackup(
    val version: Int = 1,
    val exportedAt: Instant,
    val units: BackupUnits,
    val workouts: List<BackupWorkout>
)

@Serializable
data class BackupUnits(
    val weight: String
)

@Serializable
data class BackupWorkout(
    val id: String?,
    val startedAt: Instant,
    val endedAt: Instant?,
    val name: String,
    val notes: String? = null,
    val exercises: List<BackupExercise>
)

@Serializable
data class BackupExercise(
    val name: String,
    val isSupersetWith: List<String> = emptyList(),
    val sets: List<BackupSet>
)

@Serializable
data class BackupSet(
    val order: Int,
    val weight: BackupMeasurement?,
    val reps: Int?,
    val rpe: Double?,
    val durationSec: Int?,
    val distance: BackupMeasurement?,
    val notes: String?
)

@Serializable
data class BackupMeasurement(
    val value: Double,
    val unit: String
)
