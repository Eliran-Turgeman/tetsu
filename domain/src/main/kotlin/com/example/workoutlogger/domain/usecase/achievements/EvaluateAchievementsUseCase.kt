package com.example.workoutlogger.domain.usecase.achievements

import com.example.workoutlogger.domain.model.achievements.AchievementEvent
import com.example.workoutlogger.domain.repository.AchievementsRepository
import javax.inject.Inject

class EvaluateAchievementsUseCase @Inject constructor(
    private val achievementsRepository: AchievementsRepository
) {
    suspend operator fun invoke(): List<AchievementEvent> {
        return achievementsRepository.evaluateNow()
    }
}
