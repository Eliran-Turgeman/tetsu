package com.example.workoutlogger.domain.model

import kotlinx.datetime.Instant

/**
 * Previous performance data for a particular exercise, used to surface while logging.
 */
data class PreviousPerformance(
    val sessionId: Long,
    val sessionEndedAt: Instant,
    val exerciseName: String,
    val sets: List<SessionSetLog>,
    val bestSet: SessionSetLog?
)
