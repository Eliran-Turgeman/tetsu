package com.example.workoutlogger.ui.screens.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.Workout

@Composable
fun WorkoutListRoute(
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onScheduleWorkout: (Long) -> Unit,
    onOpenSession: (Long) -> Unit,
    pendingStartWorkoutId: Long? = null,
    onConsumedPendingStart: () -> Unit = {},
    viewModel: WorkoutListViewModel = hiltViewModel()
) {
    val workouts by viewModel.workouts.collectAsState()
    val event by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(pendingStartWorkoutId) {
        if (pendingStartWorkoutId != null) {
            viewModel.startSession(pendingStartWorkoutId)
            onConsumedPendingStart()
        }
    }

    LaunchedEffect(event) {
        when (val current = event) {
            WorkoutListEvent.WorkoutDeleted -> snackbarHostState.showSnackbar(
                message = context.getString(R.string.snackbar_template_deleted)
            )
            is WorkoutListEvent.SessionStarted -> onOpenSession(current.sessionId)
            null -> Unit
        }
        if (event != null) {
            viewModel.clearEvent()
        }
    }

    WorkoutListScreen(
        workouts = workouts,
        snackbarHostState = snackbarHostState,
        onCreateWorkout = onCreateWorkout,
        onEditWorkout = onEditWorkout,
        onScheduleWorkout = onScheduleWorkout,
        onStartWorkout = { viewModel.startSession(it) },
        onDeleteWorkout = { viewModel.deleteWorkout(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutListScreen(
    workouts: List<Workout>,
    snackbarHostState: SnackbarHostState,
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onScheduleWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit
) {
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text(text = stringResource(id = R.string.dialog_delete_template_title)) },
            text = { Text(text = stringResource(id = R.string.dialog_delete_template_message)) },
            confirmButton = {
                Button(onClick = {
                    confirmDeleteId?.let(onDeleteWorkout)
                    confirmDeleteId = null
                }) {
                    Text(text = stringResource(id = R.string.action_delete))
                }
            },
            dismissButton = {
                Button(onClick = { confirmDeleteId = null }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.nav_templates)) },
                actions = {
                    Button(onClick = onCreateWorkout) {
                        Text(text = stringResource(id = R.string.action_add_template))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (workouts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(id = R.string.label_empty_templates),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onCreateWorkout) {
                                Text(text = stringResource(id = R.string.action_add_template))
                            }
                        }
                    }
                }
            }

            items(workouts, key = { it.id ?: it.hashCode().toLong() }) { workout ->
                WorkoutListItem(
                    workout = workout,
                    onStart = { workout.id?.let(onStartWorkout) },
                    onEdit = { workout.id?.let(onEditWorkout) },
                    onSchedule = { workout.id?.let(onScheduleWorkout) },
                    onDelete = { workout.id?.let { confirmDeleteId = it } }
                )
            }
        }
    }
}

@Composable
private fun WorkoutListItem(
    workout: Workout,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onSchedule: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.action_start_workout))
                }
                Button(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.nav_template_editor))
                }
                IconButton(onClick = onSchedule) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(id = R.string.label_remove))
                }
            }
        }
    }
}
