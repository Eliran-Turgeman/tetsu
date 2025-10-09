@file:OptIn(ExperimentalLayoutApi::class)

package com.example.workoutlogger.ui.screens.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WorkoutStatus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.example.workoutlogger.ui.components.ScreenContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionRoute(
    sessionId: Long,
    onExit: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(event) {
        when (event) {
            SessionEvent.SessionFinished -> {
                snackbarHostState.showSnackbar(context.getString(R.string.snackbar_session_finished))
                onExit()
            }
            SessionEvent.SessionCancelled -> {
                snackbarHostState.showSnackbar(context.getString(R.string.snackbar_session_cancelled))
                onExit()
            }
            null -> Unit
        }
        if (event != null) viewModel.clearEvent()
    }

    if (showFinishDialog) {
        ConfirmDialog(
            title = stringResource(id = R.string.action_finish_session),
            message = stringResource(id = R.string.dialog_finish_session_message),
            confirmText = stringResource(id = R.string.action_finish_session),
            onConfirm = {
                viewModel.finishSession()
                showFinishDialog = false
            },
            onDismiss = { showFinishDialog = false }
        )
    }

    if (showCancelDialog) {
        ConfirmDialog(
            title = stringResource(id = R.string.action_cancel_session),
            message = stringResource(id = R.string.dialog_cancel_session_message),
            confirmText = stringResource(id = R.string.action_cancel_session),
            onConfirm = {
                viewModel.cancelSession()
                showCancelDialog = false
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            defaultUnit = state.defaultUnit,
            onAdd = { name, superset, sets, repsMin, repsMax, unit ->
                viewModel.addExercise(name, superset, sets, repsMin, repsMax, unit)
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false }
        )
    }

    SessionScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onUpdateSet = viewModel::updateSetLog,
        onDeleteSet = viewModel::deleteSet,
        onAddSet = { exercise -> viewModel.addSet(exercise, state.defaultUnit) },
        onRemoveExercise = viewModel::removeExercise,
        onMoveExercise = { fromIndex, toIndex ->
            val ids = state.exercises.toMutableList()
            val moved = ids.removeAt(fromIndex)
            ids.add(toIndex, moved)
            viewModel.updateExerciseOrder(ids.map { it.id })
        },
        onFinish = { showFinishDialog = true },
        onCancel = { showCancelDialog = true },
        onAddExercise = { showAddExerciseDialog = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SessionScreen(
    state: SessionUiState,
    snackbarHostState: SnackbarHostState,
    onUpdateSet: (Long, Long?, Int, String, String, WeightUnit, String?) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onAddSet: (SessionExerciseUi) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onMoveExercise: (Int, Int) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onAddExercise: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.session?.workoutNameSnapshot
                            ?: stringResource(id = R.string.nav_session)
                    )
                },
                actions = {
                    TextButton(
                        onClick = onFinish,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = stringResource(id = R.string.action_finish_session))
                    }
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(text = stringResource(id = R.string.action_cancel_session))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExercise) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.label_add_exercise)
                )
            }
        }
    ) { padding ->
        ScreenContainer(paddingValues = padding) {
            val session = state.session
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    SessionHeader(session)
                }

                itemsIndexed(
                    state.exercises,
                    key = { _, exercise -> exercise.id }) { index, exercise ->
                    SessionExerciseCard(
                        exercise = exercise,
                        onUpdateSet = onUpdateSet,
                        onDeleteSet = onDeleteSet,
                        onAddSet = { onAddSet(exercise) },
                        onRemoveExercise = { onRemoveExercise(exercise.id) },
                        onMoveUp = { if (index > 0) onMoveExercise(index, index - 1) },
                        onMoveDown = {
                            if (index < state.exercises.lastIndex) onMoveExercise(
                                index,
                                index + 1
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
    private fun SessionHeader(session: WorkoutSession?) {
        val timeZone = TimeZone.currentSystemDefault()
        val started = session?.startedAt?.toLocalDateTime(timeZone)
        val ended = session?.endedAt?.toLocalDateTime(timeZone)
        Column(modifier = Modifier.fillMaxWidth()) {
            if (started != null) {
                Text(
                    text = stringResource(
                        id = R.string.label_session_started_at,
                        started.toString()
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (session?.status == WorkoutStatus.COMPLETED && ended != null) {
                Text(
                    text = stringResource(
                        id = R.string.label_session_status_completed,
                        ended.toString()
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    @Composable
    private fun SessionExerciseCard(
        exercise: SessionExerciseUi,
        onUpdateSet: (Long, Long?, Int, String, String, WeightUnit, String?) -> Unit,
        onDeleteSet: (Long) -> Unit,
        onAddSet: () -> Unit,
        onRemoveExercise: () -> Unit,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = exercise.name, style = MaterialTheme.typography.titleMedium)
                        exercise.supersetId?.takeIf { it.isNotBlank() }?.let { superset ->
                            Text(
                                text = stringResource(id = R.string.label_superset_tag, superset),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onMoveUp) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null)
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null)
                    }
                    IconButton(onClick = onRemoveExercise) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.label_remove)
                        )
                    }
                }

                exercise.previousPerformance?.let { previous ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.label_previous_performance),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = previous.summary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            previous.best?.let { best ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(id = R.string.label_previous_best) + ": " + best,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                exercise.sets.forEach { set ->
                    SessionSetRow(
                        exerciseId = exercise.id,
                        set = set,
                        onUpdate = onUpdateSet,
                        onDelete = onDeleteSet
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onAddSet,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(id = R.string.label_add_set))
                }
            }
        }
    }

    @Composable
    private fun SessionSetRow(
        exerciseId: Long,
        set: SessionSetUi,
        onUpdate: (Long, Long?, Int, String, String, WeightUnit, String?) -> Unit,
        onDelete: (Long) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.label_sets) + " ${set.index + 1}",
                fontWeight = FontWeight.SemiBold
            )
            set.targetRange?.let { range ->
                Text(text = range, style = MaterialTheme.typography.bodySmall)
            }

            val rememberKey = set.id ?: -set.index.toLong()
            var reps by remember(rememberKey) { mutableStateOf(set.reps) }
            var weight by remember(rememberKey) { mutableStateOf(set.weight) }
            var note by remember(rememberKey) { mutableStateOf(set.note) }
            var unit by remember(rememberKey) { mutableStateOf(set.unit) }
            var dropdownExpanded by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = reps,
                    onValueChange = {
                        reps = it.filter { ch -> ch.isDigit() }
                        onUpdate(exerciseId, set.id, set.index, reps, weight, unit, note)
                    },
                    label = { Text(stringResource(id = R.string.label_reps)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        weight = it.filter { ch -> ch.isDigit() || ch == '.' }
                        onUpdate(exerciseId, set.id, set.index, reps, weight, unit, note)
                    },
                    label = { Text(stringResource(id = R.string.label_weight)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(
                        onClick = { dropdownExpanded = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = unit.name)
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }) {
                        WeightUnit.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option.name) },
                                onClick = {
                                    unit = option
                                    dropdownExpanded = false
                                    onUpdate(
                                        exerciseId,
                                        set.id,
                                        set.index,
                                        reps,
                                        weight,
                                        unit,
                                        note
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        onUpdate(exerciseId, set.id, set.index, reps, weight, unit, note)
                    },
                    label = { Text(text = stringResource(id = R.string.label_note)) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (set.id != null) {
                    IconButton(onClick = { onDelete(set.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.label_remove)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ConfirmDialog(
        title: String,
        message: String,
        confirmText: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = title) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = confirmText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    @Composable
    private fun AddExerciseDialog(
        defaultUnit: WeightUnit,
        onAdd: (String, String?, Int, Int?, Int?, WeightUnit) -> Unit,
        onDismiss: () -> Unit
    ) {
        var name by remember { mutableStateOf("") }
        var superset by remember { mutableStateOf("") }
        var sets by remember { mutableStateOf("3") }
        var repsMin by remember { mutableStateOf("") }
        var repsMax by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf(defaultUnit) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(id = R.string.dialog_add_exercise_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(id = R.string.label_exercise_name)) }
                    )
                    OutlinedTextField(
                        value = superset,
                        onValueChange = { superset = it },
                        label = { Text(stringResource(id = R.string.label_superset_id)) }
                    )
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(id = R.string.label_sets_count)) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = repsMin,
                            onValueChange = { repsMin = it.filter { ch -> ch.isDigit() } },
                            label = { Text(stringResource(id = R.string.label_min_reps)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = repsMax,
                            onValueChange = { repsMax = it.filter { ch -> ch.isDigit() } },
                            label = { Text(stringResource(id = R.string.label_max_reps)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Box {
                        TextButton(
                            onClick = { dropdownExpanded = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(text = unit.name)
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }) {
                            WeightUnit.values().forEach { option ->
                                DropdownMenuItem(text = { Text(option.name) }, onClick = {
                                    unit = option
                                    dropdownExpanded = false
                                })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val setsInt = sets.toIntOrNull() ?: 0
                        val minInt = repsMin.toIntOrNull()
                        val maxInt = repsMax.toIntOrNull()
                        onAdd(name, superset.ifBlank { null }, setsInt, minInt, maxInt, unit)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = stringResource(id = R.string.action_add))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
