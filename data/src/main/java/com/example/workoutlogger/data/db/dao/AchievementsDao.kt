package com.example.workoutlogger.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.workoutlogger.data.db.entity.AchievementDefinitionEntity
import com.example.workoutlogger.data.db.entity.AchievementInstanceEntity
import com.example.workoutlogger.data.db.entity.UserGoalEntity
import com.example.workoutlogger.data.db.entity.WorkoutDailySummaryEntity
import com.example.workoutlogger.domain.model.achievements.AchievementStatus
import kotlinx.datetime.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementsDao {

    @Query("SELECT * FROM achievement_definitions ORDER BY sort ASC")
    suspend fun getDefinitions(): List<AchievementDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDefinitions(definitions: List<AchievementDefinitionEntity>)

    @Query("DELETE FROM achievement_definitions WHERE id = :id")
    suspend fun deleteDefinition(id: String)

    @Query("SELECT * FROM achievement_instances")
    fun observeInstances(): Flow<List<AchievementInstanceEntity>>

    @Query("SELECT * FROM achievement_instances")
    suspend fun getInstances(): List<AchievementInstanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstance(instance: AchievementInstanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstances(instances: List<AchievementInstanceEntity>)

    @Query("DELETE FROM achievement_instances WHERE definitionId = :definitionId")
    suspend fun deleteInstancesByDefinition(definitionId: String)

    @Query("UPDATE achievement_instances SET progressCurrent = :current, progressTarget = :target, percent = :percent, progressUnit = :unit, status = :status, completedAt = :completedAt WHERE instanceId = :instanceId")
    suspend fun updateProgress(
        instanceId: String,
        current: Double,
        target: Double,
        percent: Double,
        unit: String,
        status: AchievementStatus,
        completedAt: Instant?
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGoal(goal: UserGoalEntity)

    @Update
    suspend fun updateUserGoal(goal: UserGoalEntity)

    @Query("DELETE FROM user_goals WHERE goalId = :goalId")
    suspend fun deleteGoal(goalId: String)

    @Query("SELECT * FROM user_goals ORDER BY createdAt DESC")
    suspend fun getUserGoals(): List<UserGoalEntity>

    @Query("SELECT * FROM user_goals ORDER BY createdAt DESC")
    fun observeUserGoals(): Flow<List<UserGoalEntity>>

    @Query("SELECT DISTINCT exercise_name FROM session_exercises WHERE exercise_name LIKE :prefix || '%' ORDER BY exercise_name LIMIT :limit")
    suspend fun getDistinctExerciseNames(prefix: String, limit: Int = 50): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummaries(summaries: List<WorkoutDailySummaryEntity>)

    @Query("SELECT * FROM workout_daily_summaries WHERE date_epoch_day BETWEEN :startEpochDay AND :endEpochDay ORDER BY date_epoch_day ASC")
    suspend fun getDailySummariesBetween(startEpochDay: Long, endEpochDay: Long): List<WorkoutDailySummaryEntity>

    @Query("SELECT * FROM workout_daily_summaries ORDER BY date_epoch_day DESC LIMIT :limit")
    suspend fun getRecentDailySummaries(limit: Int): List<WorkoutDailySummaryEntity>

    @Query("DELETE FROM workout_daily_summaries WHERE date_epoch_day = :epochDay")
    suspend fun deleteDailySummary(epochDay: Long)

    @Query("DELETE FROM workout_daily_summaries")
    suspend fun clearDailySummaries()
}
