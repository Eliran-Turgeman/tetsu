package com.example.workoutlogger.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.workoutlogger.data.db.WorkoutLoggerDatabase
import com.example.workoutlogger.data.db.dao.SessionDao
import com.example.workoutlogger.data.db.dao.WorkoutDao
import com.example.workoutlogger.data.db.entity.SessionStatus
import com.example.workoutlogger.data.db.entity.WorkoutEntity
import com.example.workoutlogger.data.db.entity.WorkoutItemEntity
import com.example.workoutlogger.data.db.entity.WorkoutItemType
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WeightUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionRepositoryImplTest {

    private lateinit var database: WorkoutLoggerDatabase
    private lateinit var repository: SessionRepositoryImpl
    private lateinit var sessionDao: SessionDao
    private lateinit var workoutDao: WorkoutDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkoutLoggerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = database.sessionDao()
        workoutDao = database.workoutDao()
        repository = SessionRepositoryImpl(
            database = database,
            sessionDao = sessionDao,
            workoutDao = workoutDao
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `returns previous performance with best set`() = runTest {
        val workoutId = seedWorkout()

        val firstSession = repository.startSessionFromWorkout(
            workoutId,
            Instant.parse("2024-04-07T08:00:00Z")
        )
        val bench = firstSession.exercises.first()
        bench.sets.forEachIndexed { index, set ->
            repository.upsertSetLog(
                bench.id!!,
                SessionSetLog(
                    id = set.id,
                    sessionExerciseId = bench.id,
                    setIndex = set.setIndex,
                    targetRepsMin = set.targetRepsMin,
                    targetRepsMax = set.targetRepsMax,
                    loggedReps = if (index == 0) 8 else 7,
                    loggedWeight = if (index == 0) 60.0 else 62.5,
                    unit = WeightUnit.KG,
                    note = null
                )
            )
        }
        repository.finishSession(firstSession.id!!, Instant.parse("2024-04-07T09:00:00Z"))

        val secondSession = repository.startSessionFromWorkout(
            workoutId,
            Instant.parse("2024-04-14T08:00:00Z")
        )

        val previous = repository.getPreviousPerformance("Bench Press", secondSession.startedAt)
        requireNotNull(previous)
        assertEquals(2, previous.sets.size)
        assertEquals(62.5, previous.bestSet?.loggedWeight)
        assertEquals(7, previous.bestSet?.loggedReps)
    }

    @Test
    fun `cancel session removes session and logs`() = runTest {
        val workoutId = seedWorkout()
        val session = repository.startSessionFromWorkout(
            workoutId,
            Instant.parse("2024-05-01T08:00:00Z")
        )
        val sessionId = session.id!!
        val exerciseId = session.exercises.first().id!!
        val setId = session.exercises.first().sets.first().id!!

        repository.cancelSession(sessionId)

        assertNull(sessionDao.getSessionById(sessionId))
        assertNull(sessionDao.getExerciseById(exerciseId))
        assertNull(sessionDao.getSetLogById(setId))
    }

    @Test
    fun `observeSessionsByDateRange excludes cancelled sessions`() = runTest {
        val workoutId = seedWorkout()
        val session = repository.startSessionFromWorkout(
            workoutId,
            Instant.parse("2024-05-01T08:00:00Z")
        )
        repository.finishSession(session.id!!, Instant.parse("2024-05-01T09:00:00Z"))

        // Insert a cancelled session manually to mimic existing data from older versions.
        database.sessionDao().insertSession(
            com.example.workoutlogger.data.db.entity.WorkoutSessionEntity(
                workoutId = workoutId,
                workoutNameSnapshot = "Push",
                startedAt = Instant.parse("2024-05-02T08:00:00Z"),
                endedAt = Instant.parse("2024-05-02T08:30:00Z"),
                status = SessionStatus.CANCELLED
            )
        )

        val sessions = repository.observeSessionsByDateRange(
            LocalDate(2024, 5, 1),
            LocalDate(2024, 5, 7)
        ).first()

        assertEquals(1, sessions.size)
        assertEquals(com.example.workoutlogger.domain.model.WorkoutStatus.COMPLETED, sessions.single().status)
    }

    private suspend fun seedWorkout(): Long {
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(
                name = "Push",
                createdAt = Instant.parse("2024-04-01T00:00:00Z")
            )
        )
        workoutDao.insertWorkoutItems(
            listOf(
                WorkoutItemEntity(
                    workoutId = workoutId,
                    position = 0,
                    type = WorkoutItemType.EXERCISE,
                    supersetGroupId = "A",
                    exerciseName = "Bench Press",
                    sets = 2,
                    repsMin = 6,
                    repsMax = 8
                )
            )
        )
        return workoutId
    }
}
