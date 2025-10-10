package com.example.workoutlogger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.workoutlogger.domain.model.achievements.AchievementStatus
import com.example.workoutlogger.domain.model.achievements.AchievementType
import com.example.workoutlogger.domain.model.achievements.MetricType
import com.example.workoutlogger.domain.model.achievements.UserGoalKind
import kotlinx.datetime.Instant

@Entity(tableName = "achievement_definitions")
data class AchievementDefinitionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val type: AchievementType,
    val metric: MetricType,
    val targetValue: Double,
    val windowDays: Int?,
    val repeatable: Boolean,
    val tier: Int,
    val iconKey: String,
    val sort: Int
)

@Entity(
    tableName = "achievement_instances",
    indices = [Index("definitionId"), Index("status")]
)
data class AchievementInstanceEntity(
    @PrimaryKey
    val instanceId: String,
    val definitionId: String,
    val createdAt: Instant,
    val status: AchievementStatus,
    val progressCurrent: Double,
    val progressTarget: Double,
    val progressUnit: String,
    val percent: Double,
    val completedAt: Instant?,
    val userNotes: String? = null,
    val extraJson: String? = null
)

@Entity(tableName = "user_goals")
data class UserGoalEntity(
    @PrimaryKey
    val goalId: String,
    val title: String,
    val description: String?,
    val kind: UserGoalKind,
    val exerciseName: String?,
    val targetValue: Double,
    val secondaryValue: Double?,
    val windowDays: Int?,
    val deadlineAt: Instant?,
    val createdAt: Instant
)

/**
 * Pre-aggregated summary per calendar day (local tz) for achievements evaluation.
 */
@Entity(tableName = "workout_daily_summaries")
data class WorkoutDailySummaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "date_epoch_day")
    val dateEpochDay: Long,
    @ColumnInfo(name = "workouts_completed")
    val workoutsCompleted: Int,
    @ColumnInfo(name = "total_sets")
    val totalSets: Int,
    @ColumnInfo(name = "total_volume_kg")
    val totalVolumeKg: Double,
    @ColumnInfo(name = "unique_exercises")
    val uniqueExercises: Int,
    @ColumnInfo(name = "category_mask")
    val categoryMask: Int,
    @ColumnInfo(name = "upper_lower_mask")
    val upperLowerMask: Int,
    @ColumnInfo(name = "early_sessions")
    val earlySessions: Int,
    @ColumnInfo(name = "minutes_active")
    val minutesActive: Int
) {
    companion object {
        const val CATEGORY_PUSH = 1 shl 0
        const val CATEGORY_PULL = 1 shl 1
        const val CATEGORY_LEGS = 1 shl 2

        const val UPPER_BODY = 1 shl 0
        const val LOWER_BODY = 1 shl 1
    }
}
