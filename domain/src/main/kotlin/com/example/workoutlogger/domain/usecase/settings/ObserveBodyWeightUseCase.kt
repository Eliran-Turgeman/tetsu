package com.example.workoutlogger.domain.usecase.settings

import com.example.workoutlogger.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveBodyWeightUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<Double?> = settingsRepository.bodyWeightKg
}
