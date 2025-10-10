package com.example.workoutlogger.domain.usecase.achievements

import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.repository.AchievementsRepository
import javax.inject.Inject

class GetAchievementCatalogUseCase @Inject constructor(
    private val achievementsRepository: AchievementsRepository
) {
    suspend operator fun invoke(): List<AchievementDefinition> = achievementsRepository.getCatalog()
}
