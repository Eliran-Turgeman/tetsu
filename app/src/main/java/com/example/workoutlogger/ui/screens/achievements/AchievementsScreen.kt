package com.example.workoutlogger.ui.screens.achievements

import android.app.DatePickerDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.WorkoutStatus
import com.example.workoutlogger.domain.model.achievements.UserGoalKind
import com.example.workoutlogger.ui.components.PrimaryButton
import com.example.workoutlogger.ui.components.SectionHeader
import com.example.workoutlogger.ui.components.SecondaryButton
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsRoute(
    onOpenSession: (Long) -> Unit,
    onStartWorkout: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val showSheet = state.isGoalSheetOpen

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AchievementsTopBar()
        },
        floatingActionButton = {
            if (!showSheet) {
                FloatingActionButton(onClick = viewModel::onFabClick) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = stringResource(id = R.string.achievements_fab_content_description))
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.emptyState -> {
                EmptyState(modifier = Modifier.fillMaxSize().padding(padding), onStartWorkout = onStartWorkout)
            }
            else -> {
                AchievementsContent(
                    modifier = Modifier.padding(padding),
                    state = state,
                    onDayClick = viewModel::onDayClick,
                    onOpenSession = onOpenSession,
                    onDeleteGoal = viewModel::onConfirmDelete
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = viewModel::onDismissGoalSheet
        ) {
            NewGoalSheet(
                state = state,
                onSelectKind = viewModel::onSelectGoalKind,
                onUpdateTitle = viewModel::onUpdateGoalTitle,
                onUpdateExercise = viewModel::onUpdateGoalExercise,
                onUpdatePrimary = viewModel::onUpdatePrimaryValue,
                onUpdateSecondary = viewModel::onUpdateSecondaryValue,
                onUpdateWindow = viewModel::onUpdateWindowValue,
                onUpdateDeadline = viewModel::onUpdateDeadline,
                onSubmit = { viewModel.submitGoal(state.weightUnit, state.bodyWeightKg, state.templateExercises) },
                onCancel = viewModel::onDismissGoalSheet
            )
        }
    }

    state.pendingDeletion?.let { pending ->
        ConfirmDeleteDialog(
            title = pending.title,
            onDismiss = viewModel::onDismissDelete,
            onConfirm = viewModel::onDeleteConfirmed
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementsTopBar() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
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
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AchievementsContent(
    modifier: Modifier,
    state: AchievementsUiState,
    onDayClick: (HeatmapDayUi) -> Unit,
    onOpenSession: (Long) -> Unit,
    onDeleteGoal: (AchievementCardUi) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item("heatmap") {
            HeatmapSection(
                weeks = state.weeks,
                selectedDate = state.selectedDate,
                onDayClick = onDayClick
            )
        }

        state.selectedDate?.let { date ->
            item("selected_day") {
                SelectedDaySummary(date = date, sessions = state.selectedSessions, onOpenSession = onOpenSession)
            }
        }

        if (state.inProgress.isNotEmpty()) {
            item("in_progress_header") {
                SectionHeader(title = stringResource(id = R.string.achievements_in_progress_title), actionText = null)
            }
            items(state.inProgress, key = { it.instanceId }) { card ->
                AchievementCard(card, onDeleteGoal)
            }
        }

        if (state.completed.isNotEmpty()) {
            item("completed_header") {
                SectionHeader(title = stringResource(id = R.string.achievements_completed_title), actionText = null)
            }
            item("completed_grid") {
                CompletedBadgesGrid(badges = state.completed)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HeatmapSection(
    weeks: List<HeatmapWeekUi>,
    selectedDate: LocalDate?,
    onDayClick: (HeatmapDayUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(
            title = stringResource(id = R.string.label_heatmap_month_header),
            actionText = null
        )
        HeatmapCalendar(
            weeks = weeks,
            selectedDate = selectedDate,
            onDayClick = onDayClick
        )
    }
}

@Composable
private fun SelectedDaySummary(
    date: LocalDate,
    sessions: List<WorkoutSession>,
    onOpenSession: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(id = R.string.achievements_day_summary_title, date.toString()),
            style = MaterialTheme.typography.titleMedium
        )
        if (sessions.isEmpty()) {
            Text(
                text = stringResource(id = R.string.achievements_day_summary_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sessions.forEach { session ->
                    SessionSummaryCard(session = session, onOpenSession = onOpenSession)
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(
    session: WorkoutSession,
    onOpenSession: (Long) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = { session.id?.let(onOpenSession) })
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = session.workoutNameSnapshot,
                style = MaterialTheme.typography.titleMedium
            )
    val statusLabel = when (session.status) {
        WorkoutStatus.ACTIVE -> R.string.achievements_status_active
        WorkoutStatus.COMPLETED -> R.string.achievements_status_completed
        WorkoutStatus.CANCELLED -> R.string.achievements_status_cancelled
    }
            Text(
                text = stringResource(id = statusLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AchievementCard(card: AchievementCardUi, onDeleteGoal: (AchievementCardUi) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(card.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (card.isUserGoal) {
                    IconButton(onClick = { onDeleteGoal(card) }) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = stringResource(id = R.string.achievements_delete_goal_content_description))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(progress = { card.percent })
                Text(text = card.progressLabel, style = MaterialTheme.typography.bodyMedium)
                card.deadline?.let { deadline ->
                    Text(
                        text = stringResource(id = R.string.achievements_deadline_label, deadline.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CompletedBadgesGrid(badges: List<AchievementBadgeUi>) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        badges.forEach { badge ->
            BadgeTile(badge)
        }
    }
}

@Composable
private fun BadgeTile(badge: AchievementBadgeUi) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = badge.title,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(id = R.string.achievements_completed_on_label, badge.completedOn.toString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onStartWorkout: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.achievements_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.achievements_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = stringResource(id = R.string.achievements_empty_cta), onClick = onStartWorkout)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewGoalSheet(
    state: AchievementsUiState,
    onSelectKind: (UserGoalKind) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateExercise: (String) -> Unit,
    onUpdatePrimary: (String) -> Unit,
    onUpdateSecondary: (String) -> Unit,
    onUpdateWindow: (String) -> Unit,
    onUpdateDeadline: (LocalDate?) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(text = stringResource(id = R.string.achievements_new_goal_title), style = MaterialTheme.typography.titleLarge)

        GoalTypeSelector(selected = state.goalDraft.kind, onSelect = onSelectKind)

        GoalDetailForm(
            state = state,
            onUpdateTitle = onUpdateTitle,
            onUpdateExercise = onUpdateExercise,
            onUpdatePrimary = onUpdatePrimary,
            onUpdateSecondary = onUpdateSecondary,
            onUpdateWindow = onUpdateWindow,
            onUpdateDeadline = onUpdateDeadline
        )

        state.goalError?.let { error ->
            Text(
                text = stringResource(id = error.toMessageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        val requiresExercise = when (state.goalDraft.kind) {
            UserGoalKind.LIFT_WEIGHT, UserGoalKind.REPS_AT_WEIGHT, UserGoalKind.BODY_WEIGHT_RELATION -> true
            else -> false
        }
        val hasExercises = state.templateExercises.isNotEmpty()
        val submitEnabled = !state.isSubmittingGoal && (!requiresExercise || hasExercises)

        PrimaryButton(
            text = stringResource(id = R.string.achievements_create_goal_cta),
            onClick = onSubmit,
            enabled = submitEnabled
        )
        SecondaryButton(
            text = stringResource(id = R.string.action_cancel),
            onClick = onCancel
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalTypeSelector(selected: UserGoalKind?, onSelect: (UserGoalKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(id = R.string.achievements_goal_type_label), style = MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GoalTypeOption.all.forEach { option ->
                val isSelected = selected == option.kind
                AssistChip(
                    onClick = { onSelect(option.kind) },
                    label = { Text(text = stringResource(id = option.labelRes)) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(imageVector = Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else null,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun GoalDetailForm(
    state: AchievementsUiState,
    onUpdateTitle: (String) -> Unit,
    onUpdateExercise: (String) -> Unit,
    onUpdatePrimary: (String) -> Unit,
    onUpdateSecondary: (String) -> Unit,
    onUpdateWindow: (String) -> Unit,
    onUpdateDeadline: (LocalDate?) -> Unit
) {
    val draft = state.goalDraft
    val exercises = state.templateExercises
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = draft.title,
            onValueChange = onUpdateTitle,
            label = { Text(stringResource(id = R.string.achievements_goal_title_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        when (draft.kind) {
            UserGoalKind.LIFT_WEIGHT -> {
                ExerciseDropdownField(
                    value = draft.exerciseName,
                    onValueChange = onUpdateExercise,
                    options = exercises
                )
                WeightField(value = draft.primaryValue, onValueChange = onUpdatePrimary, weightUnit = state.weightUnit)
                DeadlinePickerRow(deadline = draft.deadline, onUpdateDeadline = onUpdateDeadline)
            }
            UserGoalKind.REPS_AT_WEIGHT -> {
                ExerciseDropdownField(
                    value = draft.exerciseName,
                    onValueChange = onUpdateExercise,
                    options = exercises
                )
                NumericField(value = draft.primaryValue, onValueChange = onUpdatePrimary, label = stringResource(id = R.string.achievements_goal_reps_label))
                WeightField(value = draft.secondaryValue, onValueChange = onUpdateSecondary, weightUnit = state.weightUnit)
                DeadlinePickerRow(deadline = draft.deadline, onUpdateDeadline = onUpdateDeadline)
            }
            UserGoalKind.FREQUENCY_IN_WINDOW -> {
                NumericField(value = draft.primaryValue, onValueChange = onUpdatePrimary, label = stringResource(id = R.string.achievements_goal_workouts_label))
                NumericField(value = draft.windowValue, onValueChange = onUpdateWindow, label = stringResource(id = R.string.achievements_goal_weeks_label))
                DeadlinePickerRow(deadline = draft.deadline, onUpdateDeadline = onUpdateDeadline)
            }
            UserGoalKind.BODY_WEIGHT_RELATION -> {
                val bodyWeightDisplay = state.bodyWeightKg?.let { kotlin.math.round(it).toInt() }
                ExerciseDropdownField(
                    value = draft.exerciseName,
                    onValueChange = onUpdateExercise,
                    options = exercises
                )
                Text(
                    text = bodyWeightDisplay?.let { stringResource(id = R.string.achievements_goal_bodyweight_hint, it) }
                        ?: stringResource(id = R.string.achievements_goal_bodyweight_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            UserGoalKind.STREAK -> {
                NumericField(value = draft.primaryValue, onValueChange = onUpdatePrimary, label = stringResource(id = R.string.achievements_goal_streak_label))
            }
            UserGoalKind.TIME_UNDER_TENSION -> {
                NumericField(value = draft.primaryValue, onValueChange = onUpdatePrimary, label = stringResource(id = R.string.achievements_goal_minutes_label))
                NumericField(value = draft.windowValue, onValueChange = onUpdateWindow, label = stringResource(id = R.string.achievements_goal_window_days_label))
                DeadlinePickerRow(deadline = draft.deadline, onUpdateDeadline = onUpdateDeadline)
            }
            null -> {
                Text(
                    text = stringResource(id = R.string.achievements_goal_select_type_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    val sortedOptions = remember(options) { options.sorted() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (sortedOptions.isNotEmpty()) {
                    expanded = !expanded
                }
            }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(id = R.string.achievements_goal_exercise_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                enabled = sortedOptions.isNotEmpty()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sortedOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
                if (value.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.action_clear)) },
                        onClick = {
                            onValueChange("")
                            expanded = false
                        }
                    )
                }
            }
        }
        if (sortedOptions.isEmpty()) {
            Text(
                text = stringResource(id = R.string.achievements_goal_no_exercises),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NumericField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.all { it.isDigit() }) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WeightField(value: String, onValueChange: (String) -> Unit, weightUnit: WeightUnit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isBlank() || input.toDoubleOrNull() != null) {
                onValueChange(input)
            }
        },
        label = { Text(stringResource(id = R.string.achievements_goal_weight_label, if (weightUnit == WeightUnit.LB) "lb" else "kg")) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DeadlinePickerRow(deadline: LocalDate?, onUpdateDeadline: (LocalDate?) -> Unit) {
    val context = LocalContext.current
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = deadline?.toString() ?: stringResource(id = R.string.achievements_goal_deadline_none),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = {
            val initial = deadline ?: now
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onUpdateDeadline(LocalDate(year = year, monthNumber = month + 1, dayOfMonth = day))
                },
                initial.year,
                initial.monthNumber - 1,
                initial.dayOfMonth
            ).show()
        }) {
            Text(text = stringResource(id = R.string.achievements_goal_pick_deadline))
        }
        if (deadline != null) {
            TextButton(onClick = { onUpdateDeadline(null) }) {
                Text(text = stringResource(id = R.string.action_clear))
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        },
        title = { Text(text = stringResource(id = R.string.achievements_delete_goal_title)) },
        text = { Text(text = stringResource(id = R.string.achievements_delete_goal_body, title)) }
    )
}

private data class GoalTypeOption(val kind: UserGoalKind, val labelRes: Int) {
    companion object {
        val all = listOf(
            GoalTypeOption(UserGoalKind.LIFT_WEIGHT, R.string.achievements_goal_type_lift_weight),
            GoalTypeOption(UserGoalKind.REPS_AT_WEIGHT, R.string.achievements_goal_type_reps),
            GoalTypeOption(UserGoalKind.FREQUENCY_IN_WINDOW, R.string.achievements_goal_type_frequency),
            GoalTypeOption(UserGoalKind.BODY_WEIGHT_RELATION, R.string.achievements_goal_type_bodyweight),
            GoalTypeOption(UserGoalKind.STREAK, R.string.achievements_goal_type_streak),
            GoalTypeOption(UserGoalKind.TIME_UNDER_TENSION, R.string.achievements_goal_type_time)
        )
    }
}

private fun GoalValidationError.toMessageRes(): Int = when (this) {
    GoalValidationError.KindRequired -> R.string.achievements_error_kind
    GoalValidationError.ExerciseRequired -> R.string.achievements_error_exercise
    GoalValidationError.TargetWeightRequired -> R.string.achievements_error_weight
    GoalValidationError.RepsRequired -> R.string.achievements_error_reps
    GoalValidationError.WorkoutsRequired -> R.string.achievements_error_workouts
    GoalValidationError.WindowRequired -> R.string.achievements_error_window
    GoalValidationError.BodyWeightMissing -> R.string.achievements_error_bodyweight_missing
    GoalValidationError.StreakRequired -> R.string.achievements_error_streak
    GoalValidationError.MinutesRequired -> R.string.achievements_error_minutes
    GoalValidationError.SubmissionFailed -> R.string.achievements_error_submission
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HeatmapCalendar(
    weeks: List<HeatmapWeekUi>,
    selectedDate: LocalDate?,
    onDayClick: (HeatmapDayUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun rememberMonthLabels(weeks: List<HeatmapWeekUi>): List<String?> {
    val locale = Locale.getDefault()
    return remember(weeks) {
        var lastMonth: Int? = null
        weeks.map { week ->
            val firstOfMonth = week.days.firstOrNull { it.date.dayOfMonth == 1 }
            val monthNumber = (firstOfMonth ?: week.days.first()).date.monthNumber
            if (monthNumber != lastMonth) {
                lastMonth = monthNumber
                week.days.first().date.toJavaLocalDate().month.getDisplayName(TextStyle.SHORT, locale)
            } else {
                null
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun DayOfWeek.shortName(): String {
    val javaDay = java.time.DayOfWeek.of(ordinal + 1)
    return javaDay.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1)
}

@Composable
private fun HeatmapCell(day: HeatmapDayUi, selected: Boolean, size: Dp, onClick: () -> Unit) {
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
            .clickable(role = Role.Button, onClick = onClick)
    )
}
