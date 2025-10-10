package com.example.workoutlogger.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutlogger.domain.model.HeatmapEntry
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.model.achievements.AchievementStatus
import com.example.workoutlogger.domain.model.achievements.Progress
import com.example.workoutlogger.domain.model.achievements.UserGoalKind
import com.example.workoutlogger.domain.usecase.achievements.CreateUserGoalUseCase
import com.example.workoutlogger.domain.usecase.achievements.DeleteUserGoalUseCase
import com.example.workoutlogger.domain.usecase.achievements.GetAchievementCatalogUseCase
import com.example.workoutlogger.domain.usecase.achievements.ObserveAchievementInstancesUseCase
import com.example.workoutlogger.domain.usecase.achievements.ObserveUserGoalsUseCase
import com.example.workoutlogger.domain.usecase.achievements.EvaluateAchievementsUseCase
import com.example.workoutlogger.domain.usecase.achievements.GetExerciseNameSuggestionsUseCase
import com.example.workoutlogger.domain.usecase.heatmap.ObserveHeatmapUseCase
import com.example.workoutlogger.domain.usecase.session.ObserveSessionsInRangeUseCase
import com.example.workoutlogger.domain.usecase.settings.ObserveBodyWeightUseCase
import com.example.workoutlogger.domain.usecase.settings.ObserveDefaultWeightUnitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    observeHeatmapUseCase: ObserveHeatmapUseCase,
    observeSessionsInRangeUseCase: ObserveSessionsInRangeUseCase,
    observeAchievementInstancesUseCase: ObserveAchievementInstancesUseCase,
    observeUserGoalsUseCase: ObserveUserGoalsUseCase,
    private val getAchievementCatalogUseCase: GetAchievementCatalogUseCase,
    private val createUserGoalUseCase: CreateUserGoalUseCase,
    private val deleteUserGoalUseCase: DeleteUserGoalUseCase,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase,
    observeDefaultWeightUnitUseCase: ObserveDefaultWeightUnitUseCase,
    observeBodyWeightUseCase: ObserveBodyWeightUseCase,
    private val getExerciseNameSuggestionsUseCase: GetExerciseNameSuggestionsUseCase
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()
    private val today = Clock.System.now().toLocalDateTime(timeZone).date
    private val startDate = today.minus(12, DateTimeUnit.MONTH)
    private val normalizedStart = startDate.minusDays((startDate.dayOfWeek.ordinal + 1) % 7)
    private val endDate = today

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _goalSheetState = MutableStateFlow(GoalSheetState())
    private val _catalog = MutableStateFlow<List<AchievementDefinition>>(emptyList())
    private val _pendingDeletion = MutableStateFlow<AchievementCardUi?>(null)

    private val templateExercises = MutableStateFlow<List<String>>(emptyList())

    private val numberFormat = NumberFormat.getNumberInstance()

    init {
        viewModelScope.launch {
            _catalog.value = getAchievementCatalogUseCase()
        }

        viewModelScope.launch {
            templateExercises.value = getExerciseNameSuggestionsUseCase().sorted()
        }
    }

    private val heatmapFlow = observeHeatmapUseCase(timeZone)
    private val sessionsFlow = observeSessionsInRangeUseCase(normalizedStart, endDate)
    private val achievementsFlow = observeAchievementInstancesUseCase()
    private val weightUnitFlow = observeDefaultWeightUnitUseCase()
    private val bodyWeightFlow = observeBodyWeightUseCase()
    private val goalsFlow = observeUserGoalsUseCase()

    private val baseInputs = combine(
        heatmapFlow,
        sessionsFlow,
        achievementsFlow,
        weightUnitFlow,
        bodyWeightFlow
    ) { entries, sessions, instances, weightUnit, bodyWeight ->
        PrimaryInputs(
            entries = entries,
            sessions = sessions,
            instances = instances,
            weightUnit = weightUnit,
            bodyWeight = bodyWeight,
            templateExercises = emptyList()
        )
    }

    private val primaryInputs = combine(baseInputs, templateExercises) { base, exercises ->
        base.copy(templateExercises = exercises)
    }

    private val combinedInputs = combine(
        primaryInputs,
        _selectedDate,
        _goalSheetState,
        _catalog,
        _pendingDeletion
    ) { primary, selectedDate, goalSheetState, catalog, pendingDeletion ->
        CombinedInputs(
            entries = primary.entries,
            sessions = primary.sessions,
            instances = primary.instances,
            weightUnit = primary.weightUnit,
            bodyWeight = primary.bodyWeight,
            templateExercises = primary.templateExercises,
            selectedDate = selectedDate,
            goalSheetState = goalSheetState,
            catalog = catalog,
            pendingDeletion = pendingDeletion
        )
    }

    val uiState: StateFlow<AchievementsUiState> = combine(combinedInputs, goalsFlow) { input, userGoals ->
        val sessionsByDate = input.sessions.groupBy { session ->
            (session.endedAt ?: session.startedAt).toLocalDateTime(timeZone).date
        }
        val grid = buildGrid(input.entries, sessionsByDate)
        val selectedSessions = input.selectedDate?.let { sessionsByDate[it].orEmpty() } ?: emptyList()

        val defById = input.catalog.associateBy { it.id }
        val (inProgress, completed) = input.instances.partition { it.status != AchievementStatus.COMPLETED }

        val inProgressUi = inProgress.mapNotNull { instance ->
            val def = defById[instance.definitionId] ?: return@mapNotNull null
            instance.toCardUi(def, input.weightUnit, input.bodyWeight)
        }.sortedWith(compareBy<AchievementCardUi> { it.sort }.thenBy { it.title })

        val completedUi = completed.mapNotNull { instance ->
            val def = defById[instance.definitionId] ?: return@mapNotNull null
            instance.toBadgeUi(def)
        }.sortedByDescending { it.completedOn }

        val hasAchievements = inProgressUi.isNotEmpty() || completedUi.isNotEmpty() || userGoals.isNotEmpty()

        AchievementsUiState(
            weeks = grid,
            selectedDate = input.selectedDate,
            selectedSessions = selectedSessions,
            inProgress = inProgressUi,
            completed = completedUi,
            isGoalSheetOpen = input.goalSheetState.isOpen,
            goalDraft = input.goalSheetState.draft,
            goalError = input.goalSheetState.error,
            templateExercises = input.templateExercises,
            isSubmittingGoal = input.goalSheetState.isSubmitting,
            weightUnit = input.weightUnit,
            bodyWeightKg = input.bodyWeight,
            pendingDeletion = input.pendingDeletion,
            emptyState = !hasAchievements
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState(isLoading = true))

    fun onDayClick(day: HeatmapDayUi) {
        _selectedDate.value = if (_selectedDate.value == day.date) null else day.date
    }

    fun onFabClick() {
        _goalSheetState.value = GoalSheetState(isOpen = true)
    }

    fun onDismissGoalSheet() {
        _goalSheetState.value = GoalSheetState()
    }

    fun onSelectGoalKind(kind: UserGoalKind) {
        _goalSheetState.update { state ->
            state.copy(
                draft = state.draft.copy(
                    kind = kind,
                    title = defaultTitleFor(kind)
                ),
                error = null
            )
        }
    }

    fun onUpdateGoalTitle(value: String) {
        _goalSheetState.update { state -> state.copy(draft = state.draft.copy(title = value), error = null) }
    }

    fun onUpdateGoalExercise(value: String) {
        _goalSheetState.update { state -> state.copy(draft = state.draft.copy(exerciseName = value), error = null) }
    }

    fun onUpdatePrimaryValue(value: String) {
        _goalSheetState.update { state -> state.copy(draft = state.draft.copy(primaryValue = value), error = null) }
    }

    fun onUpdateSecondaryValue(value: String) {
        _goalSheetState.update { state -> state.copy(draft = state.draft.copy(secondaryValue = value), error = null) }
    }

    fun onUpdateWindowValue(value: String) {
        _goalSheetState.update { state -> state.copy(draft = state.draft.copy(windowValue = value), error = null) }
    }

    fun onUpdateDeadline(value: LocalDate?) {
        _goalSheetState.update { state -> state.copy(draft = state.draft.copy(deadline = value), error = null) }
    }

    fun onConfirmDelete(goalCard: AchievementCardUi) {
        _pendingDeletion.value = goalCard
    }

    fun onDismissDelete() {
        _pendingDeletion.value = null
    }

    fun onDeleteConfirmed() {
        val toDelete = _pendingDeletion.value ?: return
        if (!toDelete.isUserGoal) {
            _pendingDeletion.value = null
            return
        }
        viewModelScope.launch {
            deleteUserGoalUseCase(goalIdFromDefinition(toDelete.definitionId))
            _pendingDeletion.value = null
        }
    }

    fun submitGoal(weightUnit: WeightUnit, bodyWeightKg: Double?, availableExercises: List<String>) {
        val state = _goalSheetState.value
        val draft = state.draft
        val kind = draft.kind ?: run {
            _goalSheetState.value = state.copy(error = GoalValidationError.KindRequired)
            return
        }

        val validation = validateDraft(draft, kind, weightUnit, bodyWeightKg, availableExercises)
        if (validation !is GoalDraftValidationResult.Valid) {
            _goalSheetState.update { it.copy(error = (validation as GoalDraftValidationResult.Invalid).error) }
            return
        }

        _goalSheetState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                createUserGoalUseCase(
                    title = draft.title.ifBlank { defaultTitleFor(kind) },
                    description = null,
                    kind = kind,
                    exerciseName = draft.exerciseName.takeIf { it.isNotBlank() },
                    targetValue = validation.targetValue,
                    secondaryValue = validation.secondaryValue,
                    windowDays = validation.windowDays,
                    deadlineAt = validation.deadline
                )
                evaluateAchievementsUseCase()
                _goalSheetState.value = GoalSheetState()
            } catch (t: Throwable) {
                _goalSheetState.update { it.copy(error = GoalValidationError.SubmissionFailed, isSubmitting = false) }
            }
        }
    }

    private fun buildGrid(
        entries: List<HeatmapEntry>,
        sessionsByDate: Map<LocalDate, List<WorkoutSession>>
    ): List<HeatmapWeekUi> {
        val entryMap = entries.associateBy { it.date }
        val totalDays = normalizedStart.daysUntil(endDate) + 1
        val days = (0 until totalDays).map { normalizedStart.plusDays(it) }
        val weeks = days.chunked(7)
        return weeks.map { weekDays ->
            HeatmapWeekUi(
                days = weekDays.map { date ->
                    val entry = entryMap[date]
                    HeatmapDayUi(
                        date = date,
                        hasWorkout = entry?.hasCompletedSession == true,
                        sessions = sessionsByDate[date].orEmpty()
                    )
                }
            )
        }
    }

    private fun AchievementInstance.toCardUi(
        definition: AchievementDefinition,
        weightUnit: WeightUnit,
        bodyWeightKg: Double?
    ): AchievementCardUi {
        val progressLabel = formatProgress(definition, progress, weightUnit, bodyWeightKg)
        val deadline = metadata?.deadlineAt?.toLocalDateTime(timeZone)?.date
        return AchievementCardUi(
            instanceId = instanceId,
            definitionId = definition.id,
            title = definition.title,
            subtitle = definition.description,
            progressLabel = progressLabel,
            percent = progress.percent.toFloat().coerceIn(0f, 1f),
            iconKey = definition.iconKey,
            sort = definition.sort,
            isUserGoal = isUserGoal(definition.id),
            deadline = deadline,
            completedAt = completedAt
        )
    }

    private fun AchievementInstance.toBadgeUi(definition: AchievementDefinition): AchievementBadgeUi? {
        val completed = completedAt ?: return null
        val localDate = completed.toLocalDateTime(timeZone).date
        return AchievementBadgeUi(
            instanceId = instanceId,
            title = definition.title,
            iconKey = definition.iconKey,
            completedOn = localDate
        )
    }

    private fun defaultTitleFor(kind: UserGoalKind): String = when (kind) {
        UserGoalKind.LIFT_WEIGHT -> "Lift heavier"
        UserGoalKind.REPS_AT_WEIGHT -> "Rep goal"
        UserGoalKind.FREQUENCY_IN_WINDOW -> "Stay consistent"
        UserGoalKind.BODY_WEIGHT_RELATION -> "Bodyweight milestone"
        UserGoalKind.STREAK -> "Streak challenge"
        UserGoalKind.TIME_UNDER_TENSION -> "Time on feet"
    }

    private fun formatProgress(
        definition: AchievementDefinition,
        progress: Progress,
        weightUnit: WeightUnit,
        bodyWeightKg: Double?
    ): String {
        return when (progress.unit) {
            "%" -> {
                val percent = (progress.current * 100).coerceIn(0.0, 100.0)
                val target = (definition.targetValue * 100).coerceIn(0.0, 100.0)
                "${percent}% / ${target}%"
            }
            "kg" -> {
                val unitLabel = if (weightUnit == WeightUnit.LB) "lb" else "kg"
                val current = formatWeight(progress.current, weightUnit)
                val target = formatWeight(progress.target.takeIf { it > 0 } ?: bodyWeightKg ?: 0.0, weightUnit)
                "$current / $target $unitLabel"
            }
            "seconds" -> {
                val current = formatDuration(progress.current)
                val target = formatDuration(progress.target)
                "$current / $target"
            }
            else -> {
                val current = numberFormat.format(progress.current.roundToInt())
                val target = if (progress.target > 0) numberFormat.format(progress.target.roundToInt()) else "—"
                "$current / $target ${progress.unit}"
            }
        }
    }

    private fun formatWeight(valueKg: Double, weightUnit: WeightUnit): String {
        val converted = if (weightUnit == WeightUnit.LB) kgToLb(valueKg) else valueKg
        return numberFormat.format(converted.roundToInt())
    }

    private fun kgToLb(valueKg: Double): Double = valueKg * 2.2046226218

    private fun formatDuration(seconds: Double): String {
        if (seconds <= 0) return "0m"
        val minutes = seconds / 60.0
        return numberFormat.format(minutes.roundToInt()) + "m"
    }

    private fun isUserGoal(definitionId: String): Boolean = definitionId.startsWith("user_goal")

    private fun goalIdFromDefinition(definitionId: String): String = definitionId.removePrefix("user_goal:")

    private fun validateDraft(
        draft: GoalDraft,
        kind: UserGoalKind,
        weightUnit: WeightUnit,
        bodyWeightKg: Double?,
        availableExercises: List<String>
    ): GoalDraftValidationResult {
        return when (kind) {
            UserGoalKind.LIFT_WEIGHT -> {
                if (draft.exerciseName.isBlank() || draft.exerciseName !in availableExercises) {
                    return invalid(GoalValidationError.ExerciseRequired)
                }
                val target = parseWeight(draft.primaryValue, weightUnit) ?: return invalid(GoalValidationError.TargetWeightRequired)
                GoalDraftValidationResult.Valid(
                    targetValue = target,
                    secondaryValue = null,
                    windowDays = null,
                    deadline = draft.deadline?.atStartOfDay(timeZone)
                )
            }
            UserGoalKind.REPS_AT_WEIGHT -> {
                if (draft.exerciseName.isBlank() || draft.exerciseName !in availableExercises) {
                    return invalid(GoalValidationError.ExerciseRequired)
                }
                val reps = draft.primaryValue.toIntOrNull()?.takeIf { it > 0 } ?: return invalid(GoalValidationError.RepsRequired)
                val weight = parseWeight(draft.secondaryValue, weightUnit) ?: return invalid(GoalValidationError.TargetWeightRequired)
                GoalDraftValidationResult.Valid(
                    targetValue = reps.toDouble(),
                    secondaryValue = weight,
                    windowDays = null,
                    deadline = draft.deadline?.atStartOfDay(timeZone)
                )
            }
            UserGoalKind.FREQUENCY_IN_WINDOW -> {
                val workouts = draft.primaryValue.toIntOrNull()?.takeIf { it > 0 } ?: return invalid(GoalValidationError.WorkoutsRequired)
                val weeks = draft.windowValue.toIntOrNull()?.takeIf { it > 0 } ?: return invalid(GoalValidationError.WindowRequired)
                GoalDraftValidationResult.Valid(
                    targetValue = workouts.toDouble(),
                    secondaryValue = null,
                    windowDays = weeks * 7,
                    deadline = draft.deadline?.atStartOfDay(timeZone)
                )
            }
            UserGoalKind.BODY_WEIGHT_RELATION -> {
                if (draft.exerciseName.isBlank() || draft.exerciseName !in availableExercises) {
                    return invalid(GoalValidationError.ExerciseRequired)
                }
                val bodyWeight = bodyWeightKg?.takeIf { it > 0 } ?: return invalid(GoalValidationError.BodyWeightMissing)
                GoalDraftValidationResult.Valid(
                    targetValue = bodyWeight,
                    secondaryValue = null,
                    windowDays = null,
                    deadline = draft.deadline?.atStartOfDay(timeZone)
                )
            }
            UserGoalKind.STREAK -> {
                val days = draft.primaryValue.toIntOrNull()?.takeIf { it > 0 } ?: return invalid(GoalValidationError.StreakRequired)
                GoalDraftValidationResult.Valid(
                    targetValue = days.toDouble(),
                    secondaryValue = null,
                    windowDays = null,
                    deadline = draft.deadline?.atStartOfDay(timeZone)
                )
            }
            UserGoalKind.TIME_UNDER_TENSION -> {
                val minutes = draft.primaryValue.toIntOrNull()?.takeIf { it > 0 } ?: return invalid(GoalValidationError.MinutesRequired)
                val window = draft.windowValue.toIntOrNull()?.takeIf { it > 0 } ?: return invalid(GoalValidationError.WindowRequired)
                GoalDraftValidationResult.Valid(
                    targetValue = minutes * 60.0,
                    secondaryValue = null,
                    windowDays = window,
                    deadline = draft.deadline?.atStartOfDay(timeZone)
                )
            }
        }
    }

    private fun parseWeight(input: String, weightUnit: WeightUnit): Double? {
        val value = input.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        return if (weightUnit == WeightUnit.LB) value / 2.2046226218 else value
    }

    private fun LocalDate.atStartOfDay(zone: TimeZone): Instant = this.atStartOfDayIn(zone)

    private fun LocalDate.minusDays(days: Int): LocalDate = this.minus(days, DateTimeUnit.DAY)

    private fun LocalDate.plusDays(days: Int): LocalDate = this.plus(days, DateTimeUnit.DAY)

    private fun LocalDate.daysUntil(other: LocalDate): Int {
        var current = this
        var count = 0
        while (current < other) {
            current = current.plusDays(1)
            count++
        }
        return count
    }

    private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
        value = transform(value)
    }

}

