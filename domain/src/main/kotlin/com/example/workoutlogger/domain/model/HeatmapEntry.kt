package com.example.workoutlogger.domain.model

import kotlinx.datetime.LocalDate

/**
 * Represents a day entry for the workout heatmap.
 */
data class HeatmapEntry(
    val date: LocalDate,
    val hasCompletedSession: Boolean
)
