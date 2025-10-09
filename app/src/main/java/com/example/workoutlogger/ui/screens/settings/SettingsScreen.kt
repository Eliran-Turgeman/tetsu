package com.example.workoutlogger.ui.screens.settings

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.model.WeightUnit

@Composable
fun SettingsRoute(
    onOpenNotificationSettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val granted = isNotificationGranted(context)
        viewModel.refreshPermissionStatus(granted)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refreshPermissionStatus(granted || isNotificationGranted(context))
        viewModel.markPermissionRequested()
    }

    SettingsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSelectUnit = viewModel::updateDefaultUnit,
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onOpenNotificationSettings = onOpenNotificationSettings
    )
}

private fun isNotificationGranted(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onSelectUnit: (WeightUnit) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(text = stringResource(id = R.string.nav_settings)) }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(id = R.string.label_settings_units), style = MaterialTheme.typography.titleMedium)
                    WeightUnit.values().forEach { unit ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = unit.name)
                            RadioButton(selected = state.defaultUnit == unit, onClick = { onSelectUnit(unit) })
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(id = R.string.label_settings_notifications), style = MaterialTheme.typography.titleMedium)
                    val permissionStatus = if (state.notificationPermissionGranted) {
                        stringResource(id = R.string.label_permission_granted)
                    } else {
                        stringResource(id = R.string.label_permission_not_granted)
                    }
                    Text(text = permissionStatus)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRequestPermission, enabled = !state.notificationPermissionGranted) {
                            Text(text = stringResource(id = R.string.label_notification_permission_request))
                        }
                        Button(onClick = onOpenNotificationSettings) {
                            Text(text = stringResource(id = R.string.content_description_open_settings))
                        }
                    }
                }
            }
        }
    }
}
