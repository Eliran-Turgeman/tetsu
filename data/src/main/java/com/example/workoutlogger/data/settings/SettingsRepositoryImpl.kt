package com.example.workoutlogger.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private val defaultUnitKey = stringPreferencesKey("default_weight_unit")
    private val notificationPermissionKey = booleanPreferencesKey("notification_permission_requested")

    override val defaultWeightUnit: Flow<WeightUnit> = dataStore.data.map { prefs ->
        prefs[defaultUnitKey]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: WeightUnit.KG
    }

    override suspend fun setDefaultWeightUnit(unit: WeightUnit) {
        dataStore.edit { prefs ->
            prefs[defaultUnitKey] = unit.name
        }
    }

    override val notificationPermissionRequested: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[notificationPermissionKey] ?: false
    }

    override suspend fun setNotificationPermissionRequested(requested: Boolean) {
        dataStore.edit { prefs ->
            prefs[notificationPermissionKey] = requested
        }
    }
}
