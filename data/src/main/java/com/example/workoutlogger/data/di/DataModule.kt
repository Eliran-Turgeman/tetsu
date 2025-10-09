package com.example.workoutlogger.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.workoutlogger.data.db.WorkoutLoggerDatabase
import com.example.workoutlogger.data.db.dao.ScheduleDao
import com.example.workoutlogger.data.db.dao.SessionDao
import com.example.workoutlogger.data.db.dao.WorkoutDao
import com.example.workoutlogger.data.repository.SessionRepositoryImpl
import com.example.workoutlogger.data.repository.WorkoutRepositoryImpl
import com.example.workoutlogger.data.settings.SettingsRepositoryImpl
import com.example.workoutlogger.domain.repository.SessionRepository
import com.example.workoutlogger.domain.repository.SettingsRepository
import com.example.workoutlogger.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "workout_logger.db"
    private const val DATASTORE_NAME = "workout_logger_prefs"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutLoggerDatabase {
        return Room.databaseBuilder(
            context,
            WorkoutLoggerDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideWorkoutDao(database: WorkoutLoggerDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideSessionDao(database: WorkoutLoggerDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideScheduleDao(database: WorkoutLoggerDatabase): ScheduleDao = database.scheduleDao()

    @Provides
    @Singleton
    fun providePreferenceDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(DATASTORE_NAME)
    }
}
