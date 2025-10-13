package com.example.workoutlogger.domain.importexport.model

/**
 * Result of an import operation.
 */
data class ImportResult(
    val workouts: Int,
    val sets: Int
)
