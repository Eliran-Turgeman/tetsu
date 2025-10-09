package com.example.workoutlogger.ui.screens.workouts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutlogger.domain.model.WorkoutItem
import com.example.workoutlogger.domain.model.WorkoutItemType
import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.usecase.workout.GetWorkoutUseCase
import com.example.workoutlogger.domain.usecase.workout.SaveWorkoutUseCase
import com.example.workoutlogger.domain.usecase.util.ParseRepsRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@HiltViewModel
class WorkoutEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWorkoutUseCase: GetWorkoutUseCase,
    private val saveWorkoutUseCase: SaveWorkoutUseCase,
    private val parseRepsRangeUseCase: ParseRepsRangeUseCase
) : ViewModel() {

    private val workoutId: Long? = savedStateHandle.get<String>("templateId")?.toLongOrNull()

    private val _state = MutableStateFlow(WorkoutEditorUiState())
    val state: StateFlow<WorkoutEditorUiState> = _state.asStateFlow()

    init {
        if (workoutId != null) {
            loadWorkout(workoutId)
        }
    }

    private fun loadWorkout(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val workout = getWorkoutUseCase(id)
            _state.value = if (workout != null) {
                WorkoutEditorUiState(
                    workoutName = workout.name,
                    createdAt = workout.createdAt,
                    items = workout.items.map { item ->
                        WorkoutEditorItemUi(
                            localId = item.id ?: item.position.toLong(),
                            type = item.type,
                            exerciseName = item.exerciseName.orEmpty(),
                            supersetId = item.supersetGroupId.orEmpty(),
                            sets = item.sets?.toString().orEmpty(),
                            repsRange = buildRepsRange(item.repsMin, item.repsMax)
                        )
                    }
                )
            } else {
                _state.value.copy(isLoading = false)
            }
        }
    }

    fun updateWorkoutName(name: String) {
        _state.value = _state.value.copy(workoutName = name)
    }

    fun updateItem(itemId: Long, transform: (WorkoutEditorItemUi) -> WorkoutEditorItemUi) {
        _state.value = _state.value.copy(
            items = _state.value.items.map { if (it.localId == itemId) transform(it) else it }
        )
    }

    fun addExercise() {
        val nextId = Clock.System.now().toEpochMilliseconds()
        _state.value = _state.value.copy(
            items = _state.value.items + WorkoutEditorItemUi(
                localId = nextId,
                type = WorkoutItemType.EXERCISE
            )
        )
    }

    fun addSupersetHeader(label: String) {
        val nextId = Clock.System.now().toEpochMilliseconds()
        _state.value = _state.value.copy(
            items = _state.value.items + WorkoutEditorItemUi(
                localId = nextId,
                type = WorkoutItemType.SUPERSET_HEADER,
                supersetId = label
            )
        )
    }

    fun removeItem(localId: Long) {
        _state.value = _state.value.copy(
            items = _state.value.items.filterNot { it.localId == localId }
        )
    }

    fun moveItemUp(localId: Long) {
        val items = _state.value.items.toMutableList()
        val index = items.indexOfFirst { it.localId == localId }
        if (index > 0) {
            items.removeAt(index).also { moved ->
                items.add(index - 1, moved)
            }
            _state.value = _state.value.copy(items = items)
        }
    }

    fun moveItemDown(localId: Long) {
        val items = _state.value.items.toMutableList()
        val index = items.indexOfFirst { it.localId == localId }
        if (index >= 0 && index < items.lastIndex) {
            items.removeAt(index).also { moved ->
                items.add(index + 1, moved)
            }
            _state.value = _state.value.copy(items = items)
        }
    }

    fun saveWorkout(onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val current = _state.value
            val workoutName = current.workoutName.trim()
            if (workoutName.isEmpty()) {
                _state.value = current.copy(error = WorkoutEditorError.NameRequired)
                return@launch
            }

            val itemsResult = current.items.mapIndexedNotNull { index, item ->
                when (item.type) {
                    WorkoutItemType.EXERCISE -> {
                        val exerciseName = item.exerciseName.trim()
                        if (exerciseName.isEmpty()) {
                            _state.value = current.copy(error = WorkoutEditorError.ExerciseNameRequired(item.localId))
                            return@launch
                        }
                        val sets = item.sets.toIntOrNull()
                        if (sets == null || sets < 0) {
                            _state.value = current.copy(error = WorkoutEditorError.InvalidSets(item.localId))
                            return@launch
                        }
                        val reps = if (item.repsRange.isBlank()) null else parseRepsRangeUseCase(item.repsRange)
                        if (item.repsRange.isNotBlank() && reps == null) {
                            _state.value = current.copy(error = WorkoutEditorError.InvalidRepsRange(item.localId))
                            return@launch
                        }
                        WorkoutItem(
                            id = null,
                            workoutId = workoutId,
                            position = index,
                            type = WorkoutItemType.EXERCISE,
                            supersetGroupId = item.supersetId.ifBlank { null },
                            exerciseName = exerciseName,
                            sets = sets,
                            repsMin = reps?.first,
                            repsMax = reps?.second
                        )
                    }
                    WorkoutItemType.SUPERSET_HEADER -> {
                        WorkoutItem(
                            id = null,
                            workoutId = workoutId,
                            position = index,
                            type = WorkoutItemType.SUPERSET_HEADER,
                            supersetGroupId = item.supersetId.ifBlank { null }
                        )
                    }
                }
            }

            val createdAt = current.createdAt ?: Clock.System.now()
            val workout = Workout(
                id = workoutId,
                name = workoutName,
                createdAt = createdAt,
                items = itemsResult
            )

            val savedId = saveWorkoutUseCase(workout)
            _state.value = current.copy(error = null, createdAt = createdAt)
            onSaved(savedId)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun buildRepsRange(min: Int?, max: Int?): String = when {
        min == null && max == null -> ""
        min != null && max != null && min == max -> min.toString()
        min != null && max != null -> "$min-$max"
        min != null -> min.toString()
        else -> ""
    }
}

data class WorkoutEditorUiState(
    val workoutName: String = "",
    val items: List<WorkoutEditorItemUi> = emptyList(),
    val createdAt: kotlinx.datetime.Instant? = null,
    val isLoading: Boolean = false,
    val error: WorkoutEditorError? = null
)

data class WorkoutEditorItemUi(
    val localId: Long,
    val type: WorkoutItemType,
    val exerciseName: String = "",
    val supersetId: String = "",
    val sets: String = "",
    val repsRange: String = ""
)

sealed interface WorkoutEditorError {
    data object NameRequired : WorkoutEditorError
    data class ExerciseNameRequired(val itemId: Long) : WorkoutEditorError
    data class InvalidSets(val itemId: Long) : WorkoutEditorError
    data class InvalidRepsRange(val itemId: Long) : WorkoutEditorError
}
