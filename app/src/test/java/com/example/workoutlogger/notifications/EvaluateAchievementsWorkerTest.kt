package com.example.workoutlogger.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.model.achievements.AchievementEvent
import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.model.achievements.AchievementStatus
import com.example.workoutlogger.domain.model.achievements.AchievementType
import com.example.workoutlogger.domain.model.achievements.MetricType
import com.example.workoutlogger.domain.model.achievements.Progress
import com.example.workoutlogger.domain.model.achievements.UserGoal
import com.example.workoutlogger.domain.model.achievements.UserGoalKind
import com.example.workoutlogger.domain.repository.AchievementsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.runner.RunWith

@RunWith(org.robolectric.RobolectricTestRunner::class)
class EvaluateAchievementsWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun workerInvokesRepositoryEvaluation() = runBlocking {
        val repository = RecordingAchievementsRepository()
        val worker = TestListenableWorkerBuilder<EvaluateAchievementsWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return EvaluateAchievementsWorker(appContext, workerParameters, repository)
                }
            })
            .build()

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(repository.called)
    }

    private class RecordingAchievementsRepository : AchievementsRepositoryStub() {
        var called = false
        override suspend fun evaluateNow(): List<AchievementEvent> {
            called = true
            return emptyList()
        }
    }
}

abstract class AchievementsRepositoryStub : AchievementsRepository {
    override suspend fun getCatalog(): List<AchievementDefinition> = emptyList()
    override fun observeInstances(): kotlinx.coroutines.flow.Flow<List<AchievementInstance>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun createUserGoal(
        title: String,
        description: String?,
        kind: UserGoalKind,
        exerciseName: String?,
        targetValue: Double,
        secondaryValue: Double?,
        windowDays: Int?,
        deadlineAt: Instant?
    ): UserGoal = throw UnsupportedOperationException()

    override suspend fun deleteGoal(goalId: String) {}
    override suspend fun updateAchievementProgress(instanceId: String, progress: Progress, completedAt: Instant?) {}
    override suspend fun insertOrUpdateInstance(instance: AchievementInstance) {}
    override suspend fun seedDefinitions(definitions: List<AchievementDefinition>) {}
    override suspend fun listUserGoals(): List<UserGoal> = emptyList()
    override fun observeUserGoals(): kotlinx.coroutines.flow.Flow<List<UserGoal>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun evaluateNow(): List<AchievementEvent> = emptyList()
    override suspend fun getDistinctExerciseNames(query: String?): List<String> = emptyList()
    override fun observeEvents(): kotlinx.coroutines.flow.Flow<AchievementEvent> = kotlinx.coroutines.flow.emptyFlow()
}
