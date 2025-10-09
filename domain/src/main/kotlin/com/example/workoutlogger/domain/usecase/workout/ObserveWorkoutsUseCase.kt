package com.example.workoutlogger.domain.usecase.workout

import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams all workouts ordered by creation date.
 */
class ObserveWorkoutsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    operator fun invoke(): Flow<List<Workout>> = workoutRepository.observeWorkouts()
}
