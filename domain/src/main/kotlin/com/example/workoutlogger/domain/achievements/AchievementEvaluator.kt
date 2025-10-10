package com.example.workoutlogger.domain.achievements

import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.model.achievements.MetricType
import com.example.workoutlogger.domain.model.achievements.Progress
import com.example.workoutlogger.domain.model.achievements.UserGoalKind
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

interface Evaluator {
    fun progress(now: Instant, ctx: DataContext, def: AchievementDefinition, inst: AchievementInstance): Progress
}

data class DataContext(
    val workouts: List<WorkoutSummary>,
    val exerciseStats: Map<String, ExerciseStats>,
    val schedules: List<ScheduleExpectation>,
    val userSettings: UserSettings,
    val timeZone: TimeZone
)

data class WorkoutSummary(
    val date: LocalDate,
    val sessions: Int,
    val totalSets: Int,
    val totalVolumeKg: Double,
    val earlySessions: Int,
    val minutesActive: Int,
    val categoryMask: Int,
    val upperLowerMask: Int
)

data class ExerciseStats(
    val name: String,
    val totalSets: Int,
    val totalVolumeKg: Double,
    val bestWeightKg: Double?,
    val bestReps: Int?,
    val bestOneRmKg: Double?,
    val performances: List<ExercisePerformance>
)

data class ExercisePerformance(
    val weightKg: Double,
    val reps: Int
)

data class ScheduleExpectation(
    val workoutId: Long,
    val expectedDates: List<LocalDate>,
    val completedDates: List<LocalDate>
)

data class UserSettings(
    val weightUnit: WeightUnit,
    val bodyWeightKg: Double?
)

class EvaluatorRegistry(private val evaluators: Map<MetricType, Evaluator>) {
    fun get(metricType: MetricType): Evaluator? = evaluators[metricType]
}

object OneRm {
    fun estimate(weightKg: Double, reps: Int): Double = weightKg * (1 + reps / 30.0)
}

fun unitForGoal(kind: UserGoalKind): String = when (kind) {
    UserGoalKind.LIFT_WEIGHT -> "kg"
    UserGoalKind.REPS_AT_WEIGHT -> "reps"
    UserGoalKind.FREQUENCY_IN_WINDOW -> "workouts"
    UserGoalKind.BODY_WEIGHT_RELATION -> "kg"
    UserGoalKind.STREAK -> "days"
    UserGoalKind.TIME_UNDER_TENSION -> "seconds"
}
