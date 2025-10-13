package com.example.workoutlogger.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutlogger.domain.importexport.CsvProfileType
import com.example.workoutlogger.domain.importexport.model.ImportResult
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.usecase.importexport.ExportWorkoutCsvUseCase
import com.example.workoutlogger.domain.usecase.importexport.ExportWorkoutJsonUseCase
import com.example.workoutlogger.domain.usecase.importexport.ImportWorkoutCsvUseCase
import com.example.workoutlogger.domain.usecase.importexport.ImportWorkoutJsonUseCase
import com.example.workoutlogger.domain.usecase.settings.ObserveDefaultWeightUnitUseCase
import com.example.workoutlogger.domain.usecase.settings.ObserveNotificationPermissionUseCase
import com.example.workoutlogger.domain.usecase.settings.SetDefaultWeightUnitUseCase
import com.example.workoutlogger.domain.usecase.settings.SetNotificationPermissionRequestedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeDefaultWeightUnitUseCase: ObserveDefaultWeightUnitUseCase,
    private val setDefaultWeightUnitUseCase: SetDefaultWeightUnitUseCase,
    observeNotificationPermissionUseCase: ObserveNotificationPermissionUseCase,
    private val setNotificationPermissionRequestedUseCase: SetNotificationPermissionRequestedUseCase,
    private val exportWorkoutCsvUseCase: ExportWorkoutCsvUseCase,
    private val exportWorkoutJsonUseCase: ExportWorkoutJsonUseCase,
    private val importWorkoutCsvUseCase: ImportWorkoutCsvUseCase,
    private val importWorkoutJsonUseCase: ImportWorkoutJsonUseCase
) : ViewModel() {

    private val _hasNotificationPermission = MutableStateFlow(false)
    private val _isProcessing = MutableStateFlow(false)
    private val _events = MutableSharedFlow<SettingsEvent>()

    val uiState: StateFlow<SettingsUiState> = combine(
        observeDefaultWeightUnitUseCase(),
        observeNotificationPermissionUseCase(),
        _hasNotificationPermission,
        _isProcessing
    ) { unit, requested, granted, processing ->
        SettingsUiState(
            defaultUnit = unit,
            notificationPermissionRequested = requested,
            notificationPermissionGranted = granted,
            isProcessing = processing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun refreshPermissionStatus(granted: Boolean) {
        _hasNotificationPermission.value = granted
    }

    fun updateDefaultUnit(unit: WeightUnit) {
        viewModelScope.launch {
            setDefaultWeightUnitUseCase(unit)
        }
    }

    fun markPermissionRequested() {
        viewModelScope.launch {
            setNotificationPermissionRequestedUseCase(true)
        }
    }

    fun exportCsv(profileType: CsvProfileType, outputStream: OutputStream) {
        viewModelScope.launch {
            performOperation {
                withContext(Dispatchers.IO) {
                    exportWorkoutCsvUseCase(profileType.profile, outputStream)
                }
                SettingsEvent.ExportCsvSuccess(profileType)
            }
        }
    }

    fun exportJson(outputStream: OutputStream) {
        viewModelScope.launch {
            performOperation {
                withContext(Dispatchers.IO) {
                    exportWorkoutJsonUseCase(outputStream)
                }
                SettingsEvent.ExportJsonSuccess
            }
        }
    }

    fun importCsv(inputStream: InputStream) {
        viewModelScope.launch {
            performOperation {
                val result = withContext(Dispatchers.IO) {
                    inputStream.use { importWorkoutCsvUseCase(it) }
                }
                SettingsEvent.ImportSuccess(result)
            }
        }
    }

    fun importJson(inputStream: InputStream) {
        viewModelScope.launch {
            performOperation {
                val result = withContext(Dispatchers.IO) {
                    inputStream.use { importWorkoutJsonUseCase(it) }
                }
                SettingsEvent.ImportSuccess(result)
            }
        }
    }

    fun reportError(message: String) {
        viewModelScope.launch {
            _events.emit(SettingsEvent.Error(message))
        }
    }

    private suspend fun performOperation(block: suspend () -> SettingsEvent) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        val result = runCatching { block() }
        _isProcessing.value = false
        result.onSuccess { event ->
            _events.emit(event)
        }.onFailure { throwable ->
            _events.emit(SettingsEvent.Error(throwable.message ?: "Operation failed"))
        }
    }
}

data class SettingsUiState(
    val defaultUnit: WeightUnit = WeightUnit.KG,
    val notificationPermissionRequested: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val isProcessing: Boolean = false
)

sealed interface SettingsEvent {
    data class ExportCsvSuccess(val profile: CsvProfileType) : SettingsEvent
    data object ExportJsonSuccess : SettingsEvent
    data class ImportSuccess(val result: ImportResult) : SettingsEvent
    data class Error(val message: String) : SettingsEvent
}
