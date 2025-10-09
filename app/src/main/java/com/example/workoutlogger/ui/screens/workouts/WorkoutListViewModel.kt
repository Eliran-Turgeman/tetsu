package com.example.workoutlogger.ui.screens.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.usecase.session.StartSessionFromWorkoutUseCase
import com.example.workoutlogger.domain.usecase.workout.DeleteWorkoutUseCase
import com.example.workoutlogger.domain.usecase.workout.ObserveWorkoutsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WorkoutListViewModel @Inject constructor(
    observeWorkoutsUseCase: ObserveWorkoutsUseCase,
    private val deleteWorkoutUseCase: DeleteWorkoutUseCase,
    private val startSessionFromWorkoutUseCase: StartSessionFromWorkoutUseCase
) : ViewModel() {

    val workouts: StateFlow<List<Workout>> = observeWorkoutsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableStateFlow<WorkoutListEvent?>(null)
    val events: StateFlow<WorkoutListEvent?> = _events.asStateFlow()

    fun clearEvent() {
        _events.value = null
    }

    fun deleteWorkout(id: Long) {
        viewModelScope.launch {
            deleteWorkoutUseCase(id)
            _events.value = WorkoutListEvent.WorkoutDeleted
        }
    }

    fun startSession(workoutId: Long) {
        viewModelScope.launch {
            val session = startSessionFromWorkoutUseCase(workoutId)
            session.id?.let { sessionId ->
                _events.value = WorkoutListEvent.SessionStarted(sessionId)
            }
        }
    }
}

sealed interface WorkoutListEvent {
    data object WorkoutDeleted : WorkoutListEvent
    data class SessionStarted(val sessionId: Long) : WorkoutListEvent
}
