package com.example.workoutlogger.data.repository

import androidx.room.withTransaction
import com.example.workoutlogger.data.db.WorkoutLoggerDatabase
import com.example.workoutlogger.data.db.dao.ScheduleDao
import com.example.workoutlogger.data.db.dao.WorkoutDao
import com.example.workoutlogger.data.mapper.toDomain
import com.example.workoutlogger.data.mapper.toEntity
import com.example.workoutlogger.domain.model.WorkoutSchedule
import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val database: WorkoutLoggerDatabase,
    private val workoutDao: WorkoutDao,
    private val scheduleDao: ScheduleDao
) : WorkoutRepository {

    override fun observeWorkouts(): Flow<List<Workout>> {
        return workoutDao.observeWorkoutsWithItems().map { workouts ->
            workouts.map { it.toDomain() }
        }
    }

    override suspend fun getWorkout(id: Long): Workout? {
        return workoutDao.getWorkoutWithItems(id)?.toDomain()
    }

    override suspend fun upsertWorkout(workout: Workout): Long {
        return database.withTransaction {
            val workoutId = if (workout.id == null) {
                workoutDao.insertWorkout(workout.toEntity())
            } else {
                val existingId = workout.id
                    ?: throw IllegalStateException("Workout id missing for update")
                workoutDao.updateWorkout(workout.toEntity())
                existingId
            }

            workoutDao.deleteWorkoutItems(workoutId)
            if (workout.items.isNotEmpty()) {
                val orderedItems = workout.items.sortedBy { it.position }
                workoutDao.insertWorkoutItems(orderedItems.map { it.toEntity(workoutId) })
            }
            workoutId
        }
    }

    override suspend fun deleteWorkout(id: Long) {
        database.withTransaction {
            workoutDao.deleteWorkoutItems(id)
            workoutDao.getWorkoutEntity(id)?.let { workoutDao.deleteWorkout(it) }
            scheduleDao.deleteByWorkoutId(id)
        }
    }

    override fun observeScheduleForWorkout(workoutId: Long): Flow<WorkoutSchedule?> {
        return scheduleDao.observeScheduleForWorkout(workoutId).map { it?.toDomain() }
    }

    override suspend fun getScheduleForWorkout(workoutId: Long): WorkoutSchedule? {
        return scheduleDao.getScheduleForWorkout(workoutId)?.toDomain()
    }

    override fun observeSchedules(): Flow<List<WorkoutSchedule>> {
        return scheduleDao.observeSchedules().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun upsertSchedule(schedule: WorkoutSchedule) {
        scheduleDao.upsert(schedule.toEntity())
    }

    override suspend fun deleteScheduleForWorkout(workoutId: Long) {
        scheduleDao.deleteByWorkoutId(workoutId)
    }

    override suspend fun getDistinctExerciseNames(): List<String> {
        return workoutDao.getDistinctExerciseNames()
    }
}
