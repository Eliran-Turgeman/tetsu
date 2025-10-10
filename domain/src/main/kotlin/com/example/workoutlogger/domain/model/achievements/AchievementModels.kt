package com.example.workoutlogger.domain.model.achievements

import kotlinx.datetime.Instant

/** Defines a catalog entry for an achievement or goal template. */
data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val type: AchievementType,
    val metric: MetricType,
    val targetValue: Double,
    val windowDays: Int?,
    val repeatable: Boolean,
    val tier: Int,
    val iconKey: String,
    val sort: Int
)

/** Holds the state of a specific achievement instance tracked for the user. */
data class AchievementInstance(
    val instanceId: String,
    val definitionId: String,
    val createdAt: Instant,
    val status: AchievementStatus,
    val progress: Progress,
    val completedAt: Instant?,
    val userNotes: String?,
    val metadata: AchievementMetadata?
)

/** Progress value object with convenience helpers. */
data class Progress(
    val current: Double,
    val target: Double,
    val percent: Double,
    val unit: String
) {
    fun isComplete(): Boolean = percent >= 1.0 && target > 0.0
}

/** Optional structured metadata decoded from persistence extras. */
data class AchievementMetadata(
    val exerciseName: String? = null,
    val exerciseId: String? = null,
    val deadlineAt: Instant? = null,
    val secondaryTarget: Double? = null,
    val windowDays: Int? = null
)
