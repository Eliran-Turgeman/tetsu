package com.example.workoutlogger.domain.usecase.workout

import com.example.workoutlogger.domain.repository.WorkoutRepository
import com.example.workoutlogger.domain.usecase.achievements.EvaluateAchievementsUseCase
import javax.inject.Inject

/**
 * Deletes a workout and its schedule.
 */
class DeleteWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase
) {
    suspend operator fun invoke(id: Long) {
        workoutRepository.deleteWorkout(id)
        evaluateAchievementsUseCase()
    }
}
