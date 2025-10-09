package com.example.workoutlogger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.datetime.Instant

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["workout_id"]),
        Index(value = ["status"]),
        Index(value = ["ended_at"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "workout_id")
    val workoutId: Long?,
    @ColumnInfo(name = "workout_name_snapshot")
    val workoutNameSnapshot: String,
    @ColumnInfo(name = "started_at")
    val startedAt: Instant,
    @ColumnInfo(name = "ended_at")
    val endedAt: Instant?,
    val status: SessionStatus
)

@Entity(
    tableName = "session_exercises",
    indices = [
        Index(value = ["session_id", "position"], unique = true),
        Index(value = ["exercise_name"])
    ]
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    val position: Int,
    @ColumnInfo(name = "superset_group_id")
    val supersetGroupId: String?,
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String
)

@Entity(
    tableName = "session_set_logs",
    indices = [
        Index(value = ["session_exercise_id", "set_index"], unique = true)
    ]
)
data class SessionSetLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "session_exercise_id")
    val sessionExerciseId: Long,
    @ColumnInfo(name = "set_index")
    val setIndex: Int,
    @ColumnInfo(name = "target_reps_min")
    val targetRepsMin: Int?,
    @ColumnInfo(name = "target_reps_max")
    val targetRepsMax: Int?,
    @ColumnInfo(name = "logged_reps")
    val loggedReps: Int?,
    @ColumnInfo(name = "logged_weight")
    val loggedWeight: Double?,
    val unit: WeightUnit,
    val note: String?
)

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}

enum class WeightUnit {
    KG,
    LB
}

/**
 * Relationship tree for session with exercises and sets.
 */
data class SessionExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_exercise_id"
    )
    val sets: List<SessionSetLogEntity>
)

data class SessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id",
        entity = SessionExerciseEntity::class
    )
    val exercises: List<SessionExerciseWithSets>
)
