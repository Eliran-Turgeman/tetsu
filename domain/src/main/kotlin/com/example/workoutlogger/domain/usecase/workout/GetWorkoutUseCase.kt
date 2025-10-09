package com.example.workoutlogger.domain.usecase.workout

import com.example.workoutlogger.domain.model.Workout
import com.example.workoutlogger.domain.repository.WorkoutRepository
import javax.inject.Inject

/**
 * Loads a workout and its items by identifier.
 */
class GetWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(id: Long): Workout? = workoutRepository.getWorkout(id)
}
