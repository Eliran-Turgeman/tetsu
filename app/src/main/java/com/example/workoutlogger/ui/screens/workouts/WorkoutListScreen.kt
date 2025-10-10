package com.example.workoutlogger.ui.screens.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.model.WorkoutItemType
import com.example.workoutlogger.ui.components.PrimaryButton
import com.example.workoutlogger.ui.components.SectionHeader
import com.example.workoutlogger.ui.components.TemplateCard
import com.example.workoutlogger.ui.components.TemplateUi
import kotlinx.coroutines.flow.collectLatest

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
    val haptics = LocalHapticFeedback.current
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
        onStartWorkout = { workoutId ->
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            viewModel.startSession(workoutId)
        },
        onDeleteWorkout = { workoutId ->
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            viewModel.deleteWorkout(workoutId)
        }
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.nav_templates),
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
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateWorkout) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = stringResource(id = R.string.action_add_template))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item("header") {
                SectionHeader(title = stringResource(id = R.string.label_template_library))
            }

            if (workouts.isEmpty()) {
                item("empty") {
                    EmptyTemplatesState(onCreateWorkout = onCreateWorkout)
                }
            } else {
                items(workouts, key = { it.id ?: it.hashCode().toLong() }) { workout ->
                    val workoutId = workout.id ?: return@items
                    TemplateCard(
                        template = workout.asTemplateUi(),
                        onStart = { onStartWorkout(workoutId) },
                        onEdit = { onEditWorkout(workoutId) },
                        onSchedule = { onScheduleWorkout(workoutId) },
                        onDelete = { onDeleteWorkout(workoutId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTemplatesState(onCreateWorkout: () -> Unit) {
    androidx.compose.material3.Surface(
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
                text = stringResource(id = R.string.label_empty_templates),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryButton(
                text = stringResource(id = R.string.action_add_template),
                onClick = onCreateWorkout
            )
        }
    }
}

@Composable
private fun Workout.asTemplateUi(): TemplateUi {
    val exerciseCount = items.count { it.type == WorkoutItemType.EXERCISE }
    val supersetCount = items.count { it.type == WorkoutItemType.SUPERSET_HEADER }
    val metaParts = buildList {
        add(pluralStringResource(id = R.plurals.label_meta_exercises, count = exerciseCount, exerciseCount))
        if (supersetCount > 0) {
            add(pluralStringResource(id = R.plurals.label_meta_supersets, count = supersetCount, supersetCount))
        }
    }
    return TemplateUi(
        id = id,
        name = name,
        meta = metaParts.joinToString(" • ")
    )
}
