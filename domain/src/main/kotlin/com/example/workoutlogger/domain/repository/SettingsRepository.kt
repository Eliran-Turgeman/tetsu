package com.example.workoutlogger.domain.repository

import com.example.workoutlogger.domain.model.WeightUnit
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val defaultWeightUnit: Flow<WeightUnit>

    suspend fun setDefaultWeightUnit(unit: WeightUnit)

    val notificationPermissionRequested: Flow<Boolean>

    suspend fun setNotificationPermissionRequested(requested: Boolean)
}
