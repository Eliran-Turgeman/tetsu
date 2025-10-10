package com.example.workoutlogger.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.workoutlogger.data.db.entity.WorkoutScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM workout_schedules")
    fun observeSchedules(): Flow<List<WorkoutScheduleEntity>>

    @Query("SELECT * FROM workout_schedules WHERE workout_id = :workoutId LIMIT 1")
    fun observeScheduleForWorkout(workoutId: Long): Flow<WorkoutScheduleEntity?>

    @Query("SELECT * FROM workout_schedules WHERE workout_id = :workoutId LIMIT 1")
    suspend fun getScheduleForWorkout(workoutId: Long): WorkoutScheduleEntity?

    @Query("SELECT * FROM workout_schedules")
    suspend fun getSchedules(): List<WorkoutScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: WorkoutScheduleEntity): Long

    @Query("DELETE FROM workout_schedules WHERE workout_id = :workoutId")
    suspend fun deleteByWorkoutId(workoutId: Long)
}
