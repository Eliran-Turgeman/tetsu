package com.example.workoutlogger.feature.share

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.workoutlogger.feature.share.data.AccentColor
import com.example.workoutlogger.feature.share.data.AchievementSelection
import com.example.workoutlogger.feature.share.data.AspectPreset
import com.example.workoutlogger.feature.share.data.DailyCount
import com.example.workoutlogger.feature.share.data.DateRange
import com.example.workoutlogger.feature.share.data.HeatmapExportRequest
import com.example.workoutlogger.feature.share.data.PreparedAchievement
import com.example.workoutlogger.feature.share.data.Theme
import com.example.workoutlogger.feature.share.render.ExportComposer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ExportComposerTest {

    private fun placeholderIcon(): ImageBitmap =
        Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).asImageBitmap()

    @Test
    fun `rendering is deterministic for same input`() {
        val request = HeatmapExportRequest(
            range = DateRange.Last4Weeks,
            weekStart = DayOfWeek.MONDAY,
            theme = Theme.DARK,
            accent = AccentColor.EMERALD,
            aspect = AspectPreset.SQUARE,
            achievements = AchievementSelection.AutoTopN()
        )
        val counts = (0 until 28).map { offset ->
            DailyCount(LocalDate.of(2024, 1, 31).minusDays(offset.toLong()), offset % 6)
        }
        val placeholder = placeholderIcon()
        val achievements = listOf(
            PreparedAchievement("1", "Milestone One", placeholder, LocalDate.of(2024, 1, 10)),
            PreparedAchievement("2", "Milestone Two", placeholder, LocalDate.of(2024, 1, 20)),
            PreparedAchievement("3", "Milestone Three", placeholder, LocalDate.of(2024, 1, 25))
        )
        val first = ExportComposer.render(request, counts, achievements, referenceDate = LocalDate.of(2024, 1, 31))
        val second = ExportComposer.render(request, counts, achievements, referenceDate = LocalDate.of(2024, 1, 31))
        val firstBitmap = first.asAndroidBitmap()
        val secondBitmap = second.asAndroidBitmap()
        assertEquals(firstBitmap.width, secondBitmap.width)
        assertEquals(firstBitmap.height, secondBitmap.height)
        org.junit.Assert.assertTrue(firstBitmap.sameAs(secondBitmap))
    }
}

