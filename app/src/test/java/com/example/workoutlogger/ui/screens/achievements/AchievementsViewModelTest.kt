package com.example.workoutlogger.ui.screens.achievements

import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.model.achievements.AchievementStatus
import com.example.workoutlogger.domain.model.achievements.AchievementType
import com.example.workoutlogger.domain.model.achievements.MetricType
import com.example.workoutlogger.domain.model.achievements.Progress
import com.example.workoutlogger.domain.model.achievements.UserGoalKind
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WorkoutStatus
import com.example.workoutlogger.domain.repository.AchievementsRepository
import com.example.workoutlogger.domain.repository.SessionRepository
import com.example.workoutlogger.domain.repository.SettingsRepository
import com.example.workoutlogger.domain.usecase.achievements.CreateUserGoalUseCase
import com.example.workoutlogger.domain.usecase.achievements.DeleteUserGoalUseCase
import com.example.workoutlogger.domain.usecase.achievements.EvaluateAchievementsUseCase
import com.example.workoutlogger.domain.usecase.achievements.GetAchievementCatalogUseCase
import com.example.workoutlogger.domain.usecase.achievements.GetExerciseNameSuggestionsUseCase
import com.example.workoutlogger.domain.usecase.achievements.ObserveAchievementInstancesUseCase
import com.example.workoutlogger.domain.usecase.achievements.ObserveUserGoalsUseCase
import com.example.workoutlogger.domain.usecase.heatmap.ObserveHeatmapUseCase
import com.example.workoutlogger.domain.usecase.session.ObserveSessionsInRangeUseCase
import com.example.workoutlogger.domain.usecase.settings.ObserveBodyWeightUseCase
import com.example.workoutlogger.domain.usecase.settings.ObserveDefaultWeightUnitUseCase
import com.example.workoutlogger.domain.repository.WorkoutRepository
import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.model.WorkoutSchedule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @BeforeTest
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun stateIncludesInProgressAchievement() = scope.runTest {
        val sessionRepository = FakeSessionRepository()
        val achievementsRepository = FakeAchievementsRepository()
        val settingsRepository = FakeSettingsRepository()
        val workoutRepository = FakeWorkoutRepository(listOf("Bench Press", "Deadlift"))

        val definition = AchievementDefinition(
            id = "first_workout",
            title = "First Workout",
            description = "Log a session",
            type = AchievementType.FIRSTS,
            metric = MetricType.FIRST_WORKOUT,
            targetValue = 1.0,
            windowDays = null,
            repeatable = false,
            tier = 1,
            iconKey = "achievement_first_workout",
            sort = 0
        )
        achievementsRepository.catalog = listOf(definition)
        achievementsRepository.instances.value = listOf(
            AchievementInstance(
                instanceId = "instance-1",
                definitionId = definition.id,
                createdAt = Instant.parse("2024-05-01T00:00:00Z"),
                status = AchievementStatus.IN_PROGRESS,
                progress = Progress(0.0, 1.0, 0.0, "workouts"),
                completedAt = null,
                userNotes = null,
                metadata = null
            )
        )

        sessionRepository.emitSession(sampleSession())

        val viewModel = AchievementsViewModel(
            observeHeatmapUseCase = ObserveHeatmapUseCase(sessionRepository),
            observeSessionsInRangeUseCase = ObserveSessionsInRangeUseCase(sessionRepository),
            observeAchievementInstancesUseCase = ObserveAchievementInstancesUseCase(achievementsRepository),
            observeUserGoalsUseCase = ObserveUserGoalsUseCase(achievementsRepository),
            getAchievementCatalogUseCase = GetAchievementCatalogUseCase(achievementsRepository),
            createUserGoalUseCase = CreateUserGoalUseCase(achievementsRepository),
            deleteUserGoalUseCase = DeleteUserGoalUseCase(achievementsRepository),
            evaluateAchievementsUseCase = EvaluateAchievementsUseCase(achievementsRepository),
            observeDefaultWeightUnitUseCase = ObserveDefaultWeightUnitUseCase(settingsRepository),
            observeBodyWeightUseCase = ObserveBodyWeightUseCase(settingsRepository),
            getExerciseNameSuggestionsUseCase = GetExerciseNameSuggestionsUseCase(workoutRepository)
        )

        val state = viewModel.uiState.value
        assertTrue(state.inProgress.isNotEmpty())
        assertEquals("First Workout", state.inProgress.first().title)
        assertTrue(state.weeks.isNotEmpty())
    }

    private fun sampleSession(): WorkoutSession {
        val start = LocalDate(2024, 5, 1).atStartOfDayIn(TimeZone.UTC)
        val exercise = SessionExercise(
            id = 1L,
            sessionId = 1L,
            position = 0,
            supersetGroupId = null,
            exerciseName = "Bench Press",
            sets = listOf(
                SessionSetLog(
                    id = 1L,
                    sessionExerciseId = 1L,
                    setIndex = 0,
                    loggedReps = 10,
                    loggedWeight = 60.0,
                    unit = WeightUnit.KG
                )
            )
        )
        return WorkoutSession(
            id = 1L,
            workoutId = 1L,
            workoutNameSnapshot = "Push",
            startedAt = start,
            endedAt = start,
            status = WorkoutStatus.COMPLETED,
            exercises = listOf(exercise)
        )
    }
}

