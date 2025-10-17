package com.example.workoutlogger.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workoutlogger.R
import com.example.workoutlogger.domain.importexport.CsvProfileType
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.ui.components.PrimaryButton
import com.example.workoutlogger.ui.components.SecondaryButton
import com.example.workoutlogger.ui.components.SectionHeader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import com.example.workoutlogger.ui.components.SegmentedControl

@Composable
fun SettingsRoute(
    onOpenNotificationSettings: () -> Unit,
    onOpenShareConsistency: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is SettingsEvent.ExportCsvSuccess -> when (event.profile) {
                    CsvProfileType.STRONG_HEVY -> context.getString(R.string.snackbar_export_csv_strong_success)
                    CsvProfileType.FITNOTES_IOS -> context.getString(R.string.snackbar_export_csv_fitnotes_success)
                }
                SettingsEvent.ExportJsonSuccess -> context.getString(R.string.snackbar_export_json_success)
                is SettingsEvent.ImportSuccess -> context.getString(
                    R.string.snackbar_import_success,
                    event.result.workouts,
                    event.result.sets
                )
                is SettingsEvent.Error -> event.message
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        val granted = isNotificationGranted(context)
        viewModel.refreshPermissionStatus(granted)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refreshPermissionStatus(granted || isNotificationGranted(context))
        viewModel.markPermissionRequested()
    }

    val exportStrongLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        viewModel.exportCsv(CsvProfileType.STRONG_HEVY, stream)
                    } ?: viewModel.reportError(context.getString(R.string.error_open_destination))
                }.onFailure { throwable ->
                    viewModel.reportError(throwable.message ?: context.getString(R.string.error_open_destination))
                }
            }
        }
    }

    val exportFitNotesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        viewModel.exportCsv(CsvProfileType.FITNOTES_IOS, stream)
                    } ?: viewModel.reportError(context.getString(R.string.error_open_destination))
                }.onFailure { throwable ->
                    viewModel.reportError(throwable.message ?: context.getString(R.string.error_open_destination))
                }
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        viewModel.exportJson(stream)
                    } ?: viewModel.reportError(context.getString(R.string.error_open_destination))
                }.onFailure { throwable ->
                    viewModel.reportError(throwable.message ?: context.getString(R.string.error_open_destination))
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    val mime = context.contentResolver.getType(uri)?.lowercase(Locale.US)
                    val isJson = mime?.contains("json") == true || uri.toString().lowercase(Locale.US).endsWith(".json")
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        if (isJson) {
                            viewModel.importJson(stream)
                        } else {
                            viewModel.importCsv(stream)
                        }
                    } ?: viewModel.reportError(context.getString(R.string.error_open_document))
                }.onFailure { throwable ->
                    viewModel.reportError(throwable.message ?: context.getString(R.string.error_open_document))
                }
            }
        }
    }

    val supportedImportMimeTypes = remember {
        arrayOf("text/*", "application/json", "application/octet-stream")
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
        onOpenNotificationSettings = onOpenNotificationSettings,
        onExportStrongCsv = { fileName -> exportStrongLauncher.launch(fileName) },
        onExportFitNotesCsv = { fileName -> exportFitNotesLauncher.launch(fileName) },
        onExportJson = { fileName -> exportJsonLauncher.launch(fileName) },
        onImportData = { importLauncher.launch(supportedImportMimeTypes) },
        onShareConsistency = onOpenShareConsistency
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
    onOpenNotificationSettings: () -> Unit,
    onExportStrongCsv: (String) -> Unit,
    onExportFitNotesCsv: (String) -> Unit,
    onExportJson: (String) -> Unit,
    onImportData: () -> Unit,
    onShareConsistency: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val units = remember { WeightUnit.values() }
    val fileFormatter = remember { DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss") }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.nav_settings),
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item("units") {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(title = stringResource(id = R.string.label_settings_units))
                        SegmentedControl(
                            options = units.map { it.name },
                            selectedIndex = units.indexOf(state.defaultUnit),
                            onSelect = { index -> onSelectUnit(units[index]) }
                        )
                    }
                }
            }

            item("notifications") {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(title = stringResource(id = R.string.label_settings_notifications))
                        NotificationStatusRow(granted = state.notificationPermissionGranted)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimaryButton(
                                text = stringResource(id = R.string.label_notification_permission_request),
                                onClick = onRequestPermission,
                                enabled = !state.notificationPermissionGranted
                            )
                            SecondaryButton(
                                text = stringResource(id = R.string.content_description_open_settings),
                                onClick = onOpenNotificationSettings
                            )
                        }
                    }
                }
            }

            item("data") {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader(title = stringResource(id = R.string.label_settings_data))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryButton(
                                text = stringResource(id = R.string.label_share_consistency),
                                onClick = onShareConsistency,
                                enabled = !state.isProcessing
                            )
                            SecondaryButton(
                                text = stringResource(id = R.string.label_export_strong_csv),
                                onClick = {
                                    val timestamp = LocalDateTime.now().format(fileFormatter)
                                    onExportStrongCsv("tetsu-${timestamp}-strong.csv")
                                },
                                enabled = !state.isProcessing
                            )
                            SecondaryButton(
                                text = stringResource(id = R.string.label_export_fitnotes_csv),
                                onClick = {
                                    val timestamp = LocalDateTime.now().format(fileFormatter)
                                    onExportFitNotesCsv("tetsu-${timestamp}-fitnotes.csv")
                                },
                                enabled = !state.isProcessing
                            )
                            SecondaryButton(
                                text = stringResource(id = R.string.label_export_json_backup),
                                onClick = {
                                    val timestamp = LocalDateTime.now().format(fileFormatter)
                                    onExportJson("tetsu-${timestamp}-backup.json")
                                },
                                enabled = !state.isProcessing
                            )
                            PrimaryButton(
                                text = stringResource(id = R.string.label_import_workout_data),
                                onClick = onImportData,
                                enabled = !state.isProcessing
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
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
            content = content
        )
    }
}

@Composable
private fun NotificationStatusRow(granted: Boolean) {
    val label = if (granted) {
        stringResource(id = R.string.label_permission_granted)
    } else {
        stringResource(id = R.string.label_permission_not_granted)
    }
    val badgeColor = if (granted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
    }
    val textColor = if (granted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Surface(
        color = badgeColor,
        contentColor = textColor,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
