package com.example.workoutlogger.domain.repository

import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.model.achievements.AchievementEvent
import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.model.achievements.Progress
import com.example.workoutlogger.domain.model.achievements.UserGoal
import com.example.workoutlogger.domain.model.achievements.UserGoalKind
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface AchievementsRepository {
    suspend fun getCatalog(): List<AchievementDefinition>

    fun observeInstances(): Flow<List<AchievementInstance>>

    suspend fun createUserGoal(
        title: String,
        description: String?,
        kind: UserGoalKind,
        exerciseName: String?,
        targetValue: Double,
        secondaryValue: Double?,
        windowDays: Int?,
        deadlineAt: Instant?
    ): UserGoal

    suspend fun deleteGoal(goalId: String)

    suspend fun updateAchievementProgress(instanceId: String, progress: Progress, completedAt: Instant?)

    suspend fun insertOrUpdateInstance(instance: AchievementInstance)

    suspend fun seedDefinitions(definitions: List<AchievementDefinition>)

    suspend fun listUserGoals(): List<UserGoal>

    fun observeUserGoals(): Flow<List<UserGoal>>

    suspend fun evaluateNow(): List<AchievementEvent>

    suspend fun getDistinctExerciseNames(query: String? = null): List<String>

    fun observeEvents(): Flow<AchievementEvent>
}
