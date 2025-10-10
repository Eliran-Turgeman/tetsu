package com.example.workoutlogger.domain.usecase.achievements

import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.repository.AchievementsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAchievementInstancesUseCase @Inject constructor(
    private val achievementsRepository: AchievementsRepository
) {
    operator fun invoke(): Flow<List<AchievementInstance>> = achievementsRepository.observeInstances()
}
