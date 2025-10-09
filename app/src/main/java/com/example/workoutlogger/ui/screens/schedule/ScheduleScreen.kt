package com.example.workoutlogger.ui.screens.schedule

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

@Composable
fun ScheduleRoute(
    templateId: Long,
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ScheduleScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onToggleDay = viewModel::toggleDay,
        onToggleEnabled = viewModel::toggleEnabled,
        onChangeTime = viewModel::updateTime,
        onSave = {
            viewModel.save { enabled ->
                scope.launch {
                    val messageRes = if (enabled) {
                        R.string.snackbar_schedule_saved
                    } else {
                        R.string.snackbar_schedule_disabled
                    }
                    snackbarHostState.showSnackbar(context.getString(messageRes))
                    onBack()
                }
            }
        },
        onBack = onBack
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleScreen(
    state: ScheduleUiState,
    snackbarHostState: SnackbarHostState,
    onToggleDay: (DayOfWeek) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onChangeTime: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val timePicker = remember(state.hour, state.minute) {
        TimePickerDialog(context, { _, hour, minute ->
            onChangeTime(hour, minute)
        }, state.hour, state.minute, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.label_schedule_header)) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = stringResource(id = R.string.label_select_days), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.values().forEach { day ->
                    FilterChip(
                        selected = day in state.days,
                        onClick = { onToggleDay(day) },
                        label = { Text(text = day.name.take(3)) }
                    )
                }
            }

            Text(text = stringResource(id = R.string.label_select_time), style = MaterialTheme.typography.titleMedium)
            Button(onClick = { timePicker.show() }) {
                Text(text = stringResource(id = R.string.label_schedule_time_prefix, String.format("%02d:%02d", state.hour, state.minute)))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(id = R.string.label_enable_schedule))
                Switch(checked = state.enabled, onCheckedChange = onToggleEnabled)
            }

            Button(onClick = onSave, enabled = state.days.isNotEmpty()) {
                Text(text = stringResource(id = R.string.action_save))
            }
        }
    }
}