data class AchievementsUiState(
    val weeks: List<HeatmapWeekUi> = emptyList(),
    val selectedDate: LocalDate? = null,
    val selectedSessions: List<WorkoutSession> = emptyList(),
    val inProgress: List<AchievementCardUi> = emptyList(),
    val completed: List<AchievementBadgeUi> = emptyList(),
    val isGoalSheetOpen: Boolean = false,
    val goalDraft: GoalDraft = GoalDraft(),
    val goalError: GoalValidationError? = null,
    val templateExercises: List<String> = emptyList(),
    val isSubmittingGoal: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val bodyWeightKg: Double? = null,
    val pendingDeletion: AchievementCardUi? = null,
    val emptyState: Boolean = true,
    val isLoading: Boolean = false
)

private data class PrimaryInputs(
    val entries: List<HeatmapEntry>,
    val sessions: List<WorkoutSession>,
    val instances: List<AchievementInstance>,
    val weightUnit: WeightUnit,
    val bodyWeight: Double?,
    val templateExercises: List<String>
)

private data class CombinedInputs(
    val entries: List<HeatmapEntry>,
    val sessions: List<WorkoutSession>,
    val instances: List<AchievementInstance>,
    val weightUnit: WeightUnit,
    val bodyWeight: Double?,
    val templateExercises: List<String>,
    val selectedDate: LocalDate?,
    val goalSheetState: GoalSheetState,
    val catalog: List<AchievementDefinition>,
    val pendingDeletion: AchievementCardUi?
)

