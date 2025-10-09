package com.example.workoutlogger.ui.screens.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutlogger.domain.model.WorkoutSchedule
import com.example.workoutlogger.domain.usecase.schedule.DeleteScheduleUseCase
import com.example.workoutlogger.domain.usecase.schedule.ObserveScheduleForWorkoutUseCase
import com.example.workoutlogger.domain.usecase.schedule.SaveScheduleUseCase
import com.example.workoutlogger.notifications.WorkoutReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeScheduleForWorkoutUseCase: ObserveScheduleForWorkoutUseCase,
    private val saveScheduleUseCase: SaveScheduleUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase,
    private val reminderScheduler: WorkoutReminderScheduler
) : ViewModel() {

    private val templateId: Long = savedStateHandle.get<String>("templateId")?.toLong()
        ?: error("Template id required")

    private val initialState = ScheduleUiState(templateId = templateId)
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeScheduleForWorkoutUseCase(templateId).collect { schedule ->
                if (schedule != null) {
                    _state.value = initialState.copy(
                        templateId = templateId,
                        days = schedule.daysOfWeek.toMutableSet(),
                        hour = schedule.notifyHour,
                        minute = schedule.notifyMinute,
                        enabled = schedule.enabled
                    )
                }
            }
        }
    }

    fun toggleDay(day: DayOfWeek) {
        val days = _state.value.days.toMutableSet()
        if (!days.add(day)) {
            days.remove(day)
        }
        _state.value = _state.value.copy(days = days)
    }

    fun updateTime(hour: Int, minute: Int) {
        _state.value = _state.value.copy(hour = hour, minute = minute)
    }

    fun toggleEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun save(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val current = _state.value
            val enabled = current.enabled && current.days.isNotEmpty()
            if (!enabled) {
                deleteScheduleUseCase(templateId)
                reminderScheduler.cancel(templateId)
                onResult(false)
                return@launch
            }
            val schedule = WorkoutSchedule(
                id = null,
                workoutId = templateId,
                daysOfWeek = current.days,
                notifyHour = current.hour,
                notifyMinute = current.minute,
                enabled = true
            )
            saveScheduleUseCase(schedule)
            reminderScheduler.scheduleTemplate(templateId)
            onResult(true)
        }
    }
}

data class ScheduleUiState(
    val templateId: Long,
    val days: Set<DayOfWeek> = emptySet(),
    val hour: Int = 8,
    val minute: Int = 0,
    val enabled: Boolean = false
)