private class FakeSessionRepository : SessionRepository {
    private val sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())

    fun emitSession(session: WorkoutSession) {
        sessions.update { it + session }
    }

    override fun observeActiveSession(): Flow<WorkoutSession?> = flowOf(null)

    override fun observeSession(sessionId: Long): Flow<WorkoutSession?> = flowOf(null)

    override fun observeSessionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkoutSession>> = sessions

    override suspend fun startSessionFromWorkout(workoutId: Long, startedAt: Instant): WorkoutSession = throw UnsupportedOperationException()

    override suspend fun startAdHocSession(name: String, startedAt: Instant): WorkoutSession = throw UnsupportedOperationException()

    override suspend fun upsertSessionExercise(sessionId: Long, exercise: SessionExercise): Long = 0L

    override suspend fun updateSessionExerciseOrder(sessionId: Long, exerciseIdsInOrder: List<Long>) {}

    override suspend fun deleteSessionExercise(exerciseId: Long) {}

    override suspend fun upsertSetLog(exerciseId: Long, setLog: SessionSetLog): Long = 0L

    override suspend fun deleteSetLog(setLogId: Long) {}

    override suspend fun finishSession(sessionId: Long, endedAt: Instant) {}

    override suspend fun cancelSession(sessionId: Long) {}

    override suspend fun getPreviousPerformance(exerciseName: String, before: Instant) = null
}

private class FakeAchievementsRepository : AchievementsRepository {
    val instances = MutableStateFlow<List<AchievementInstance>>(emptyList())
    private val events = MutableStateFlow<com.example.workoutlogger.domain.model.achievements.AchievementEvent?>(null)
    var catalog: List<AchievementDefinition> = emptyList()

    override suspend fun getCatalog(): List<AchievementDefinition> = catalog

    override fun observeInstances(): Flow<List<AchievementInstance>> = instances

    override suspend fun createUserGoal(
        title: String,
        description: String?,
        kind: UserGoalKind,
        exerciseName: String?,
        targetValue: Double,
        secondaryValue: Double?,
        windowDays: Int?,
        deadlineAt: Instant?
    ) = throw UnsupportedOperationException()

    override suspend fun deleteGoal(goalId: String) {}

    override suspend fun updateAchievementProgress(instanceId: String, progress: Progress, completedAt: Instant?) {}

    override suspend fun insertOrUpdateInstance(instance: AchievementInstance) {}

    override suspend fun seedDefinitions(definitions: List<AchievementDefinition>) {}

    override suspend fun listUserGoals() = emptyList<com.example.workoutlogger.domain.model.achievements.UserGoal>()

    override fun observeUserGoals(): Flow<List<com.example.workoutlogger.domain.model.achievements.UserGoal>> = flowOf(emptyList())

    override suspend fun evaluateNow(): List<com.example.workoutlogger.domain.model.achievements.AchievementEvent> = emptyList()

    override suspend fun getDistinctExerciseNames(query: String?): List<String> = emptyList()

    override fun observeEvents(): Flow<com.example.workoutlogger.domain.model.achievements.AchievementEvent> = events.filterNotNull()
}

private class FakeWorkoutRepository(
    private val names: List<String>
) : WorkoutRepository {
    override fun observeWorkouts(): Flow<List<Workout>> = flowOf(emptyList())

    override suspend fun getWorkout(id: Long): Workout? = null

    override suspend fun upsertWorkout(workout: Workout): Long = 0L

    override suspend fun deleteWorkout(id: Long) {}

    override fun observeScheduleForWorkout(workoutId: Long): Flow<WorkoutSchedule?> = flowOf(null)

    override suspend fun getScheduleForWorkout(workoutId: Long): WorkoutSchedule? = null

    override fun observeSchedules(): Flow<List<WorkoutSchedule>> = flowOf(emptyList())

    override suspend fun upsertSchedule(schedule: WorkoutSchedule) {}

    override suspend fun deleteScheduleForWorkout(workoutId: Long) {}

    override suspend fun getDistinctExerciseNames(): List<String> = names
}

private class FakeSettingsRepository : SettingsRepository {
    private val unit = MutableStateFlow(WeightUnit.KG)
    private val bodyWeight = MutableStateFlow<Double?>(null)

    override val defaultWeightUnit: Flow<WeightUnit> = unit

    override suspend fun setDefaultWeightUnit(unit: WeightUnit) {
        this.unit.value = unit
    }

    override val notificationPermissionRequested: Flow<Boolean> = flowOf(false)

    override suspend fun setNotificationPermissionRequested(requested: Boolean) {}

    override val bodyWeightKg: Flow<Double?> = bodyWeight

    override suspend fun setBodyWeightKg(weightKg: Double?) {
        bodyWeight.value = weightKg
    }
}
