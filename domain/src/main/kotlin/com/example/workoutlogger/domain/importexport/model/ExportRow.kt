package com.example.workoutlogger.domain.importexport.model

import kotlinx.datetime.LocalDateTime

/**
 * Represents the normalized data for a single exported set.
 */
data class ExportRow(
    val dateTime: LocalDateTime,
    val workoutName: String?,
    val exerciseName: String,
    val setOrder: Int,
    val weight: Double?,
    val weightUnit: String?,
    val reps: Int?,
    val rpe: Double?,
    val distance: Double?,
    val distanceUnit: String?,
    val seconds: Int?,
    val notes: String?,
    val workoutNotes: String?,
    val workoutDuration: String?,
    val category: String? = null
)
