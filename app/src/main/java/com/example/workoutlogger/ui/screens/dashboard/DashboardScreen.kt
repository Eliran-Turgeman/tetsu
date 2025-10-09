package com.example.workoutlogger.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workoutlogger.ui.components.ScreenContainer
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.model.WorkoutStatus

@Composable
fun DashboardRoute(
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onOpenSession: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
        onStartWorkout = { workoutId ->
            viewModel.startSession(workoutId) { sessionId ->
                sessionId?.let(onOpenSession)
            }
        },
        onResumeSession = onOpenSession
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    state: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onResumeSession: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.nav_dashboard)) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        ScreenContainer(paddingValues = padding) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text(
                        text = stringResource(id = R.string.label_active_session),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.activeSession != null) {
                    Card(
                        onClick = { state.activeSession.id?.let(onResumeSession) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = state.activeSession.workoutNameSnapshot,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            supportingContent = {
                                val statusLabel = when (state.activeSession.status) {
                                    WorkoutStatus.ACTIVE -> stringResource(id = R.string.label_status_active)
                                    WorkoutStatus.COMPLETED -> stringResource(id = R.string.label_status_completed)
                                    WorkoutStatus.CANCELLED -> stringResource(id = R.string.label_status_cancelled)
                                }
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        )
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.label_no_previous_data),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Text(
                    text = stringResource(id = R.string.nav_templates),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.workouts.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(text = stringResource(id = R.string.label_empty_templates))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onCreateWorkout) {
                                Text(text = stringResource(id = R.string.action_add_template))
                            }
                        }
                    }
                }
            }

            items(state.workouts, key = { it.id ?: it.hashCode().toLong() }) { workout ->
                WorkoutCard(
                    workout = workout,
                    onStart = { workout.id?.let(onStartWorkout) },
                    onEdit = { workout.id?.let(onEditWorkout) }
                )
            }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutCard(
    workout: Workout,
    onStart: () -> Unit,
    onEdit: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onStart) {
                    Text(text = stringResource(id = R.string.action_start_workout))
                }
                FilledTonalButton(onClick = onEdit) {
                    Text(text = stringResource(id = R.string.nav_template_editor))
                }
            }
        }
    }
}
