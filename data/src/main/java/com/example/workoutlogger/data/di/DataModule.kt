package com.example.workoutlogger.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.workoutlogger.data.db.WorkoutLoggerDatabase
import com.example.workoutlogger.data.db.dao.AchievementsDao
import com.example.workoutlogger.data.db.dao.ScheduleDao
import com.example.workoutlogger.data.db.dao.SessionDao
import com.example.workoutlogger.data.db.dao.WorkoutDao
import com.example.workoutlogger.data.db.migration.MIGRATION_1_2
import com.example.workoutlogger.data.repository.SessionRepositoryImpl
import com.example.workoutlogger.data.repository.WorkoutRepositoryImpl
import com.example.workoutlogger.data.settings.SettingsRepositoryImpl
import com.example.workoutlogger.domain.repository.SessionRepository
import com.example.workoutlogger.domain.repository.SettingsRepository
import com.example.workoutlogger.domain.repository.WorkoutRepository
import com.example.workoutlogger.data.repository.AchievementsRepositoryImpl
import com.example.workoutlogger.domain.repository.AchievementsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

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

    @Binds
    @Singleton
    abstract fun bindAchievementsRepository(impl: AchievementsRepositoryImpl): AchievementsRepository
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
        ).addMigrations(MIGRATION_1_2)
            .addCallback(SeedCallback)
            .build()
    }

    @Provides
    fun provideWorkoutDao(database: WorkoutLoggerDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideSessionDao(database: WorkoutLoggerDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideScheduleDao(database: WorkoutLoggerDatabase): ScheduleDao = database.scheduleDao()

    @Provides
    fun provideAchievementsDao(database: WorkoutLoggerDatabase): AchievementsDao = database.achievementsDao()

    @Provides
    @Singleton
    fun providePreferenceDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(DATASTORE_NAME)
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
}

private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        com.example.workoutlogger.data.achievements.AchievementSeedData.insertDefinitions(db)
        com.example.workoutlogger.data.achievements.AchievementSeedData.insertInstancesFromDefinitions(db, System.currentTimeMillis())
    }
}
