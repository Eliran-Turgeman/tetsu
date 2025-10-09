package com.example.workoutlogger.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.usecase.session.ObserveActiveSessionUseCase
import com.example.workoutlogger.domain.usecase.session.StartSessionFromWorkoutUseCase
import com.example.workoutlogger.domain.usecase.workout.ObserveWorkoutsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeWorkouts: ObserveWorkoutsUseCase,
    observeActiveSession: ObserveActiveSessionUseCase,
    private val startSessionFromWorkout: StartSessionFromWorkoutUseCase
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        observeWorkouts(),
        observeActiveSession()
    ) { workouts, activeSession ->
        DashboardUiState(
            workouts = workouts,
            activeSession = activeSession
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState())

    fun clearError() {
        _error.value = null
    }

    fun startSession(workoutId: Long, onResult: (Long?) -> Unit) {
        viewModelScope.launch {
            val sessionId = runCatching {
                startSessionFromWorkout(workoutId).id ?: error("Session id missing")
            }.onFailure { throwable ->
                _error.value = throwable.message
            }.getOrNull()
            onResult(sessionId)
        }
    }
}

data class DashboardUiState(
    val workouts: List<Workout> = emptyList(),
    val activeSession: WorkoutSession? = null
)
