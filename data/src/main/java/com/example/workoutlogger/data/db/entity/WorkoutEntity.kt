package com.example.workoutlogger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.datetime.Instant

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)

@Entity(tableName = "workout_items")
data class WorkoutItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "workout_id")
    val workoutId: Long,
    val position: Int,
    @ColumnInfo(name = "type")
    val type: WorkoutItemType,
    @ColumnInfo(name = "superset_group_id")
    val supersetGroupId: String?,
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String?,
    val sets: Int?,
    @ColumnInfo(name = "reps_min")
    val repsMin: Int?,
    @ColumnInfo(name = "reps_max")
    val repsMax: Int?
)

enum class WorkoutItemType {
    EXERCISE,
    SUPERSET_HEADER
}

@Entity(tableName = "workout_schedules")
data class WorkoutScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "workout_id")
    val workoutId: Long,
    @ColumnInfo(name = "days_of_week")
    val daysOfWeek: String,
    @ColumnInfo(name = "notify_hour")
    val notifyHour: Int,
    @ColumnInfo(name = "notify_minute")
    val notifyMinute: Int,
    val enabled: Boolean
)

/**
 * Relationship object for queries returning workouts with their items.
 */
data class WorkoutWithItems(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workout_id"
    )
    val items: List<WorkoutItemEntity>
)
