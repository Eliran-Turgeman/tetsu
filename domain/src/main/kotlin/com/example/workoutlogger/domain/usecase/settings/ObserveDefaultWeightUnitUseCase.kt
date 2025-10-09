package com.example.workoutlogger.domain.usecase.settings

import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits the current default weight unit preference. */
class ObserveDefaultWeightUnitUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<WeightUnit> = settingsRepository.defaultWeightUnit
}