data class HeatmapWeekUi(val days: List<HeatmapDayUi>)

data class HeatmapDayUi(
    val date: LocalDate,
    val hasWorkout: Boolean,
    val sessions: List<WorkoutSession>
)

data class AchievementCardUi(
    val instanceId: String,
    val definitionId: String,
    val title: String,
    val subtitle: String,
    val progressLabel: String,
    val percent: Float,
    val iconKey: String,
    val sort: Int,
    val isUserGoal: Boolean,
    val deadline: LocalDate?,
    val completedAt: Instant?
)

data class AchievementBadgeUi(
    val instanceId: String,
    val title: String,
    val iconKey: String,
    val completedOn: LocalDate
)

data class GoalSheetState(
    val isOpen: Boolean = false,
    val draft: GoalDraft = GoalDraft(),
    val error: GoalValidationError? = null,
    val isSubmitting: Boolean = false
)

data class GoalDraft(
    val kind: UserGoalKind? = null,
    val title: String = "",
    val exerciseName: String = "",
    val primaryValue: String = "",
    val secondaryValue: String = "",
    val windowValue: String = "",
    val deadline: LocalDate? = null
)

sealed class GoalValidationError {
    data object KindRequired : GoalValidationError()
    data object ExerciseRequired : GoalValidationError()
    data object TargetWeightRequired : GoalValidationError()
    data object RepsRequired : GoalValidationError()
    data object WorkoutsRequired : GoalValidationError()
    data object WindowRequired : GoalValidationError()
    data object BodyWeightMissing : GoalValidationError()
    data object StreakRequired : GoalValidationError()
    data object MinutesRequired : GoalValidationError()
    data object SubmissionFailed : GoalValidationError()
}

sealed class GoalDraftValidationResult {
    data class Valid(
        val targetValue: Double,
        val secondaryValue: Double?,
        val windowDays: Int?,
        val deadline: Instant?
    ) : GoalDraftValidationResult()

    data class Invalid(val error: GoalValidationError) : GoalDraftValidationResult()
}

private fun invalid(error: GoalValidationError): GoalDraftValidationResult = GoalDraftValidationResult.Invalid(error)
