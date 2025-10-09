package com.example.workoutlogger.domain.usecase.settings

import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.repository.SettingsRepository
import javax.inject.Inject

/** Updates the preferred weight unit. */
class SetDefaultWeightUnitUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(unit: WeightUnit) {
        settingsRepository.setDefaultWeightUnit(unit)
    }
}
