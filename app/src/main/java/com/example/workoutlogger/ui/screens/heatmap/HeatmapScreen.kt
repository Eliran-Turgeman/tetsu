package com.example.workoutlogger.ui.screens.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.WorkoutStatus
import com.example.workoutlogger.ui.components.PrimaryButton
import com.example.workoutlogger.ui.components.SectionHeader
import com.example.workoutlogger.ui.components.SecondaryButton
import com.example.workoutlogger.ui.theme.WorkoutLoggerTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapRoute(
    onOpenSession: (Long) -> Unit,
    onStartWorkout: () -> Unit,
    viewModel: HeatmapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var sheetDay by remember { mutableStateOf<HeatmapDayUi?>(null) }

    HeatmapScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onDayClick = { day ->
            viewModel.onSelectDate(day.date)
            if (day.sessions.isNotEmpty()) {
                sheetDay = day
            }
        },
        onOpenSession = onOpenSession
    )

    val currentSheetDay = sheetDay
    if (currentSheetDay != null && currentSheetDay.sessions.isNotEmpty()) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    sheetDay = null
                    if (state.selectedDate == currentSheetDay.date) {
                        viewModel.onSelectDate(currentSheetDay.date)
                    }
                }
            }
        ) {
            HeatmapDayDetailSheet(
                day = currentSheetDay,
                sessions = state.selectedSessions,
                onStartWorkout = onStartWorkout,
                onOpenSession = onOpenSession,
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        sheetDay = null
                        if (state.selectedDate == currentSheetDay.date) {
                            viewModel.onSelectDate(currentSheetDay.date)
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeatmapScreen(
    state: HeatmapUiState,
    snackbarHostState: SnackbarHostState,
    onDayClick: (HeatmapDayUi) -> Unit,
    onOpenSession: (Long) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.nav_heatmap),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item("calendar") {
                HeatmapCalendar(
                    weeks = state.weeks,
                    selectedDate = state.selectedDate,
                    onDayClick = onDayClick
                )
            }

            if (state.selectedDate != null) {
                item("summary") {
                    SelectedDaySummary(state = state, onOpenSession = onOpenSession)
                }
            }
        }
    }
}

