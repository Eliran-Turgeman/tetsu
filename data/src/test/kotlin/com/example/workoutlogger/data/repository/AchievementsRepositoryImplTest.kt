package com.example.workoutlogger.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.workoutlogger.data.db.WorkoutLoggerDatabase
import com.example.workoutlogger.data.db.entity.SessionStatus
import com.example.workoutlogger.data.db.entity.WorkoutEntity
import com.example.workoutlogger.data.db.entity.WorkoutItemEntity
import com.example.workoutlogger.data.db.entity.WorkoutItemType
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.repository.SettingsRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AchievementsRepositoryImplTest {

    private lateinit var database: WorkoutLoggerDatabase
    private lateinit var sessionRepository: SessionRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkoutLoggerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionRepository = SessionRepositoryImpl(
            database = database,
            sessionDao = database.sessionDao(),
            workoutDao = database.workoutDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `evaluateNow ignores sessions that are not completed`() = runTest {
        val workoutId = insertWorkout()
        val sessionDao = database.sessionDao()

        val completed = sessionRepository.startSessionFromWorkout(
            workoutId,
            Instant.parse("2024-05-01T07:00:00Z")
        )
        sessionRepository.finishSession(completed.id!!, Instant.parse("2024-05-01T08:00:00Z"))

        sessionRepository.startSessionFromWorkout(
            workoutId,
            Instant.parse("2024-05-02T07:00:00Z")
        ) // leave active

        val cancelled = sessionRepository.startSessionFromWorkout(
            workoutId,
            Instant.parse("2024-05-03T07:00:00Z")
        )
        val entity = sessionDao.getSessionById(cancelled.id!!)
        requireNotNull(entity)
        sessionDao.updateSession(
            entity.copy(
                status = SessionStatus.CANCELLED,
                endedAt = entity.startedAt
            )
        )

        val achievementsRepository = AchievementsRepositoryImpl(
            database = database,
            achievementsDao = database.achievementsDao(),
            sessionDao = sessionDao,
            scheduleDao = database.scheduleDao(),
            settingsRepository = FakeSettingsRepository()
        )

        achievementsRepository.evaluateNow()

        val summaries = database.achievementsDao().getRecentDailySummaries(10)
        assertEquals(1, summaries.size)
        val summary = summaries.single()
        assertEquals(1, summary.workoutsCompleted)
        val date = epochDayToLocalDate(summary.dateEpochDay)
        assertEquals(LocalDate(2024, 5, 1), date)
        assertNull(sessionDao.getSessionById(cancelled.id!!))
    }

    private suspend fun insertWorkout(): Long {
        val workoutId = database.workoutDao().insertWorkout(
            WorkoutEntity(
                name = "Full Body",
                createdAt = Instant.parse("2024-04-01T00:00:00Z")
            )
        )
        database.workoutDao().insertWorkoutItems(
            listOf(
                WorkoutItemEntity(
                    workoutId = workoutId,
                    position = 0,
                    type = WorkoutItemType.EXERCISE,
                    supersetGroupId = null,
                    exerciseName = "Deadlift",
                    sets = 3,
                    repsMin = 5,
                    repsMax = 5
                )
            )
        )
        return workoutId
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val unit = MutableStateFlow(WeightUnit.KG)
        private val bodyWeight = MutableStateFlow<Double?>(null)

        override val defaultWeightUnit: Flow<WeightUnit> = unit

        override suspend fun setDefaultWeightUnit(unit: WeightUnit) {
            this.unit.value = unit
        }

        override val notificationPermissionRequested: Flow<Boolean> = flowOf(false)

        override suspend fun setNotificationPermissionRequested(requested: Boolean) = Unit

        override val bodyWeightKg: Flow<Double?> = bodyWeight

        override suspend fun setBodyWeightKg(weightKg: Double?) {
            bodyWeight.value = weightKg
        }
    }
}

private val epochStart = LocalDate(1970, 1, 1)

private fun epochDayToLocalDate(value: Long): LocalDate =
    epochStart.plus(value.toInt(), DateTimeUnit.DAY)
