package com.example.workoutlogger.ui.screens.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.ui.components.ScreenContainer
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WorkoutStatus
import java.util.Locale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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
        ScreenContainer(paddingValues = padding) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(id = R.string.label_heatmap_month_header),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(id = R.string.label_heatmap_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HeatmapGrid(
                            weeks = state.weeks,
                            selectedDate = state.selectedDate,
                            onSelect = onSelect
                        )
                    }
                }

                item {
                    HeatmapSelectionSummary(state = state)
                }

                if (state.selectedSessions.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.label_heatmap_sessions_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(state.selectedSessions, key = { it.id ?: it.hashCode().toLong() }) { session ->
                        SessionRow(session = session, onOpenSession = onOpenSession)
                    }
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
    if (weeks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(vertical = 20.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.label_heatmap_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val cellSize = 14.dp
    val labelWidth = 44.dp
    val dayLabels = weeks.first().days.map { it.date.dayOfWeek }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(labelWidth))
            dayLabels.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.toShortLabel(),
                    modifier = Modifier.width(cellSize),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = week.days.firstOrNull()?.date?.toWeekLabel().orEmpty(),
                    modifier = Modifier.width(labelWidth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                week.days.forEach { day ->
                    HeatmapCell(
                        day = day,
                        selectedDate = selectedDate,
                        onSelect = onSelect,
                        size = cellSize
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapSelectionSummary(state: HeatmapUiState) {
    val hasSelection = state.selectedDate != null
    val containerColor = if (hasSelection) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (hasSelection) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = state.selectedDate?.asFriendlyString()
                ?: stringResource(id = R.string.label_heatmap_no_selection),
            style = MaterialTheme.typography.titleSmall,
            color = contentColor
        )
        if (hasSelection) {
            val count = state.selectedSessions.size
            val message = if (count == 1) {
                stringResource(id = R.string.label_heatmap_single_session)
            } else {
                stringResource(id = R.string.label_heatmap_sessions_count, count)
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}

@Composable
private fun HeatmapCell(
    day: HeatmapDayUi,
    selectedDate: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    size: Dp
) {
    val sessionCount = day.sessions.size
    val baseColor = when {
        sessionCount >= 3 -> MaterialTheme.colorScheme.primary
        sessionCount == 2 -> MaterialTheme.colorScheme.primaryContainer
        sessionCount == 1 -> MaterialTheme.colorScheme.secondaryContainer
        day.hasWorkout -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val isSelected = selectedDate == day.date
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val labelColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .size(size)
            .background(shape = MaterialTheme.shapes.extraSmall, color = baseColor)
            .border(borderWidth, borderColor, MaterialTheme.shapes.extraSmall)
            .clickable(enabled = day.sessions.isNotEmpty()) {
                onSelect(day.date)
            },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
        }
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, onOpenSession: (Long) -> Unit) {
    val timeZone = TimeZone.currentSystemDefault()
    val end = (session.endedAt ?: session.startedAt).toLocalDateTime(timeZone)
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { session.id?.let(onOpenSession) }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = session.workoutNameSnapshot,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    id = R.string.label_session_status_completed,
                    formatSessionTimestamp(end)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SessionStatusBadge(status = session.status)
        }
    }
}

@Composable
private fun SessionStatusBadge(status: WorkoutStatus) {
    val (labelRes, containerColor, contentColor) = when (status) {
        WorkoutStatus.ACTIVE -> Triple(
            R.string.label_status_active,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        WorkoutStatus.COMPLETED -> Triple(
            R.string.label_status_completed,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        WorkoutStatus.CANCELLED -> Triple(
            R.string.label_status_cancelled,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    androidx.compose.material3.Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp
    ) {
        Text(
            text = stringResource(id = labelRes),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private fun DayOfWeek.toShortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}

private fun LocalDate.toWeekLabel(): String {
    val monthPart = month.name.take(3).lowercase(Locale.getDefault()).replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
    }
    return "$monthPart ${dayOfMonth}"
}

private fun LocalDate.asFriendlyString(): String {
    val monthPart = month.name.lowercase(Locale.getDefault()).replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
    }
    return "$monthPart ${dayOfMonth}, $year"
}

private fun formatSessionTimestamp(value: LocalDateTime): String {
    val time = "%02d:%02d".format(value.hour, value.minute)
    return "${value.date.asFriendlyString()} • $time"
}
