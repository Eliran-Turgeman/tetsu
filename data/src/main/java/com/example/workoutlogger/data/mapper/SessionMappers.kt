package com.example.workoutlogger.data.mapper

import com.example.workoutlogger.data.db.entity.SessionExerciseEntity
import com.example.workoutlogger.data.db.entity.SessionExerciseWithSets
import com.example.workoutlogger.data.db.entity.SessionSetLogEntity
import com.example.workoutlogger.data.db.entity.SessionStatus
import com.example.workoutlogger.data.db.entity.SessionWithExercises
import com.example.workoutlogger.data.db.entity.WeightUnit as EntityWeightUnit
import com.example.workoutlogger.data.db.entity.WorkoutSessionEntity
import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WorkoutStatus

fun SessionWithExercises.toDomain(): WorkoutSession {
    return WorkoutSession(
        id = session.id,
        workoutId = session.workoutId,
        workoutNameSnapshot = session.workoutNameSnapshot,
        startedAt = session.startedAt,
        endedAt = session.endedAt,
        status = session.status.toDomain(),
        exercises = exercises
            .sortedBy { it.exercise.position }
            .map { it.toDomain() }
    )
}

fun SessionExerciseWithSets.toDomain(): SessionExercise {
    return SessionExercise(
        id = exercise.id,
        sessionId = exercise.sessionId,
        position = exercise.position,
        supersetGroupId = exercise.supersetGroupId,
        exerciseName = exercise.exerciseName,
        sets = sets
            .sortedBy { it.setIndex }
            .map { it.toDomain() }
    )
}

fun SessionSetLogEntity.toDomain(): SessionSetLog {
    return SessionSetLog(
        id = id,
        sessionExerciseId = sessionExerciseId,
        setIndex = setIndex,
        targetRepsMin = targetRepsMin,
        targetRepsMax = targetRepsMax,
        loggedReps = loggedReps,
        loggedWeight = loggedWeight,
        unit = unit.toDomain(),
        note = note
    )
}

fun SessionExercise.toEntity(sessionId: Long): SessionExerciseEntity {
    return SessionExerciseEntity(
        id = id ?: 0,
        sessionId = sessionId,
        position = position,
        supersetGroupId = supersetGroupId,
        exerciseName = exerciseName
    )
}

fun SessionSetLog.toEntity(exerciseId: Long): SessionSetLogEntity {
    return SessionSetLogEntity(
        id = id ?: 0,
        sessionExerciseId = exerciseId,
        setIndex = setIndex,
        targetRepsMin = targetRepsMin,
        targetRepsMax = targetRepsMax,
        loggedReps = loggedReps,
        loggedWeight = loggedWeight,
        unit = unit.toEntity(),
        note = note
    )
}

fun WorkoutSession.toEntity(): WorkoutSessionEntity {
    return WorkoutSessionEntity(
        id = id ?: 0,
        workoutId = workoutId,
        workoutNameSnapshot = workoutNameSnapshot,
        startedAt = startedAt,
        endedAt = endedAt,
        status = status.toEntity()
    )
}

fun SessionStatus.toDomain(): WorkoutStatus = when (this) {
    SessionStatus.ACTIVE -> WorkoutStatus.ACTIVE
    SessionStatus.COMPLETED -> WorkoutStatus.COMPLETED
    SessionStatus.CANCELLED -> WorkoutStatus.CANCELLED
}

fun WorkoutStatus.toEntity(): SessionStatus = when (this) {
    WorkoutStatus.ACTIVE -> SessionStatus.ACTIVE
    WorkoutStatus.COMPLETED -> SessionStatus.COMPLETED
    WorkoutStatus.CANCELLED -> SessionStatus.CANCELLED
}

fun EntityWeightUnit.toDomain(): WeightUnit = when (this) {
    EntityWeightUnit.KG -> WeightUnit.KG
    EntityWeightUnit.LB -> WeightUnit.LB
}

fun WeightUnit.toEntity(): EntityWeightUnit = when (this) {
    WeightUnit.KG -> EntityWeightUnit.KG
    WeightUnit.LB -> EntityWeightUnit.LB
}
