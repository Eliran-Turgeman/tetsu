package com.example.workoutlogger.feature.share.data

import androidx.compose.ui.graphics.ImageBitmap
import java.time.DayOfWeek
import java.time.LocalDate

/** Simple count representation for a day in the heatmap. */
data class DailyCount(val date: LocalDate, val count: Int)

sealed interface AchievementSelection {
    data class AutoTopN(val n: Int = 3, val includeInProgress: Boolean = false) : AchievementSelection
    data class Manual(val ids: List<String>) : AchievementSelection
}

data class PreparedAchievement(
    val id: String,
    val name: String,
    val icon: ImageBitmap,
    val achievedAt: LocalDate?,
    val tier: Int? = null,
    val category: String? = null,
    val inProgressText: String? = null
)

enum class Theme { DARK, LIGHT, HIGH_CONTRAST }

enum class AspectPreset(val width: Int, val height: Int) {
    SQUARE(1080, 1080),
    PORTRAIT(1080, 1350),
    LANDSCAPE(1920, 1080),
    OG(1200, 630);

    val size get() = width to height
}

sealed interface DateRange {
    data object Last4Weeks : DateRange
    data object Last12Weeks : DateRange
    data object YTD : DateRange
    data object Last365 : DateRange
    data class Custom(val start: LocalDate, val endInclusive: LocalDate) : DateRange
}

/**
 * Request payload describing how to render the heatmap export.
 */
data class HeatmapExportRequest(
    val range: DateRange,
    val weekStart: DayOfWeek,
    val theme: Theme,
    val accent: AccentColor,
    val aspect: AspectPreset,
    val bucketThresholds: IntArray = intArrayOf(0, 1, 3, 5, 6),
    val achievements: AchievementSelection
)

enum class ImageFormat {
    PNG,
    JPEG;

    fun mimeType(): String = when (this) {
        PNG -> "image/png"
        JPEG -> "image/jpeg"
    }
}

