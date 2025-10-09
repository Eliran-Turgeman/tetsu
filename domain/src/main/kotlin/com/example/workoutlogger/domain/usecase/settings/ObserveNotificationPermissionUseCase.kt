package com.example.workoutlogger.domain.usecase.settings

import com.example.workoutlogger.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits whether the notification permission prompt was already displayed. */
class ObserveNotificationPermissionUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = settingsRepository.notificationPermissionRequested
}
