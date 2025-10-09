package com.example.workoutlogger.domain.model

import kotlinx.datetime.Instant

/**
 * Represents a reusable workout definition stored in the catalog.
 */
data class Workout(
    val id: Long? = null,
    val name: String,
    val createdAt: Instant,
    val items: List<WorkoutItem>
)

data class WorkoutItem(
    val id: Long? = null,
    val workoutId: Long? = null,
    val position: Int,
    val type: WorkoutItemType,
    val supersetGroupId: String? = null,
    val exerciseName: String? = null,
    val sets: Int? = null,
    val repsMin: Int? = null,
    val repsMax: Int? = null
)

enum class WorkoutItemType {
    EXERCISE,
    SUPERSET_HEADER
}

// Backwards-compatible aliases for existing callers still referencing template terminology.
typealias WorkoutTemplate = Workout
typealias TemplateItem = WorkoutItem
typealias TemplateItemType = WorkoutItemType
