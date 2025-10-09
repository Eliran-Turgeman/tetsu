@file:OptIn(ExperimentalLayoutApi::class)

package com.example.workoutlogger.ui.screens.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.WorkoutItemType
import com.example.workoutlogger.ui.components.ScreenContainer
import kotlinx.coroutines.launch

@Composable
fun WorkoutEditorRoute(
    workoutId: Long?,
    onDone: () -> Unit,
    viewModel: WorkoutEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSupersetDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(state.error) {
        when (val error = state.error) {
            WorkoutEditorError.NameRequired -> snackbarHostState.showSnackbar(
                message = context.getString(R.string.error_template_name_required)
            )
            is WorkoutEditorError.ExerciseNameRequired -> snackbarHostState.showSnackbar(
                message = context.getString(R.string.error_exercise_name_required)
            )
            is WorkoutEditorError.InvalidSets -> snackbarHostState.showSnackbar(
                message = context.getString(R.string.error_sets_invalid)
            )
            is WorkoutEditorError.InvalidRepsRange -> snackbarHostState.showSnackbar(
                message = context.getString(R.string.error_reps_range_invalid)
            )
            null -> Unit
        }
        if (state.error != null) {
            viewModel.clearError()
        }
    }

    if (showSupersetDialog) {
        SupersetDialog(
            onAdd = { label ->
                viewModel.addSupersetHeader(label)
                showSupersetDialog = false
            },
            onDismiss = { showSupersetDialog = false }
        )
    }

    WorkoutEditorScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNameChanged = viewModel::updateWorkoutName,
        onAddExercise = viewModel::addExercise,
        onAddSuperset = { showSupersetDialog = true },
        onUpdateItem = viewModel::updateItem,
        onMoveUp = viewModel::moveItemUp,
        onMoveDown = viewModel::moveItemDown,
        onRemoveItem = viewModel::removeItem,
        onSave = {
            viewModel.saveWorkout { _ ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_template_saved))
                }
                onDone()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutEditorScreen(
    state: WorkoutEditorUiState,
    snackbarHostState: SnackbarHostState,
    onNameChanged: (String) -> Unit,
    onAddExercise: () -> Unit,
    onAddSuperset: () -> Unit,
    onUpdateItem: (Long, (WorkoutEditorItemUi) -> WorkoutEditorItemUi) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.nav_template_editor)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        ScreenContainer(paddingValues = padding) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = state.workoutName,
                        onValueChange = onNameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.label_template_name)) }
                    )
                }

                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onAddExercise,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = stringResource(id = R.string.label_add_exercise))
                        }
                        FilledTonalButton(
                            onClick = onAddSuperset,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(text = stringResource(id = R.string.label_add_superset))
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(id = R.string.label_template_items),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(state.items, key = { it.localId }) { item ->
                    WorkoutItemEditorCard(
                        item = item,
                        onUpdate = { transform -> onUpdateItem(item.localId, transform) },
                        onMoveUp = { onMoveUp(item.localId) },
                        onMoveDown = { onMoveDown(item.localId) },
                        onRemove = { onRemoveItem(item.localId) }
                    )
                }

                item {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = stringResource(id = R.string.action_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutItemEditorCard(
    item: WorkoutEditorItemUi,
    onUpdate: ((WorkoutEditorItemUi) -> WorkoutEditorItemUi) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onMoveUp) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null)
                }
                IconButton(onClick = onMoveDown) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null)
                }
                IconButton(onClick = onRemove) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(id = R.string.label_remove))
                }
            }

            if (item.type == WorkoutItemType.SUPERSET_HEADER) {
                OutlinedTextField(
                    value = item.supersetId,
                    onValueChange = { value -> onUpdate { it.copy(supersetId = value) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(id = R.string.label_superset_id)) },
                    supportingText = { Text(text = stringResource(id = R.string.label_superset_hint)) }
                )
            } else {
                OutlinedTextField(
                    value = item.exerciseName,
                    onValueChange = { value -> onUpdate { it.copy(exerciseName = value) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(id = R.string.label_exercise_name)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = item.sets,
                        onValueChange = { value -> onUpdate { it.copy(sets = value.filter { ch -> ch.isDigit() }) } },
                        label = { Text(stringResource(id = R.string.label_sets_count)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = item.repsRange,
                        onValueChange = { value -> onUpdate { it.copy(repsRange = value) } },
                        label = { Text(stringResource(id = R.string.label_reps_range)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = item.supersetId,
                    onValueChange = { value -> onUpdate { it.copy(supersetId = value) } },
                    label = { Text(stringResource(id = R.string.label_superset_id)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SupersetDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.dialog_add_superset_title)) },
        text = {
            Column {
                Text(text = stringResource(id = R.string.label_superset_hint))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(id = R.string.label_superset_id)) }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(value.trim()) },
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
