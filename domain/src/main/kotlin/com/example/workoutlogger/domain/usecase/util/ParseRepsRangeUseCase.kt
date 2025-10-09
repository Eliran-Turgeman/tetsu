package com.example.workoutlogger.domain.usecase.util

/**
 * Parses a user-supplied reps range string (e.g. "6-8") into integer bounds.
 * Accepts single values ("6") or hyphenated values with optional spaces.
 */
import javax.inject.Inject

class ParseRepsRangeUseCase @Inject constructor() {

    /**
     * @return Pair of minimum and maximum reps, or null when the input is invalid.
     */
    operator fun invoke(raw: String): Pair<Int, Int>? {
        val normalized = raw.trim()
        if (normalized.isEmpty()) return null

        val range = normalized.split('-', limit = 2).map { it.trim() }
        return when (range.size) {
            1 -> range[0].toIntOrNull()?.let { it to it }
            2 -> {
                val min = range[0].toIntOrNull()
                val max = range[1].toIntOrNull()
                if (min == null || max == null || min <= 0 || max <= 0 || min > max) {
                    null
                } else {
                    min to max
                }
            }
            else -> null
        }
    }
}
