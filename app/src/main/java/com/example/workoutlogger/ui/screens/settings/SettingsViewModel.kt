package com.example.workoutlogger.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.usecase.settings.ObserveDefaultWeightUnitUseCase
import com.example.workoutlogger.domain.usecase.settings.ObserveNotificationPermissionUseCase
import com.example.workoutlogger.domain.usecase.settings.SetDefaultWeightUnitUseCase
import com.example.workoutlogger.domain.usecase.settings.SetNotificationPermissionRequestedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeDefaultWeightUnitUseCase: ObserveDefaultWeightUnitUseCase,
    private val setDefaultWeightUnitUseCase: SetDefaultWeightUnitUseCase,
    observeNotificationPermissionUseCase: ObserveNotificationPermissionUseCase,
    private val setNotificationPermissionRequestedUseCase: SetNotificationPermissionRequestedUseCase
) : ViewModel() {

    private val _hasNotificationPermission = MutableStateFlow(false)
    val uiState: StateFlow<SettingsUiState> = combine(
        observeDefaultWeightUnitUseCase(),
        observeNotificationPermissionUseCase(),
        _hasNotificationPermission
    ) { unit, requested, granted ->
        SettingsUiState(
            defaultUnit = unit,
            notificationPermissionRequested = requested,
            notificationPermissionGranted = granted
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

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
}

data class SettingsUiState(
    val defaultUnit: WeightUnit = WeightUnit.KG,
    val notificationPermissionRequested: Boolean = false,
    val notificationPermissionGranted: Boolean = false
)
