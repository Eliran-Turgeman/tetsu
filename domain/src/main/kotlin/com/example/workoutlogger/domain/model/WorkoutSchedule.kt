package com.example.workoutlogger.domain.model

import kotlinx.datetime.DayOfWeek

/**
 * Represents a reminder schedule attached to a workout definition.
 */
data class WorkoutSchedule(
    val id: Long? = null,
    val workoutId: Long,
    val daysOfWeek: Set<DayOfWeek>,
    val notifyHour: Int,
    val notifyMinute: Int,
    val enabled: Boolean
)

// Alias for legacy naming.
typealias TemplateSchedule = WorkoutSchedule
