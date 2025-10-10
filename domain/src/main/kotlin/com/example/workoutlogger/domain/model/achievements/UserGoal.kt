package com.example.workoutlogger.domain.model.achievements

import kotlinx.datetime.Instant

data class UserGoal(
    val goalId: String,
    val title: String,
    val description: String?,
    val kind: UserGoalKind,
    val exerciseName: String?,
    val targetValue: Double,
    val secondaryValue: Double?,
    val windowDays: Int?,
    val deadlineAt: Instant?,
    val createdAt: Instant
)