@Composable
private fun HeatmapCalendar(
    weeks: List<HeatmapWeekUi>,
    selectedDate: LocalDate?,
    onDayClick: (HeatmapDayUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(
            title = stringResource(id = R.string.label_heatmap_month_header),
            actionText = null
        )
        if (weeks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = stringResource(id = R.string.label_heatmap_empty),
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return
        }

        val listState = rememberLazyListState()
        val cellSize = 14.dp
        val cellSpacing = 2.dp
        val dayLabelWidth = 24.dp
        val monthLabels = rememberMonthLabels(weeks)

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dayLabelWidth)
                .height(cellSize + 16.dp),
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(cellSpacing),
            userScrollEnabled = false
        ) {
            items(weeks.size) { index ->
                val label = monthLabels[index]
                Box(
                    modifier = Modifier
                        .width(cellSize)
                        .height(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (label != null) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }

        val dayOrder = listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )

        Row(horizontalArrangement = Arrangement.spacedBy(cellSpacing)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(cellSpacing),
                modifier = Modifier.width(dayLabelWidth)
            ) {
                dayOrder.forEach { day ->
                    Text(
                        text = day.shortName(),
                        modifier = Modifier.height(cellSize),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(cellSpacing)
            ) {
                items(weeks) { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(cellSpacing)
                    ) {
                        week.days.forEach { day ->
                            HeatmapCell(
                                day = day,
                                selected = selectedDate == day.date,
                                size = cellSize,
                                onClick = { onDayClick(day) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    day: HeatmapDayUi,
    selected: Boolean,
    size: Dp,
    onClick: () -> Unit
) {
    val sessionCount = day.sessions.size
    val background = when (sessionCount) {
        0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
        else -> MaterialTheme.colorScheme.primary
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else Color.Transparent

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {}
}

@Composable
private fun SelectedDaySummary(
    state: HeatmapUiState,
    onOpenSession: (Long) -> Unit
) {
    val selectedDate = state.selectedDate ?: return
    val totalWorkouts = state.selectedSessions.size
    val totalSets = state.selectedSessions.sumOf { session -> session.exercises.sumOf { it.sets.size } }
    val workoutsSummary = if (totalWorkouts == 1) {
        stringResource(id = R.string.label_heatmap_single_session)
    } else {
        stringResource(id = R.string.label_heatmap_sessions_count, totalWorkouts)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = selectedDate.asFullLabel(),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = workoutsSummary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (totalWorkouts > 0) {
                Text(
                    text = stringResource(id = R.string.label_total_sets_logged, totalSets),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.selectedSessions.forEach { session ->
                        SessionSummaryRow(session = session, onOpenSession = onOpenSession)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryRow(session: WorkoutSession, onOpenSession: (Long) -> Unit) {
    val sessionId = session.id ?: return
    val totalSets = session.exercises.sumOf { it.sets.size }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp,
        onClick = { onOpenSession(sessionId) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = session.workoutNameSnapshot,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.label_total_sets_logged, totalSets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeatmapDayDetailSheet(
    day: HeatmapDayUi,
    sessions: List<WorkoutSession>,
    onStartWorkout: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val totalSets = sessions.sumOf { session -> session.exercises.sumOf { it.sets.size } }
    val workoutsSummary = if (sessions.size == 1) {
        stringResource(id = R.string.label_heatmap_single_session)
    } else {
        stringResource(id = R.string.label_heatmap_sessions_count, sessions.size)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = day.date.asFullLabel(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = workoutsSummary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(id = R.string.label_total_sets_logged, totalSets),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PrimaryButton(
            text = stringResource(id = R.string.label_start_workout_cta),
            onClick = {
                onStartWorkout()
                onDismiss()
            }
        )
        if (sessions.isNotEmpty()) {
            SecondaryButton(
                text = stringResource(id = R.string.action_view_logs),
                onClick = {
                    sessions.firstOrNull()?.id?.let(onOpenSession)
                    onDismiss()
                }
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sessions.forEach { session ->
                    SessionSummaryRow(session = session, onOpenSession = {
                        onOpenSession(it)
                        onDismiss()
                    })
                }
            }
        }
    }
}

@Composable
private fun rememberMonthLabels(weeks: List<HeatmapWeekUi>): List<String?> {
    return remember(weeks) {
        var lastMonth: Month? = null
        weeks.map { week ->
            val firstOfMonth = week.days.firstOrNull { it.date.dayOfMonth == 1 }
            val month = (firstOfMonth ?: week.days.first()).date.month
            if (month != lastMonth) {
                lastMonth = month
                month.displayName()
            } else {
                null
            }
        }
    }
}

private fun Month.displayName(): String {
    return toJavaMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

private fun Month.toJavaMonth(): java.time.Month = java.time.Month.of(ordinal + 1)

private fun DayOfWeek.shortName(): String {
    val javaDay = java.time.DayOfWeek.of(ordinal + 1)
    return javaDay.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1)
}

private fun LocalDate.asFullLabel(): String {
    val monthName = month.toJavaMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$monthName ${dayOfMonth}, $year"
}

@Preview(showBackground = true)
@Composable
private fun HeatmapScreenPreview() {
    val today = LocalDate(2024, Month.DECEMBER, 31)
    val start = today.minus(DatePeriod(days = 52 * 7))
    val weeks = (0 until 53).map { weekIndex ->
        HeatmapWeekUi(
            days = (0 until 7).map { dayIndex ->
                val date = start.plus(DatePeriod(days = weekIndex * 7 + dayIndex))
                val count = (date.dayOfMonth + weekIndex) % 5
                val sessions = if (count == 0) {
                    emptyList()
                } else {
                    List(count.coerceAtMost(2)) { sampleSession(date, id = weekIndex * 10L + dayIndex + it) }
                }
                HeatmapDayUi(
                    date = date,
                    hasWorkout = sessions.isNotEmpty(),
                    sessions = sessions
                )
            }
        )
    }
    WorkoutLoggerTheme {
        HeatmapScreen(
            state = HeatmapUiState(weeks = weeks, selectedDate = today, selectedSessions = emptyList()),
            snackbarHostState = remember { SnackbarHostState() },
            onDayClick = {},
            onOpenSession = {}
        )
    }
}

@Composable
private fun sampleSession(date: LocalDate, id: Long): WorkoutSession {
    val instant = date.atStartOfDayIn(TimeZone.UTC)
    val exercise = SessionExercise(
        id = id,
        sessionId = id,
        position = 0,
        supersetGroupId = null,
        exerciseName = "Bench Press",
        sets = listOf(
            SessionSetLog(
                id = id * 10,
                sessionExerciseId = id,
                setIndex = 0,
                loggedReps = 8,
                loggedWeight = 60.0,
                unit = WeightUnit.KG
            )
        )
    )
    return WorkoutSession(
        id = id,
        workoutId = 1L,
        workoutNameSnapshot = "Push Day",
        startedAt = instant,
        endedAt = instant,
        status = WorkoutStatus.COMPLETED,
        exercises = listOf(exercise)
    )
}
