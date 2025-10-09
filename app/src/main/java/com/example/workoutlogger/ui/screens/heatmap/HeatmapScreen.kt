package com.example.workoutlogger.ui.screens.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.WorkoutSession
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HeatmapRoute(
    onOpenSession: (Long) -> Unit,
    viewModel: HeatmapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    HeatmapScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSelect = viewModel::onSelectDate,
        onOpenSession = onOpenSession
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeatmapScreen(
    state: HeatmapUiState,
    snackbarHostState: SnackbarHostState,
    onSelect: (LocalDate) -> Unit,
    onOpenSession: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.nav_heatmap)) })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.label_heatmap_month_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                HeatmapGrid(
                    weeks = state.weeks,
                    selectedDate = state.selectedDate,
                    onSelect = onSelect
                )
            }

            item {
                Text(
                    text = state.selectedDate?.toString() ?: stringResource(id = R.string.label_heatmap_no_selection),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            if (state.selectedSessions.isNotEmpty()) {
                items(state.selectedSessions, key = { it.id ?: it.hashCode().toLong() }) { session ->
                    SessionRow(session = session, onOpenSession = onOpenSession)
                }
            }
        }
    }
}

@Composable
private fun HeatmapGrid(
    weeks: List<HeatmapWeekUi>,
    selectedDate: LocalDate?,
    onSelect: (LocalDate) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        LazyRow(contentPadding = PaddingValues(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(weeks) { week ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.days.forEach { day ->
                        val color = if (day.hasWorkout) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(color, shape = MaterialTheme.shapes.extraSmall)
                                .clickable(enabled = day.sessions.isNotEmpty()) {
                                    onSelect(day.date)
                                }
                                .border(
                                    width = if (selectedDate == day.date) 2.dp else 1.dp,
                                    color = if (selectedDate == day.date) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, onOpenSession: (Long) -> Unit) {
    val timeZone = TimeZone.currentSystemDefault()
    val end = (session.endedAt ?: session.startedAt).toLocalDateTime(timeZone)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(16.dp)
            .clickable { session.id?.let(onOpenSession) }
    ) {
        Text(text = session.workoutNameSnapshot, style = MaterialTheme.typography.titleMedium)
        Text(text = end.toString(), style = MaterialTheme.typography.bodySmall)
    }
}
