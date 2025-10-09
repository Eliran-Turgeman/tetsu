package com.example.workoutlogger.ui.screens.heatmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutlogger.domain.model.HeatmapEntry
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.usecase.heatmap.ObserveHeatmapUseCase
import com.example.workoutlogger.domain.usecase.session.ObserveSessionsInRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class HeatmapViewModel @Inject constructor(
    observeHeatmapUseCase: ObserveHeatmapUseCase,
    observeSessionsInRangeUseCase: ObserveSessionsInRangeUseCase
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()
    private val today = Clock.System.now().toLocalDateTime(timeZone).date
    private val startDate = today.minus(DatePeriod(months = 12))
    private val normalizedStart = startDate.minusDays((startDate.dayOfWeek.ordinal + 1) % 7)
    private val endDate = today

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    val uiState: StateFlow<HeatmapUiState>

    init {
        val heatmapFlow = observeHeatmapUseCase(timeZone)
        val sessionsFlow = observeSessionsInRangeUseCase(normalizedStart, endDate)

        uiState = combine(heatmapFlow, sessionsFlow, _selectedDate) { entries, sessions, selected ->
            val sessionsByDate = sessions.groupBy { session ->
                (session.endedAt ?: session.startedAt).toLocalDateTime(timeZone).date
            }
            val grid = buildGrid(entries, sessionsByDate)
            val selectedSessions = selected?.let { sessionsByDate[it].orEmpty() } ?: emptyList()
            HeatmapUiState(
                weeks = grid,
                selectedDate = selected,
                selectedSessions = selectedSessions
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeatmapUiState())
    }

    fun onSelectDate(date: LocalDate) {
        _selectedDate.value = if (_selectedDate.value == date) null else date
    }

    private fun buildGrid(
        entries: List<HeatmapEntry>,
        sessionsByDate: Map<LocalDate, List<WorkoutSession>>
    ): List<HeatmapWeekUi> {
        val entryMap = entries.associateBy { it.date }
        val totalDays = normalizedStart.daysUntil(endDate) + 1
        val days = (0 until totalDays).map { normalizedStart.plus(DatePeriod(days = it)) }
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

    private fun LocalDate.minusDays(days: Int): LocalDate = this.minus(DatePeriod(days = days))

    private fun LocalDate.daysUntil(other: LocalDate): Int {
        var current = this
        var count = 0
        while (current < other) {
            current = current.plus(DatePeriod(days = 1))
            count++
        }
        return count
    }
}

data class HeatmapUiState(
    val weeks: List<HeatmapWeekUi> = emptyList(),
    val selectedDate: LocalDate? = null,
    val selectedSessions: List<WorkoutSession> = emptyList()
)

data class HeatmapWeekUi(val days: List<HeatmapDayUi>)

data class HeatmapDayUi(
    val date: LocalDate,
    val hasWorkout: Boolean,
    val sessions: List<WorkoutSession>
)
