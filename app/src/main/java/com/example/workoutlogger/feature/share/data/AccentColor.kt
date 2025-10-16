package com.example.workoutlogger.feature.share.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Brand accent palette with helper utilities for bucket intensities. */
enum class AccentColor(val displayName: String, private val base: Color) {
    EMERALD("Emerald", Color(0xFF34D399)),
    BLUE("Blue", Color(0xFF60A5FA)),
    PURPLE("Purple", Color(0xFFA855F7)),
    ORANGE("Orange", Color(0xFFF97316));

    fun bucketColors(theme: Theme): List<Color> {
        val inactive = when (theme) {
            Theme.DARK -> Color(0xFF1A1D23)
            Theme.LIGHT -> Color(0xFFE9EEF5)
            Theme.HIGH_CONTRAST -> Color(0xFFFFFFFF)
        }
        val baseTarget = when (theme) {
            Theme.HIGH_CONTRAST -> base.copy(alpha = 1f)
            else -> base
        }

        val steps = listOf(0.2f, 0.4f, 0.6f, 0.8f)
        val active = steps.map { fraction -> lerp(inactive, baseTarget, fraction) }
        return listOf(inactive) + active
    }

    fun background(theme: Theme): Color = when (theme) {
        Theme.DARK -> Color(0xFF0D0F12)
        Theme.LIGHT -> Color.White
        Theme.HIGH_CONTRAST -> Color.Black
    }

    fun titleColor(theme: Theme): Color = when (theme) {
        Theme.DARK -> Color(0xFFE0E3EA)
        Theme.LIGHT -> Color(0xFF111827)
        Theme.HIGH_CONTRAST -> Color.White
    }

    fun secondaryTextColor(theme: Theme): Color = when (theme) {
        Theme.DARK -> Color(0xFFA7ADBA)
        Theme.LIGHT -> Color(0xFF4B5563)
        Theme.HIGH_CONTRAST -> Color(0xFFE5E5E5)
    }

    fun strokeColor(theme: Theme): Color = when (theme) {
        Theme.DARK -> Color(0x3321272F)
        Theme.LIGHT -> Color(0x33CBD5E1)
        Theme.HIGH_CONTRAST -> Color(0xFFFFFFFF)
    }
}

