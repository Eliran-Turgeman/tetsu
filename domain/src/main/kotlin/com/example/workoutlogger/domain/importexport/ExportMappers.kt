package com.example.workoutlogger.domain.importexport

import com.example.workoutlogger.domain.importexport.model.ExportRow
import com.example.workoutlogger.domain.model.SessionExercise
import com.example.workoutlogger.domain.model.SessionSetLog
import com.example.workoutlogger.domain.model.WeightUnit
import com.example.workoutlogger.domain.model.WorkoutSession
import com.example.workoutlogger.domain.model.WorkoutStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max

private val DEFAULT_TIME_ZONE = TimeZone.currentSystemDefault()

fun WorkoutSession.toExportRows(timeZone: TimeZone = DEFAULT_TIME_ZONE): List<ExportRow> {
    if (status == WorkoutStatus.CANCELLED) return emptyList()
    val workoutDuration = formatDuration(startedAt, endedAt)
    val dateTime = startedAt.toLocalDateTime(timeZone)
    val supersetPartners = buildSupersetLookup(exercises)
    return exercises.sortedBy { it.position }.flatMap { exercise ->
        val partnerNames = exercise.supersetGroupId
            ?.let { supersetPartners[it].orEmpty().filter { name -> name != exercise.exerciseName } }
            ?: emptyList()
        exercise.sets.sortedBy { it.setIndex }.map { set ->
            ExportRow(
                dateTime = dateTime,
                workoutName = workoutNameSnapshot,
                exerciseName = exercise.exerciseName,
                setOrder = set.setIndex + 1,
                weight = set.loggedWeight,
                weightUnit = set.unit.toLabel(),
                reps = set.loggedReps,
                rpe = null,
                distance = null,
                distanceUnit = null,
                seconds = null,
                notes = buildNotes(set, partnerNames),
                workoutNotes = null,
                workoutDuration = workoutDuration
            )
        }
    }
}

private fun buildNotes(set: SessionSetLog, partnerNames: List<String>): String? {
    val fragments = mutableListOf<String>()
    set.note?.takeIf { it.isNotBlank() }?.let { fragments += it }
    if (partnerNames.isNotEmpty()) {
        fragments += "Superset with: ${partnerNames.joinToString()}"
    }
    return fragments.takeIf { it.isNotEmpty() }?.joinToString(separator = " | ")
}

private fun WeightUnit.toLabel(): String = when (this) {
    WeightUnit.KG -> "kg"
    WeightUnit.LB -> "lb"
}

private fun buildSupersetLookup(exercises: List<SessionExercise>): Map<String, List<String>> {
    val grouped = exercises.filter { it.supersetGroupId != null }
        .groupBy { it.supersetGroupId!! }
    return grouped.mapValues { entry ->
        entry.value.map { it.exerciseName }
    }
}

private fun formatDuration(startedAt: Instant, endedAt: Instant?): String? {
    val end = endedAt ?: return null
    val durationSeconds = max(0, (end.toEpochMilliseconds() - startedAt.toEpochMilliseconds()) / 1000)
    if (durationSeconds <= 0) return null
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    val parts = mutableListOf<String>()
    if (hours > 0) parts += "${hours}h"
    if (minutes > 0) parts += "${minutes}m"
    if (seconds > 0 && hours == 0) parts += "${seconds}s"
    return parts.joinToString(" ")
}
