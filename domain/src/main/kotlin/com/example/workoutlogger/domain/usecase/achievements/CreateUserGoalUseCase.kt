package com.example.workoutlogger.domain.usecase.achievements

import com.example.workoutlogger.domain.model.achievements.UserGoal
import com.example.workoutlogger.domain.model.achievements.UserGoalKind
import com.example.workoutlogger.domain.repository.AchievementsRepository
import javax.inject.Inject
import kotlinx.datetime.Instant

class CreateUserGoalUseCase @Inject constructor(
    private val achievementsRepository: AchievementsRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String?,
        kind: UserGoalKind,
        exerciseName: String?,
        targetValue: Double,
        secondaryValue: Double?,
        windowDays: Int?,
        deadlineAt: Instant?
    ): UserGoal {
        return achievementsRepository.createUserGoal(
            title = title,
            description = description,
            kind = kind,
            exerciseName = exerciseName,
            targetValue = targetValue,
            secondaryValue = secondaryValue,
            windowDays = windowDays,
            deadlineAt = deadlineAt
        )
    }
}
