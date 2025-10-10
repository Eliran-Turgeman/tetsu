package com.example.workoutlogger.data.mapper

import com.example.workoutlogger.data.db.entity.AchievementDefinitionEntity
import com.example.workoutlogger.data.db.entity.AchievementInstanceEntity
import com.example.workoutlogger.data.db.entity.UserGoalEntity
import com.example.workoutlogger.domain.model.achievements.AchievementDefinition
import com.example.workoutlogger.domain.model.achievements.AchievementInstance
import com.example.workoutlogger.domain.model.achievements.AchievementMetadata
import com.example.workoutlogger.domain.model.achievements.Progress
import com.example.workoutlogger.domain.model.achievements.UserGoal
import kotlinx.datetime.Instant
import org.json.JSONObject

fun AchievementDefinitionEntity.toDomain(): AchievementDefinition = AchievementDefinition(
    id = id,
    title = title,
    description = description,
    type = type,
    metric = metric,
    targetValue = targetValue,
    windowDays = windowDays,
    repeatable = repeatable,
    tier = tier,
    iconKey = iconKey,
    sort = sort
)

fun AchievementDefinition.toEntity(): AchievementDefinitionEntity = AchievementDefinitionEntity(
    id = id,
    title = title,
    description = description,
    type = type,
    metric = metric,
    targetValue = targetValue,
    windowDays = windowDays,
    repeatable = repeatable,
    tier = tier,
    iconKey = iconKey,
    sort = sort
)

fun AchievementInstanceEntity.toDomain(): AchievementInstance = AchievementInstance(
    instanceId = instanceId,
    definitionId = definitionId,
    createdAt = createdAt,
    status = status,
    progress = Progress(
        current = progressCurrent,
        target = progressTarget,
        percent = percent,
        unit = progressUnit
    ),
    completedAt = completedAt,
    userNotes = userNotes,
    metadata = extraJson?.let(::parseMetadata)
)

fun AchievementInstance.toEntity(): AchievementInstanceEntity = AchievementInstanceEntity(
    instanceId = instanceId,
    definitionId = definitionId,
    createdAt = createdAt,
    status = status,
    progressCurrent = progress.current,
    progressTarget = progress.target,
    progressUnit = progress.unit,
    percent = progress.percent,
    completedAt = completedAt,
    userNotes = userNotes,
    extraJson = metadata?.let(::metadataToJson)
)

fun UserGoalEntity.toDomain(): UserGoal = UserGoal(
    goalId = goalId,
    title = title,
    description = description,
    kind = kind,
    exerciseName = exerciseName,
    targetValue = targetValue,
    secondaryValue = secondaryValue,
    windowDays = windowDays,
    deadlineAt = deadlineAt,
    createdAt = createdAt
)

fun UserGoal.toEntity(): UserGoalEntity = UserGoalEntity(
    goalId = goalId,
    title = title,
    description = description,
    kind = kind,
    exerciseName = exerciseName,
    targetValue = targetValue,
    secondaryValue = secondaryValue,
    windowDays = windowDays,
    deadlineAt = deadlineAt,
    createdAt = createdAt
)

private fun parseMetadata(raw: String): AchievementMetadata? = runCatching {
    val json = JSONObject(raw)
    AchievementMetadata(
        exerciseName = json.optString("exerciseName").takeIf { it.isNotBlank() },
        exerciseId = json.optString("exerciseId").takeIf { it.isNotBlank() },
        deadlineAt = json.optLong("deadlineAt").takeIf { it != 0L }?.let(Instant::fromEpochMilliseconds),
        secondaryTarget = json.optDouble("secondaryTarget").takeIf { !json.isNull("secondaryTarget") },
        windowDays = if (json.isNull("windowDays")) null else json.getInt("windowDays")
    )
}.getOrNull()

private fun metadataToJson(metadata: AchievementMetadata): String {
    val json = JSONObject()
    metadata.exerciseName?.let { json.put("exerciseName", it) }
    metadata.exerciseId?.let { json.put("exerciseId", it) }
    metadata.deadlineAt?.let { json.put("deadlineAt", it.toEpochMilliseconds()) }
    metadata.secondaryTarget?.let { json.put("secondaryTarget", it) }
    metadata.windowDays?.let { json.put("windowDays", it) }
    return json.toString()
}
