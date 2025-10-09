package com.example.workoutlogger.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.workoutlogger.data.db.entity.WorkoutItemEntity
import com.example.workoutlogger.data.db.entity.WorkoutWithItems
import com.example.workoutlogger.data.db.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY created_at DESC")
    fun observeWorkoutsWithItems(): Flow<List<WorkoutWithItems>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutWithItems(id: Long): WorkoutWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workout_items WHERE workout_id = :workoutId")
    suspend fun deleteWorkoutItems(workoutId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutItems(items: List<WorkoutItemEntity>)

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutEntity(id: Long): WorkoutEntity?
}
