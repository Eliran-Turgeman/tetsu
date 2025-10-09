package com.example.workoutlogger.domain.repository

import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.model.WorkoutSchedule
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeWorkouts(): Flow<List<Workout>>

    suspend fun getWorkout(id: Long): Workout?

    suspend fun upsertWorkout(workout: Workout): Long

    suspend fun deleteWorkout(id: Long)

    fun observeScheduleForWorkout(workoutId: Long): Flow<WorkoutSchedule?>

    suspend fun getScheduleForWorkout(workoutId: Long): WorkoutSchedule?

    fun observeSchedules(): Flow<List<WorkoutSchedule>>

    suspend fun upsertSchedule(schedule: WorkoutSchedule)

    suspend fun deleteScheduleForWorkout(workoutId: Long)
}

// Backwards-compatible alias until all callers migrate.
typealias TemplateRepository = WorkoutRepository
