package com.example.workoutlogger.domain.usecase.settings

import com.example.workoutlogger.domain.repository.SettingsRepository
import javax.inject.Inject

/** Persists whether notification permission has been requested. */
class SetNotificationPermissionRequestedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(requested: Boolean) {
        settingsRepository.setNotificationPermissionRequested(requested)
    }
}
