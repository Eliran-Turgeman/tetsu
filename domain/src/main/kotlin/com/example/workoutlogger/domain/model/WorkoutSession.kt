package com.example.workoutlogger.domain.model

import kotlinx.datetime.Instant

/**
 * Tracks a concrete workout execution derived from a saved workout or ad-hoc.
 */
data class WorkoutSession(
    val id: Long? = null,
    val workoutId: Long? = null,
    val workoutNameSnapshot: String,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val status: WorkoutStatus,
    val exercises: List<SessionExercise>
)

data class SessionExercise(
    val id: Long? = null,
    val sessionId: Long? = null,
    val position: Int,
    val supersetGroupId: String? = null,
    val exerciseName: String,
    val sets: List<SessionSetLog>
)

data class SessionSetLog(
    val id: Long? = null,
    val sessionExerciseId: Long? = null,
    val setIndex: Int,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val loggedReps: Int? = null,
    val loggedWeight: Double? = null,
    val unit: WeightUnit = WeightUnit.KG,
    val note: String? = null
)

enum class WorkoutStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}

enum class WeightUnit {
    KG,
    LB
}
