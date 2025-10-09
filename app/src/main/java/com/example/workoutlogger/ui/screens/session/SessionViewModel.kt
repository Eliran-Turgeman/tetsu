package com.example.workoutlogger.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutlogger.domain.model.PreviousPerformance
import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WorkoutStatus
import com.example.workoutlogger.domain.usecase.session.CancelSessionUseCase
import com.example.workoutlogger.domain.usecase.session.DeleteSessionExerciseUseCase
import com.example.workoutlogger.domain.usecase.session.DeleteSetLogUseCase
import com.example.workoutlogger.domain.usecase.session.FinishSessionUseCase
import com.example.workoutlogger.domain.usecase.session.GetPreviousPerformanceUseCase
import com.example.workoutlogger.domain.usecase.session.ObserveSessionUseCase
import com.example.workoutlogger.domain.usecase.session.UpdateExerciseOrderUseCase
import com.example.workoutlogger.domain.usecase.session.UpsertSessionExerciseUseCase
import com.example.workoutlogger.domain.usecase.session.UpsertSetLogUseCase
import com.example.workoutlogger.domain.usecase.settings.ObserveDefaultWeightUnitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSessionUseCase: ObserveSessionUseCase,
    observeDefaultWeightUnitUseCase: ObserveDefaultWeightUnitUseCase,
    private val getPreviousPerformanceUseCase: GetPreviousPerformanceUseCase,
    private val upsertSetLogUseCase: UpsertSetLogUseCase,
    private val deleteSetLogUseCase: DeleteSetLogUseCase,
    private val upsertSessionExerciseUseCase: UpsertSessionExerciseUseCase,
    private val deleteSessionExerciseUseCase: DeleteSessionExerciseUseCase,
    private val updateExerciseOrderUseCase: UpdateExerciseOrderUseCase,
    private val finishSessionUseCase: FinishSessionUseCase,
    private val cancelSessionUseCase: CancelSessionUseCase
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<String>("sessionId")?.toLong()
        ?: error("Session id required")

    private val previousPerformances = MutableStateFlow<Map<Long, PreviousPerformance?>>(emptyMap())

    val uiState: StateFlow<SessionUiState>

    private val _events = MutableStateFlow<SessionEvent?>(null)
    val events: StateFlow<SessionEvent?> = _events.asStateFlow()

    init {
        val sessionFlow = observeSessionUseCase(sessionId)
        val defaultUnitFlow = observeDefaultWeightUnitUseCase()

        uiState = combine(sessionFlow, previousPerformances, defaultUnitFlow) { session, previous, defaultUnit ->
            val exercises = session?.exercises?.map { exercise ->
                SessionExerciseUi(
                    id = exercise.id ?: 0L,
                    position = exercise.position,
                    name = exercise.exerciseName,
                    supersetId = exercise.supersetGroupId,
                    sets = exercise.sets.map { set ->
                        SessionSetUi(
                            id = set.id,
                            index = set.setIndex,
                            targetRange = formatRange(set.targetRepsMin, set.targetRepsMax),
                            reps = set.loggedReps?.toString().orEmpty(),
                            weight = set.loggedWeight?.toString().orEmpty(),
                            unit = set.unit,
                            note = set.note.orEmpty()
                        )
                    },
                    previousPerformance = buildPreviousUi(previous[exercise.id ?: -1L])
                )
            }.orEmpty()

            SessionUiState(
                session = session,
                exercises = exercises.sortedBy { it.position },
                defaultUnit = defaultUnit,
                isLoading = session == null
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState(isLoading = true))

        viewModelScope.launch {
            observeSessionUseCase(sessionId).collect { session ->
                if (session != null) {
                    fetchPreviousPerformances(session)
                }
            }
        }
    }

    private var fetchJob: Job? = null

    private fun fetchPreviousPerformances(session: WorkoutSession) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val map = mutableMapOf<Long, PreviousPerformance?>()
            session.exercises.forEach { exercise ->
                val previous = getPreviousPerformanceUseCase(
                    exerciseName = exercise.exerciseName,
                    before = session.startedAt
                )
                map[exercise.id ?: return@forEach] = previous
            }
            previousPerformances.value = map
        }
    }

    fun clearEvent() {
        _events.value = null
    }

    fun updateSetLog(
        exerciseId: Long,
        setId: Long?,
        setIndex: Int,
        reps: String,
        weight: String,
        unit: WeightUnit,
        note: String?
    ) {
        viewModelScope.launch {
            val repsInt = reps.toIntOrNull()
            val weightDouble = weight.toDoubleOrNull()
            val setLog = SessionSetLog(
                id = setId,
                sessionExerciseId = exerciseId,
                setIndex = setIndex,
                targetRepsMin = null,
                targetRepsMax = null,
                loggedReps = repsInt,
                loggedWeight = weightDouble,
                unit = unit,
                note = note
            )
            upsertSetLogUseCase(exerciseId, setLog)
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            deleteSetLogUseCase(setId)
        }
    }

    fun addSet(exercise: SessionExerciseUi, defaultUnit: WeightUnit) {
        viewModelScope.launch {
            val newSet = SessionSetLog(
                id = null,
                sessionExerciseId = exercise.id,
                setIndex = exercise.sets.size,
                targetRepsMin = parseRangeMin(exercise.sets.firstOrNull()?.targetRange),
                targetRepsMax = parseRangeMax(exercise.sets.firstOrNull()?.targetRange),
                loggedReps = null,
                loggedWeight = null,
                unit = defaultUnit,
                note = null
            )
            upsertSetLogUseCase(exercise.id, newSet)
        }
    }

    fun updateExerciseOrder(exerciseIds: List<Long>) {
        viewModelScope.launch {
            updateExerciseOrderUseCase(sessionId, exerciseIds)
        }
    }

    fun addExercise(
        name: String,
        supersetId: String?,
        sets: Int,
        repsMin: Int?,
        repsMax: Int?,
        defaultUnit: WeightUnit
    ) {
        viewModelScope.launch {
            val existing = uiState.value.session?.exercises?.size ?: 0
            val sessionsSets = buildList {
                repeat(sets.coerceAtLeast(0)) { index ->
                    add(
                        SessionSetLog(
                            id = null,
                            sessionExerciseId = null,
                            setIndex = index,
                            targetRepsMin = repsMin,
                            targetRepsMax = repsMax,
                            loggedReps = null,
                            loggedWeight = null,
                            unit = defaultUnit,
                            note = null
                        )
                    )
                }
            }
            val exercise = SessionExercise(
                id = null,
                sessionId = sessionId,
                position = existing,
                supersetGroupId = supersetId?.ifBlank { null },
                exerciseName = name,
                sets = sessionsSets
            )
            upsertSessionExerciseUseCase(sessionId, exercise)
        }
    }

    fun removeExercise(exerciseId: Long) {
        viewModelScope.launch {
            deleteSessionExerciseUseCase(exerciseId)
        }
    }

    fun finishSession() {
        viewModelScope.launch {
            finishSessionUseCase(sessionId)
            _events.value = SessionEvent.SessionFinished
        }
    }

    fun cancelSession() {
        viewModelScope.launch {
            cancelSessionUseCase(sessionId)
            _events.value = SessionEvent.SessionCancelled
        }
    }

    private fun formatRange(min: Int?, max: Int?): String? {
        return when {
            min == null && max == null -> null
            min != null && max != null && min == max -> min.toString()
            min != null && max != null -> "$min-$max"
            min != null -> min.toString()
            else -> null
        }
    }

    private fun parseRangeMin(range: String?): Int? = range?.substringBefore('-')?.toIntOrNull()
    private fun parseRangeMax(range: String?): Int? = range?.substringAfter('-', missingDelimiterValue = range ?: "")?.toIntOrNull()

    private fun buildPreviousUi(previous: PreviousPerformance?): PreviousPerformanceUi? {
        previous ?: return null
        val best = previous.bestSet
        val setsSummary = previous.sets.joinToString(separator = "; ") { set ->
            val reps = set.loggedReps?.toString() ?: "-"
            val weight = set.loggedWeight?.toString() ?: "-"
            "$reps x $weight ${set.unit.name}"
        }
        val bestSummary = best?.let { set ->
            val reps = set.loggedReps?.toString() ?: "-"
            val weight = set.loggedWeight?.toString() ?: "-"
            "$weight ${set.unit.name} × $reps"
        }
        return PreviousPerformanceUi(
            summary = setsSummary,
            best = bestSummary
        )
    }
}

data class SessionUiState(
    val session: WorkoutSession? = null,
    val exercises: List<SessionExerciseUi> = emptyList(),
    val defaultUnit: WeightUnit = WeightUnit.KG,
    val isLoading: Boolean = false
)

data class SessionExerciseUi(
    val id: Long,
    val position: Int,
    val name: String,
    val supersetId: String?,
    val sets: List<SessionSetUi>,
    val previousPerformance: PreviousPerformanceUi?
)

data class SessionSetUi(
    val id: Long?,
    val index: Int,
    val targetRange: String?,
    val reps: String,
    val weight: String,
    val unit: WeightUnit,
    val note: String
)

data class PreviousPerformanceUi(
    val summary: String,
    val best: String?
)

enum class SessionEvent {
    SessionFinished,
    SessionCancelled
}
