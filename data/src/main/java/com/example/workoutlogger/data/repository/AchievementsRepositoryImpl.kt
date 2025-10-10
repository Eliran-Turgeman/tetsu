package com.example.workoutlogger.data.repository

import androidx.room.withTransaction
import com.example.workoutlogger.data.db.WorkoutLoggerDatabase
import com.example.workoutlogger.data.db.dao.AchievementsDao
import com.example.workoutlogger.data.db.dao.ScheduleDao
import com.example.workoutlogger.data.db.dao.SessionDao
import com.example.workoutlogger.data.db.entity.SessionStatus
import com.example.workoutlogger.data.db.entity.WorkoutDailySummaryEntity
import com.example.workoutlogger.data.db.entity.WeightUnit as EntityWeightUnit
import com.example.workoutlogger.data.mapper.toDomain
import com.example.workoutlogger.data.mapper.toEntity
import com.example.workoutlogger.domain.achievements.*
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.model.achievements.AchievementEvent
import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.model.achievements.AchievementStatus
import com.example.workoutlogger.domain.model.achievements.MetricType
import com.example.workoutlogger.domain.model.achievements.Progress
import com.example.workoutlogger.domain.model.achievements.UserGoal
import com.example.workoutlogger.domain.model.achievements.UserGoalKind
import com.example.workoutlogger.domain.repository.AchievementsRepository
import com.example.workoutlogger.domain.repository.SettingsRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Singleton
class AchievementsRepositoryImpl @Inject constructor(
    private val database: WorkoutLoggerDatabase,
    private val achievementsDao: AchievementsDao,
    private val sessionDao: SessionDao,
    private val scheduleDao: ScheduleDao,
    private val settingsRepository: SettingsRepository
) : AchievementsRepository {

    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
    private val _events = MutableSharedFlow<AchievementEvent>(extraBufferCapacity = 16)
    private val registry: EvaluatorRegistry = EvaluatorRegistry(
        mapOf(
            MetricType.FIRST_WORKOUT to FirstWorkoutEvaluator(),
            MetricType.WORKOUTS_PER_WEEK to WorkoutsPerWindowEvaluator(7),
            MetricType.WORKOUTS_PER_MONTH to WorkoutsPerWindowEvaluator(30),
            MetricType.STREAK_ACTIVE_DAYS to StreakEvaluator(),
            MetricType.TOTAL_VOLUME to TotalVolumeEvaluator(),
            MetricType.VARIETY_BALANCE to VarietyBalanceEvaluator(),
            MetricType.SCHEDULE_ADHERENCE to ScheduleAdherenceEvaluator(),
            MetricType.EARLY_BIRD to EarlyBirdEvaluator(),
            MetricType.COMEBACK to ComebackEvaluator(),
            MetricType.ONE_RM_TARGET to OneRmTargetEvaluator(),
            MetricType.FREQUENCY_TARGET to FrequencyGoalEvaluator(),
            MetricType.REPS_AT_WEIGHT to RepsAtWeightGoalEvaluator(),
            MetricType.BODY_WEIGHT_RELATION to BodyWeightRelationEvaluator(),
            MetricType.TIME_UNDER_TENSION to TimeUnderTensionEvaluator()
        )
    )

    override suspend fun getCatalog(): List<AchievementDefinition> {
        return achievementsDao.getDefinitions().map { it.toDomain() }
    }

    override fun observeInstances(): Flow<List<AchievementInstance>> {
        return achievementsDao.observeInstances().map { list -> list.map { it.toDomain() } }
    }

    override fun observeEvents(): Flow<AchievementEvent> = _events.asSharedFlow()

    override suspend fun createUserGoal(
        title: String,
        description: String?,
        kind: UserGoalKind,
        exerciseName: String?,
        targetValue: Double,
        secondaryValue: Double?,
        windowDays: Int?,
        deadlineAt: Instant?
    ): UserGoal {
        val now = Clock.System.now()
        val goalId = UUID.randomUUID().toString()
        val definitionId = "user_goal:$goalId"
        val instanceId = "user_goal_instance:$goalId"
        val metric = metricForGoalKind(kind)
        val definition = AchievementDefinition(
            id = definitionId,
            title = title,
            description = description ?: "",
            type = com.example.workoutlogger.domain.model.achievements.AchievementType.USER_GOAL,
            metric = metric,
            targetValue = targetValue,
            windowDays = windowDays,
            repeatable = false,
            tier = 1,
            iconKey = "goal_${kind.name.lowercase()}",
            sort = 10_000
        )

        val goal = UserGoal(
            goalId = goalId,
            title = title,
            description = description,
            kind = kind,
            exerciseName = exerciseName,
            targetValue = targetValue,
            secondaryValue = secondaryValue,
            windowDays = windowDays,
            deadlineAt = deadlineAt,
            createdAt = now
        )

        val metadata = com.example.workoutlogger.domain.model.achievements.AchievementMetadata(
            exerciseName = exerciseName,
            deadlineAt = deadlineAt,
            secondaryTarget = secondaryValue,
            windowDays = windowDays
        )

        val instance = AchievementInstance(
            instanceId = instanceId,
            definitionId = definitionId,
            createdAt = now,
            status = AchievementStatus.IN_PROGRESS,
            progress = Progress(
                current = 0.0,
                target = targetValue,
                percent = 0.0,
                unit = unitForGoal(kind)
            ),
            completedAt = null,
            userNotes = null,
            metadata = metadata
        )

        database.withTransaction {
            achievementsDao.insertUserGoal(goal.toEntity())
            achievementsDao.upsertDefinitions(listOf(definition.toEntity()))
            achievementsDao.upsertInstance(instance.toEntity())
        }

        evaluateNow()

        return goal
    }

    override suspend fun deleteGoal(goalId: String) {
        val definitionId = "user_goal:$goalId"
        database.withTransaction {
            achievementsDao.deleteInstancesByDefinition(definitionId)
            achievementsDao.deleteDefinition(definitionId)
            achievementsDao.deleteGoal(goalId)
        }
    }

    override suspend fun updateAchievementProgress(instanceId: String, progress: Progress, completedAt: Instant?) {
        val now = Clock.System.now()
        val status = statusForProgress(progress)
        achievementsDao.updateProgress(
            instanceId = instanceId,
            current = progress.current,
            target = progress.target,
            percent = progress.percent,
            unit = progress.unit,
            status = status,
            completedAt = completedAt ?: if (status == AchievementStatus.COMPLETED) now else null
        )
    }

    override suspend fun insertOrUpdateInstance(instance: AchievementInstance) {
        achievementsDao.upsertInstance(instance.toEntity())
    }

    override suspend fun seedDefinitions(definitions: List<AchievementDefinition>) {
        achievementsDao.upsertDefinitions(definitions.map { it.toEntity() })
    }

    override suspend fun listUserGoals(): List<UserGoal> {
        return achievementsDao.getUserGoals().map { it.toDomain() }
    }

    override fun observeUserGoals(): Flow<List<UserGoal>> {
        return achievementsDao.observeUserGoals().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun evaluateNow(): List<AchievementEvent> {
        val now = Clock.System.now()
        val aggregated = rebuildAggregatedData(now)
        val userSettings = loadUserSettings()
        val emittedEvents = mutableListOf<AchievementEvent>()

        database.withTransaction {
            achievementsDao.clearDailySummaries()
            if (aggregated.summaries.isNotEmpty()) {
                achievementsDao.upsertDailySummaries(aggregated.summaries)
            }

            val definitions = achievementsDao.getDefinitions().map { it.toDomain() }
            val defById = definitions.associateBy { it.id }
            val instances = achievementsDao.getInstances().map { it.toDomain() }
            val context = DataContext(
                workouts = aggregated.summaries.map { it.toDomain() },
                exerciseStats = aggregated.exerciseStats,
                schedules = aggregated.scheduleExpectations,
                userSettings = userSettings,
                timeZone = timeZone
            )

            instances.forEach { instance ->
                val def = defById[instance.definitionId] ?: return@forEach
                val evaluator = registry.get(def.metric) ?: return@forEach
                val progress = evaluator.progress(now, context, def, instance)
                val previousStatus = instance.status
                val status = statusForProgress(progress)
                val completedAt = if (status == AchievementStatus.COMPLETED) {
                    instance.completedAt ?: now
                } else {
                    instance.completedAt
                }
                achievementsDao.updateProgress(
                    instanceId = instance.instanceId,
                    current = progress.current,
                    target = progress.target,
                    percent = progress.percent,
                    unit = progress.unit,
                    status = status,
                    completedAt = completedAt
                )

                if (previousStatus != AchievementStatus.COMPLETED && status == AchievementStatus.COMPLETED && completedAt != null) {
                    emittedEvents += AchievementEvent.Completed(
                        instanceId = instance.instanceId,
                        definitionId = instance.definitionId,
                        title = def.title,
                        completedAt = completedAt
                    )
                }

                if (isUserGoal(def) && status != AchievementStatus.COMPLETED) {
                    val deadline = instance.metadata?.deadlineAt
                    if (deadline != null) {
                        val remainingMillis = deadline.toEpochMilliseconds() - now.toEpochMilliseconds()
                        if (remainingMillis in 1..MILLIS_72_HOURS) {
                            emittedEvents += AchievementEvent.GoalDeadlineApproaching(
                                instanceId = instance.instanceId,
                                definitionId = instance.definitionId,
                                title = def.title,
                                deadlineAt = deadline
                            )
                        }
                    }
                }
            }
        }

        emittedEvents.forEach { _events.tryEmit(it) }
        return emittedEvents
    }

    override suspend fun getDistinctExerciseNames(query: String?): List<String> {
        val prefix = query?.trim().orEmpty()
        return achievementsDao.getDistinctExerciseNames(prefix)
    }

    private fun isUserGoal(def: AchievementDefinition): Boolean = def.type == com.example.workoutlogger.domain.model.achievements.AchievementType.USER_GOAL

    private suspend fun loadUserSettings(): UserSettings {
        val unit = settingsRepository.defaultWeightUnit.first()
        val bodyWeight = settingsRepository.bodyWeightKg.first()
        return UserSettings(weightUnit = unit, bodyWeightKg = bodyWeight)
    }

    private fun WorkoutDailySummaryEntity.toDomain(): WorkoutSummary = WorkoutSummary(
        date = epochDayToLocalDate(dateEpochDay),
        sessions = workoutsCompleted,
        totalSets = totalSets,
        totalVolumeKg = totalVolumeKg,
        earlySessions = earlySessions,
        minutesActive = minutesActive,
        categoryMask = categoryMask,
        upperLowerMask = upperLowerMask
    )

    private fun metricForGoalKind(kind: UserGoalKind): MetricType = when (kind) {
        UserGoalKind.LIFT_WEIGHT -> MetricType.ONE_RM_TARGET
        UserGoalKind.REPS_AT_WEIGHT -> MetricType.REPS_AT_WEIGHT
        UserGoalKind.FREQUENCY_IN_WINDOW -> MetricType.FREQUENCY_TARGET
        UserGoalKind.BODY_WEIGHT_RELATION -> MetricType.BODY_WEIGHT_RELATION
        UserGoalKind.STREAK -> MetricType.STREAK_ACTIVE_DAYS
        UserGoalKind.TIME_UNDER_TENSION -> MetricType.TIME_UNDER_TENSION
    }

    private fun statusForProgress(progress: Progress): AchievementStatus = when {
        progress.percent >= 1.0 -> AchievementStatus.COMPLETED
        progress.current <= 0.0 -> AchievementStatus.LOCKED
        else -> AchievementStatus.IN_PROGRESS
    }

    private fun convertToKg(weight: Double, unit: EntityWeightUnit): Double = when (unit) {
        EntityWeightUnit.KG -> weight
        EntityWeightUnit.LB -> weight * 0.45359237
    }

    private fun classifyExercise(name: String): Pair<Int, Int> {
        val value = name.lowercase()
        var categoryMask = 0
        var upperLowerMask = 0

        if (pushKeywords.any { value.contains(it) }) {
            categoryMask = categoryMask or WorkoutDailySummaryEntity.CATEGORY_PUSH
            upperLowerMask = upperLowerMask or WorkoutDailySummaryEntity.UPPER_BODY
        }
        if (pullKeywords.any { value.contains(it) }) {
            categoryMask = categoryMask or WorkoutDailySummaryEntity.CATEGORY_PULL
            upperLowerMask = upperLowerMask or WorkoutDailySummaryEntity.UPPER_BODY
        }
        if (legKeywords.any { value.contains(it) }) {
            categoryMask = categoryMask or WorkoutDailySummaryEntity.CATEGORY_LEGS
            upperLowerMask = upperLowerMask or WorkoutDailySummaryEntity.LOWER_BODY
        }

        if (upperKeywords.any { value.contains(it) }) {
            upperLowerMask = upperLowerMask or WorkoutDailySummaryEntity.UPPER_BODY
        }
        if (lowerKeywords.any { value.contains(it) }) {
            upperLowerMask = upperLowerMask or WorkoutDailySummaryEntity.LOWER_BODY
        }

        if (categoryMask == 0) {
            categoryMask = WorkoutDailySummaryEntity.CATEGORY_PUSH
        }
        if (upperLowerMask == 0) {
            upperLowerMask = WorkoutDailySummaryEntity.UPPER_BODY
        }
        return categoryMask to upperLowerMask
    }

    private val pushKeywords = setOf("press", "push", "bench", "dip", "overhead")
    private val pullKeywords = setOf("row", "pull", "chin", "lat", "deadlift")
    private val legKeywords = setOf("squat", "leg", "lunge", "calf", "glute", "hip", "deadlift")
    private val upperKeywords = setOf("press", "row", "curl", "fly", "pull", "chin", "dip", "bench", "push", "shoulder")
    private val lowerKeywords = setOf("squat", "deadlift", "lunge", "leg", "calf", "glute", "hip", "hamstring", "quad")

    private suspend fun rebuildAggregatedData(now: Instant): AggregatedData {
        val sessions = sessionDao.getAllSessions()
        val daily = mutableMapOf<LocalDate, DailyAccumulator>()
        val exerciseAcc = mutableMapOf<String, ExerciseAccumulator>()
        val completedByWorkout = mutableMapOf<Long, MutableSet<LocalDate>>()

        val eightAm = LocalTime(hour = 8, minute = 0)

        sessions.forEach { sessionWithExercises ->
            val session = sessionWithExercises.session
            val sessionDate = (session.endedAt ?: session.startedAt).toLocalDateTime(timeZone).date
            val accumulator = daily.getOrPut(sessionDate) { DailyAccumulator() }
            accumulator.sessions += 1

            val startedLocal = session.startedAt.toLocalDateTime(timeZone)
            if (startedLocal.time < eightAm) {
                accumulator.earlySessions += 1
            }

            val durationMinutes = session.endedAt?.let {
                val duration = it - session.startedAt
                duration.inWholeMinutes.toInt().coerceAtLeast(0)
            } ?: 0
            accumulator.minutesActive += durationMinutes

            sessionWithExercises.exercises.forEach { exerciseWithSets ->
                val name = exerciseWithSets.exercise.exerciseName.trim()
                if (name.isEmpty()) return@forEach
                val normalised = name.lowercase()
                accumulator.uniqueExercises.add(normalised)
                val (categoryMask, upperLowerMask) = classifyExercise(name)
                accumulator.categoryMask = accumulator.categoryMask or categoryMask
                accumulator.upperLowerMask = accumulator.upperLowerMask or upperLowerMask

                val exerciseAccumulator = exerciseAcc.getOrPut(normalised) { ExerciseAccumulator(name) }
                val sets = exerciseWithSets.sets
                accumulator.totalSets += sets.size
                exerciseAccumulator.totalSets += sets.size

                sets.forEach { set ->
                    val reps = set.loggedReps ?: 0
                    val weight = set.loggedWeight
                    val unit = set.unit
                    val weightKg = weight?.let { convertToKg(it, unit) }
                    if (reps > 0 && weightKg != null && weightKg > 0.0) {
                        val volume = weightKg * reps
                        accumulator.totalVolumeKg += volume
                        exerciseAccumulator.totalVolumeKg += volume
                        exerciseAccumulator.performances.add(ExercisePerformance(weightKg, reps))

                        if (exerciseAccumulator.bestWeightKg == null || weightKg > exerciseAccumulator.bestWeightKg!!) {
                            exerciseAccumulator.bestWeightKg = weightKg
                        }
                        if (exerciseAccumulator.bestReps == null || reps > exerciseAccumulator.bestReps!!) {
                            exerciseAccumulator.bestReps = reps
                        }
                        val oneRm = OneRm.estimate(weightKg, reps)
                        if (exerciseAccumulator.bestOneRmKg == null || oneRm > exerciseAccumulator.bestOneRmKg!!) {
                            exerciseAccumulator.bestOneRmKg = oneRm
                        }
                    }
                }
            }

            if (session.status == SessionStatus.COMPLETED) {
                session.workoutId?.let { workoutId ->
                    completedByWorkout.getOrPut(workoutId) { mutableSetOf() }.add(sessionDate)
                }
            }
        }

        val summaries = daily.entries
            .sortedBy { it.key }
            .map { (date, acc) ->
                WorkoutDailySummaryEntity(
                    dateEpochDay = date.toEpochDayLong(),
                    workoutsCompleted = acc.sessions,
                    totalSets = acc.totalSets,
                    totalVolumeKg = acc.totalVolumeKg,
                    uniqueExercises = acc.uniqueExercises.size,
                    categoryMask = acc.categoryMask,
                    upperLowerMask = acc.upperLowerMask,
                    earlySessions = acc.earlySessions,
                    minutesActive = acc.minutesActive
                )
            }

        val exerciseStats = exerciseAcc.mapValues { (_, acc) ->
            ExerciseStats(
                name = acc.name,
                totalSets = acc.totalSets,
                totalVolumeKg = acc.totalVolumeKg,
                bestWeightKg = acc.bestWeightKg,
                bestReps = acc.bestReps,
                bestOneRmKg = acc.bestOneRmKg,
                performances = acc.performances
            )
        }

        val schedules = buildScheduleExpectations(completedByWorkout)

        return AggregatedData(summaries = summaries, exerciseStats = exerciseStats, scheduleExpectations = schedules)
    }

    private suspend fun buildScheduleExpectations(
        completedByWorkout: Map<Long, Set<LocalDate>>
    ): List<ScheduleExpectation> {
        val schedules = scheduleDao.getSchedules()
        if (schedules.isEmpty()) return emptyList()
        val end = Clock.System.now().toLocalDateTime(timeZone).date
        val start = end.minusDays(28)
        return schedules.filter { it.enabled }.map { entity ->
            val daysOfWeek = if (entity.daysOfWeek.isBlank()) emptySet() else entity.daysOfWeek.split(',')
                .mapNotNull { runCatching { kotlinx.datetime.DayOfWeek.valueOf(it) }.getOrNull() }
                .toSet()
            val expectedDates = mutableListOf<LocalDate>()
            var cursor = start
            while (cursor <= end) {
                if (cursor.dayOfWeek in daysOfWeek) {
                    expectedDates += cursor
                }
                cursor = cursor.plusDays(1)
            }
            val completed = completedByWorkout[entity.workoutId]?.filter { it in start..end } ?: emptyList()
            ScheduleExpectation(
                workoutId = entity.workoutId,
                expectedDates = expectedDates,
                completedDates = completed
            )
        }
    }

    private data class DailyAccumulator(
        var sessions: Int = 0,
        var totalSets: Int = 0,
        var totalVolumeKg: Double = 0.0,
        var categoryMask: Int = 0,
        var upperLowerMask: Int = 0,
        var earlySessions: Int = 0,
        var minutesActive: Int = 0,
        val uniqueExercises: MutableSet<String> = mutableSetOf()
    )

    private data class ExerciseAccumulator(
        val name: String,
        var totalSets: Int = 0,
        var totalVolumeKg: Double = 0.0,
        var bestWeightKg: Double? = null,
        var bestReps: Int? = null,
        var bestOneRmKg: Double? = null,
        val performances: MutableList<ExercisePerformance> = mutableListOf()
    )

    private data class AggregatedData(
        val summaries: List<WorkoutDailySummaryEntity>,
        val exerciseStats: Map<String, ExerciseStats>,
        val scheduleExpectations: List<ScheduleExpectation>
    )

    companion object {
        private const val MILLIS_72_HOURS = 72L * 60L * 60L * 1000L
    }
}

private val EPOCH_DAY_REFERENCE = LocalDate(1970, 1, 1)

private fun epochDayToLocalDate(value: Long): LocalDate =
    EPOCH_DAY_REFERENCE.plus(value.toInt(), DateTimeUnit.DAY)

private fun LocalDate.toEpochDayLong(): Long =
    EPOCH_DAY_REFERENCE.daysUntil(this).toLong()

private fun LocalDate.plusDays(days: Int): LocalDate =
    this.plus(days, DateTimeUnit.DAY)

private fun LocalDate.minusDays(days: Int): LocalDate =
    this.minus(days, DateTimeUnit.DAY)
