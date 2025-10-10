package com.example.workoutlogger.domain.usecase.schedule

import com.example.workoutlogger.domain.repository.WorkoutRepository
import com.example.workoutlogger.domain.usecase.achievements.EvaluateAchievementsUseCase
import javax.inject.Inject

/** Removes a schedule when the user disables reminders. */
class DeleteScheduleUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase
) {
    suspend operator fun invoke(workoutId: Long) {
        workoutRepository.deleteScheduleForWorkout(workoutId)
        evaluateAchievementsUseCase()
    }
}
