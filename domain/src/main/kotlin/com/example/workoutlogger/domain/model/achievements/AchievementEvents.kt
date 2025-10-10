package com.example.workoutlogger.domain.model.achievements

import kotlinx.datetime.Instant

sealed class AchievementEvent {
    data class Completed(
        val instanceId: String,
        val definitionId: String,
        val title: String,
        val completedAt: Instant
    ) : AchievementEvent()

    data class GoalDeadlineApproaching(
        val instanceId: String,
        val definitionId: String,
        val title: String,
        val deadlineAt: Instant
    ) : AchievementEvent()
}
