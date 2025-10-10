package com.example.workoutlogger.domain.usecase.achievements

import com.example.workoutlogger.domain.repository.AchievementsRepository
import javax.inject.Inject

class DeleteUserGoalUseCase @Inject constructor(
    private val achievementsRepository: AchievementsRepository
) {
    suspend operator fun invoke(goalId: String) {
        achievementsRepository.deleteGoal(goalId)
    }
}
