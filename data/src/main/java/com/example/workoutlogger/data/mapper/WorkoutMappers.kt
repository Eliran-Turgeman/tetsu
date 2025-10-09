package com.example.workoutlogger.data.mapper

import com.example.workoutlogger.data.db.entity.WorkoutItemEntity
import com.example.workoutlogger.data.db.entity.WorkoutItemType as EntityWorkoutItemType
import com.example.workoutlogger.data.db.entity.WorkoutWithItems
import com.example.workoutlogger.data.db.entity.WorkoutScheduleEntity
import com.example.workoutlogger.data.db.entity.WorkoutEntity
import com.example.workoutlogger.domain.model.WorkoutItem
import com.example.workoutlogger.domain.model.WorkoutItemType
import com.example.workoutlogger.domain.model.WorkoutSchedule
import com.example.workoutlogger.domain.model.Workout
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant

fun WorkoutEntity.toDomain(items: List<WorkoutItemEntity>): Workout {
    return Workout(
        id = id,
        name = name,
        createdAt = createdAt,
        items = items.sortedBy { it.position }.map { it.toDomain() }
    )
}

fun WorkoutItemEntity.toDomain(): WorkoutItem {
    return WorkoutItem(
        id = id,
        workoutId = workoutId,
        position = position,
        type = when (type) {
            EntityWorkoutItemType.EXERCISE -> WorkoutItemType.EXERCISE
            EntityWorkoutItemType.SUPERSET_HEADER -> WorkoutItemType.SUPERSET_HEADER
        },
        supersetGroupId = supersetGroupId,
        exerciseName = exerciseName,
        sets = sets,
        repsMin = repsMin,
        repsMax = repsMax
    )
}

fun WorkoutItem.toEntity(workoutId: Long): WorkoutItemEntity {
    return WorkoutItemEntity(
        id = id ?: 0,
        workoutId = workoutId,
        position = position,
        type = when (type) {
            WorkoutItemType.EXERCISE -> EntityWorkoutItemType.EXERCISE
            WorkoutItemType.SUPERSET_HEADER -> EntityWorkoutItemType.SUPERSET_HEADER
        },
        supersetGroupId = supersetGroupId,
        exerciseName = exerciseName,
        sets = sets,
        repsMin = repsMin,
        repsMax = repsMax
    )
}

fun Workout.toEntity(): WorkoutEntity {
    return WorkoutEntity(
        id = id ?: 0,
        name = name,
        createdAt = createdAt
    )
}

fun WorkoutWithItems.toDomain(): Workout = workout.toDomain(items)

fun WorkoutScheduleEntity.toDomain(): WorkoutSchedule {
    val days = if (daysOfWeek.isBlank()) emptySet() else daysOfWeek.split(',')
        .filter { it.isNotBlank() }
        .map { DayOfWeek.valueOf(it) }
        .toSet()

    return WorkoutSchedule(
        id = id,
        workoutId = workoutId,
        daysOfWeek = days,
        notifyHour = notifyHour,
        notifyMinute = notifyMinute,
        enabled = enabled
    )
}

fun WorkoutSchedule.toEntity(): WorkoutScheduleEntity {
    return WorkoutScheduleEntity(
        id = id ?: 0,
        workoutId = workoutId,
        daysOfWeek = daysOfWeek.joinToString(separator = ",") { it.name },
        notifyHour = notifyHour,
        notifyMinute = notifyMinute,
        enabled = enabled
    )
}
