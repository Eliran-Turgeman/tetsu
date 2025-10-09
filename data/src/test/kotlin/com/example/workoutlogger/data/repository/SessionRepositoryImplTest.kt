package com.example.workoutlogger.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.workoutlogger.data.db.WorkoutLoggerDatabase
import com.example.workoutlogger.data.db.entity.TemplateItemEntity
import com.example.workoutlogger.data.db.entity.TemplateItemType
import com.example.workoutlogger.data.db.entity.WorkoutTemplateEntity
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WeightUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionRepositoryImplTest {

    private lateinit var database: WorkoutLoggerDatabase
    private lateinit var repository: SessionRepositoryImpl

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkoutLoggerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SessionRepositoryImpl(
            database = database,
            sessionDao = database.sessionDao(),
            templateDao = database.templateDao()
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `returns previous performance with best set`() = runTest {
        val templateId = database.templateDao().insertTemplate(
            WorkoutTemplateEntity(
                name = "Push",
                createdAt = Instant.parse("2024-04-01T00:00:00Z")
            )
        )
        database.templateDao().insertTemplateItems(
            listOf(
                TemplateItemEntity(
                    templateId = templateId,
                    position = 0,
                    type = TemplateItemType.EXERCISE,
                    supersetGroupId = "A",
                    exerciseName = "Bench Press",
                    sets = 2,
                    repsMin = 6,
                    repsMax = 8
                )
            )
        )

        val firstSession = repository.startSessionFromTemplate(
            templateId,
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

        val secondSession = repository.startSessionFromTemplate(
            templateId,
            Instant.parse("2024-04-14T08:00:00Z")
        )

        val previous = repository.getPreviousPerformance("Bench Press", secondSession.startedAt)
        requireNotNull(previous)
        assertEquals(2, previous.sets.size)
        assertEquals(62.5, previous.bestSet?.loggedWeight)
        assertEquals(7, previous.bestSet?.loggedReps)
    }
}
