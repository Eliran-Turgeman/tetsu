package com.example.workoutlogger.domain.achievements

import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.model.achievements.AchievementMetadata
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
            metadata = AchievementMetadata(exerciseName = "deadlift")
        )
        val progress = evaluator.progress(now(), context, def, instance)
        assertEquals(180.0, progress.current)
        assertTrue(progress.percent >= 1.0)
    }

    @Test
    fun streakEvaluatorStopsAtGap() {
        val evaluator = StreakEvaluator()
        val context = context(
            workouts = listOf(
                summary(LocalDate(2024, 5, 5), sessions = 1),
                summary(LocalDate(2024, 5, 4), sessions = 1),
                summary(LocalDate(2024, 5, 2), sessions = 1) // gap on 5/3 breaks streak
            )
        )
        val def = baseDefinition.copy(metric = MetricType.STREAK_ACTIVE_DAYS, targetValue = 5.0)
        val progress = evaluator.progress(now(), context, def, baseInstance)
        assertEquals(2.0, progress.current)
        assertEquals("days", progress.unit)
    }

    @Test
    fun varietyBalanceRequiresAllCategories() {
        val evaluator = VarietyBalanceEvaluator()
        val context = context(
            workouts = listOf(
                summary(LocalDate(2024, 5, 1), sessions = 1, categoryMask = WorkoutDailyCategory.PUSH),
                summary(LocalDate(2024, 5, 2), sessions = 1, categoryMask = WorkoutDailyCategory.PULL),
                summary(
                    LocalDate(2024, 5, 3),
                    sessions = 1,
                    categoryMask = WorkoutDailyCategory.LEGS or WorkoutDailyCategory.PULL
                )
            )
        )
        val def = baseDefinition.copy(metric = MetricType.VARIETY_BALANCE, targetValue = 3.0)
        val progress = evaluator.progress(now(), context, def, baseInstance)
        assertEquals(3.0, progress.current)
        assertEquals("categories", progress.unit)
    }

    @Test
    fun scheduleAdherenceUsesCompletedRatio() {
        val evaluator = ScheduleAdherenceEvaluator()
        val schedule = ScheduleExpectation(
            workoutId = 1L,
            expectedDates = listOf(
                LocalDate(2024, 4, 29),
                LocalDate(2024, 4, 30),
                LocalDate(2024, 5, 1),
                LocalDate(2024, 5, 2)
            ),
            completedDates = listOf(
                LocalDate(2024, 4, 29),
                LocalDate(2024, 5, 1),
                LocalDate(2024, 5, 2)
            )
        )
        val context = context(schedules = listOf(schedule))
        val def = baseDefinition.copy(
            metric = MetricType.SCHEDULE_ADHERENCE,
            targetValue = 0.75,
            windowDays = 7
        )
        val progress = evaluator.progress(now(), context, def, baseInstance)
        assertEquals(0.75, progress.current)
        assertTrue(progress.percent >= 1.0)
    }

    @Test
    fun comebackEvaluatorCountsSessionsAfterBreak() {
        val evaluator = ComebackEvaluator()
        val workouts = listOf(
            summary(LocalDate(2024, 3, 1), sessions = 1),
            summary(LocalDate(2024, 3, 2), sessions = 1),
            summary(LocalDate(2024, 4, 20), sessions = 1),
            summary(LocalDate(2024, 4, 21), sessions = 1),
            summary(LocalDate(2024, 4, 22), sessions = 1)
        )
        val context = context(workouts = workouts)
        val def = baseDefinition.copy(
            metric = MetricType.COMEBACK,
            targetValue = 3.0,
            windowDays = 7
        )
        val progress = evaluator.progress(now(), context, def, baseInstance)
        assertEquals(3.0, progress.current)
        assertTrue(progress.percent >= 1.0)
    }

    @Test
    fun repsAtWeightGoalUsesStrongestMatchingSet() {
        val evaluator = RepsAtWeightGoalEvaluator()
        val stats = ExerciseStats(
            name = "bench press",
            totalSets = 10,
            totalVolumeKg = 1000.0,
            bestWeightKg = 120.0,
            bestReps = 10,
            bestOneRmKg = 140.0,
            performances = listOf(
                ExercisePerformance(weightKg = 100.0, reps = 12),
                ExercisePerformance(weightKg = 110.0, reps = 8),
                ExercisePerformance(weightKg = 105.0, reps = 9)
            )
        )
        val context = context(exerciseStats = mapOf("bench press" to stats))
        val def = baseDefinition.copy(
            metric = MetricType.REPS_AT_WEIGHT,
            targetValue = 8.0
        )
        val instance = baseInstance.copy(
            definitionId = def.id,
            metadata = AchievementMetadata(
                exerciseName = "bench press",
                secondaryTarget = 105.0
            )
        )
        val progress = evaluator.progress(now(), context, def, instance)
        assertEquals(9.0, progress.current)
        assertTrue(progress.percent >= 1.0)
    }

    @Test
    fun totalVolumeEvaluatorSumsWorkouts() {
        val evaluator = TotalVolumeEvaluator()
        val context = context(
            workouts = listOf(
                summary(LocalDate(2024, 5, 1), sessions = 1, totalVolumeKg = 300.0),
                summary(LocalDate(2024, 5, 2), sessions = 1, totalVolumeKg = 150.0)
            )
        )
        val def = baseDefinition.copy(metric = MetricType.TOTAL_VOLUME, targetValue = 400.0)
        val progress = evaluator.progress(now(), context, def, baseInstance)
        assertEquals(450.0, progress.current)
        assertEquals("kg", progress.unit)
        assertTrue(progress.percent >= 1.0)
    }

    @Test
    fun earlyBirdEvaluatorCountsRecentEarlySessions() {
        val evaluator = EarlyBirdEvaluator()
        val context = context(
            workouts = listOf(
                summary(LocalDate(2024, 5, 5), sessions = 1, earlySessions = 1),
                summary(LocalDate(2024, 4, 15), sessions = 1, earlySessions = 2),
                summary(LocalDate(2024, 3, 1), sessions = 1, earlySessions = 5) // outside default window
            )
        )
        val def = baseDefinition.copy(metric = MetricType.EARLY_BIRD, targetValue = 3.0, windowDays = 30)
        val progress = evaluator.progress(now(), context, def, baseInstance)
        assertEquals(3.0, progress.current)
        assertEquals("workouts", progress.unit)
    }

    @Test
    fun oneRmTargetEvaluatorUsesBestOneRm() {
        val evaluator = OneRmTargetEvaluator()
        val stats = ExerciseStats(
            name = "squat",
            totalSets = 5,
            totalVolumeKg = 800.0,
            bestWeightKg = 150.0,
            bestReps = 5,
            bestOneRmKg = 180.0,
            performances = emptyList()
        )
        val context = context(exerciseStats = mapOf("squat" to stats))
        val def = baseDefinition.copy(metric = MetricType.ONE_RM_TARGET, targetValue = 180.0)
        val instance = baseInstance.copy(
            definitionId = def.id,
            metadata = AchievementMetadata(exerciseName = "squat")
        )
        val progress = evaluator.progress(now(), context, def, instance)
        assertEquals(180.0, progress.current)
        assertTrue(progress.percent >= 1.0)
    }

    @Test
    fun frequencyGoalEvaluatorRespectsMetadataWindow() {
        val evaluator = FrequencyGoalEvaluator()
        val context = context(
            workouts = listOf(
                summary(LocalDate(2024, 5, 5), sessions = 1),
                summary(LocalDate(2024, 5, 1), sessions = 1),
                summary(LocalDate(2024, 4, 27), sessions = 1),
                summary(LocalDate(2024, 4, 20), sessions = 1) // outside 14-day window
            )
        )
        val def = baseDefinition.copy(metric = MetricType.FREQUENCY_TARGET, targetValue = 3.0)
        val instance = baseInstance.copy(
            definitionId = def.id,
            metadata = AchievementMetadata(windowDays = 14)
        )
        val progress = evaluator.progress(now(), context, def, instance)
        assertEquals(3.0, progress.current)
        assertEquals("workouts", progress.unit)
        assertTrue(progress.percent >= 1.0)
    }

    @Test
    fun timeUnderTensionConvertsMinutesToSeconds() {
        val evaluator = TimeUnderTensionEvaluator()
        val context = context(
            workouts = listOf(
                summary(LocalDate(2024, 5, 1), sessions = 1, minutesActive = 30),
                summary(LocalDate(2024, 5, 2), sessions = 1, minutesActive = 15)
            )
        )
        val def = baseDefinition.copy(
            metric = MetricType.TIME_UNDER_TENSION,
            targetValue = 2700.0,
            windowDays = 7
        )
        val progress = evaluator.progress(now(), context, def, baseInstance)
        assertEquals(2700.0, progress.current)
        assertTrue(progress.percent >= 1.0)
    }

    private fun summary(
        date: LocalDate,
        sessions: Int = 0,
        minutesActive: Int = 0,
        categoryMask: Int = 0,
        totalVolumeKg: Double = 0.0,
        earlySessions: Int = 0
    ): WorkoutSummary = WorkoutSummary(
        date = date,
        sessions = sessions,
        totalSets = 0,
        totalVolumeKg = totalVolumeKg,
        earlySessions = earlySessions,
        minutesActive = minutesActive,
        categoryMask = categoryMask,
        upperLowerMask = 0
    )

    private fun now(): Instant = Instant.parse("2024-05-05T00:00:00Z")

    private fun context(
        workouts: List<WorkoutSummary> = emptyList(),
        exerciseStats: Map<String, ExerciseStats> = emptyMap(),
        schedules: List<ScheduleExpectation> = emptyList(),
        userSettings: UserSettings = UserSettings(weightUnit = WeightUnit.KG, bodyWeightKg = null)
    ): DataContext = DataContext(
        workouts = workouts,
        exerciseStats = exerciseStats,
        schedules = schedules,
        userSettings = userSettings,
        timeZone = timeZone
    )
}
