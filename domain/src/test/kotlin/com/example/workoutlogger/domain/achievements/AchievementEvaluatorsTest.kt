package com.example.workoutlogger.domain.achievements

import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.model.achievements.AchievementStatus
import com.example.workoutlogger.domain.model.achievements.AchievementType
import com.example.workoutlogger.domain.model.achievements.MetricType
import com.example.workoutlogger.domain.model.achievements.Progress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class AchievementEvaluatorsTest {

    private val timeZone = TimeZone.UTC
    private val baseDefinition = AchievementDefinition(
        id = "test",
        title = "Test",
        description = "",
        type = AchievementType.CONSISTENCY,
        metric = MetricType.WORKOUTS_PER_WEEK,
        targetValue = 3.0,
        windowDays = 7,
        repeatable = false,
        tier = 1,
        iconKey = "icon",
        sort = 0
    )
    private val baseInstance = AchievementInstance(
        instanceId = "instance",
        definitionId = baseDefinition.id,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        status = AchievementStatus.IN_PROGRESS,
        progress = Progress(0.0, baseDefinition.targetValue, 0.0, "workouts"),
        completedAt = null,
        userNotes = null,
        metadata = null
    )

    @Test
    fun firstWorkoutCompletesWithSingleSession() {
        val evaluator = FirstWorkoutEvaluator()
        val context = DataContext(
            workouts = listOf(summary(LocalDate(2024, 5, 1), sessions = 1)),
            exerciseStats = emptyMap(),
            schedules = emptyList(),
            userSettings = UserSettings(weightUnit = WeightUnit.KG, bodyWeightKg = null),
            timeZone = timeZone
        )
        val def = baseDefinition.copy(metric = MetricType.FIRST_WORKOUT, targetValue = 1.0)
        val progress = evaluator.progress(now(), context, def, baseInstance)
        assertEquals(1.0, progress.current)
        assertTrue(progress.percent >= 1.0)
    }

    @Test
    fun workoutsPerWeekCountsWindow() {
        val evaluator = WorkoutsPerWindowEvaluator(7)
        val context = DataContext(
            workouts = listOf(
                summary(LocalDate(2024, 5, 1), sessions = 1),
                summary(LocalDate(2024, 5, 2), sessions = 1),
                summary(LocalDate(2024, 5, 9), sessions = 1) // outside 7d window
            ),
            exerciseStats = emptyMap(),
            schedules = emptyList(),
            userSettings = UserSettings(weightUnit = WeightUnit.KG, bodyWeightKg = null),
            timeZone = timeZone
        )
        val def = baseDefinition.copy(metric = MetricType.WORKOUTS_PER_WEEK, targetValue = 3.0)
        val progress = evaluator.progress(now(), context, def, baseInstance)
        assertEquals(2.0, progress.current)
        assertEquals("workouts", progress.unit)
    }

    @Test
    fun bodyWeightGoalUsesUserWeight() {
        val evaluator = BodyWeightRelationEvaluator()
        val stats = ExerciseStats(
            name = "deadlift",
            totalSets = 3,
            totalVolumeKg = 300.0,
            bestWeightKg = 180.0,
            bestReps = 5,
            bestOneRmKg = 200.0,
            performances = emptyList()
        )
        val context = DataContext(
            workouts = emptyList(),
            exerciseStats = mapOf("deadlift" to stats),
            schedules = emptyList(),
            userSettings = UserSettings(weightUnit = WeightUnit.KG, bodyWeightKg = 90.0),
            timeZone = timeZone
        )
        val def = baseDefinition.copy(
            id = "user_goal:body_weight",
            metric = MetricType.BODY_WEIGHT_RELATION,
            targetValue = 90.0
        )
        val instance = baseInstance.copy(
            definitionId = def.id,
            metadata = com.example.workoutlogger.domain.model.achievements.AchievementMetadata(exerciseName = "deadlift")
        )
        val progress = evaluator.progress(now(), context, def, instance)
        assertEquals(180.0, progress.current)
        assertTrue(progress.percent >= 1.0)
    }

    private fun summary(date: LocalDate, sessions: Int = 0): WorkoutSummary = WorkoutSummary(
        date = date,
        sessions = sessions,
        totalSets = 0,
        totalVolumeKg = 0.0,
        earlySessions = 0,
        minutesActive = 0,
        categoryMask = 0,
        upperLowerMask = 0
    )

    private fun now(): Instant = Instant.parse("2024-05-05T00:00:00Z")
}
