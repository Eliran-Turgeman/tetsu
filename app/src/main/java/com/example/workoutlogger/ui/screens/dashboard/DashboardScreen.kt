package com.example.workoutlogger.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.model.WorkoutItemType
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.ui.components.PrimaryButton
import com.example.workoutlogger.ui.components.SectionHeader
import com.example.workoutlogger.ui.components.TemplateCard
import com.example.workoutlogger.ui.components.TemplateUi
import com.example.workoutlogger.ui.components.TonalIconButton

@Composable
fun DashboardRoute(
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onOpenSession: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onViewAllTemplates: () -> Unit,
    onScheduleWorkout: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    DashboardScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onCreateWorkout = onCreateWorkout,
        onEditWorkout = onEditWorkout,
        onScheduleWorkout = onScheduleWorkout,
        onStartWorkout = { workoutId ->
            viewModel.startSession(workoutId) { sessionId ->
                sessionId?.let(onOpenSession)
            }
        },
        onResumeSession = onOpenSession,
        onNavigateToSettings = onNavigateToSettings,
        onViewAllTemplates = onViewAllTemplates
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    state: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onScheduleWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onResumeSession: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onViewAllTemplates: () -> Unit
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
                        text = stringResource(id = R.string.nav_dashboard),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    TonalIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = stringResource(id = R.string.nav_settings),
                        onClick = onNavigateToSettings
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp)
        ) {
            item("active_session") {
                ActiveSessionSection(
                    session = state.activeSession,
                    onResume = onResumeSession,
                    onStartWorkoutCta = onViewAllTemplates
                )
            }

            item("templates_preview") {
                TemplatesPreviewSection(
                    workouts = state.workouts,
                    onViewAll = onViewAllTemplates,
                    onCreateWorkout = onCreateWorkout,
                    onStartWorkout = onStartWorkout,
                    onEditWorkout = onEditWorkout,
                    onScheduleWorkout = onScheduleWorkout
                )
            }
        }
    }
}

@Composable
private fun ActiveSessionSection(
    session: WorkoutSession?,
    onResume: (Long) -> Unit,
    onStartWorkoutCta: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(title = stringResource(id = R.string.label_active_session))
        if (session != null && session.id != null) {
            ActiveSessionCard(session = session, onResume = onResume)
        } else {
            EmptyActiveSessionCard(onStartCta = onStartWorkoutCta)
        }
    }
}

@Composable
private fun ActiveSessionCard(session: WorkoutSession, onResume: (Long) -> Unit) {
    val sessionId = session.id ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = session.workoutNameSnapshot,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(id = R.string.label_status_active),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryButton(
                text = stringResource(id = R.string.action_resume_session),
                icon = Icons.Rounded.PlayArrow,
                onClick = { onResume(sessionId) }
            )
        }
    }
}

@Composable
private fun EmptyActiveSessionCard(onStartCta: () -> Unit) {
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
                text = stringResource(id = R.string.label_no_active_session),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryButton(
                text = stringResource(id = R.string.label_start_workout_cta),
                onClick = onStartCta
            )
        }
    }
}

@Composable
private fun TemplatesPreviewSection(
    workouts: List<Workout>,
    onViewAll: () -> Unit,
    onCreateWorkout: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onEditWorkout: (Long) -> Unit,
    onScheduleWorkout: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(
            title = stringResource(id = R.string.nav_templates),
            actionText = stringResource(id = R.string.action_view_all_templates),
            onActionClick = onViewAll
        )

        if (workouts.isEmpty()) {
            EmptyTemplatesCard(onCreateWorkout = onCreateWorkout)
        } else {
            workouts.take(3).forEach { workout ->
                val id = workout.id ?: return@forEach
                TemplateCard(
                    template = workout.asTemplateUi(),
                    onStart = { onStartWorkout(id) },
                    onEdit = { onEditWorkout(id) },
                    onSchedule = { onScheduleWorkout(id) }
                )
            }
        }
    }
}

@Composable
private fun EmptyTemplatesCard(onCreateWorkout: () -> Unit) {
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
