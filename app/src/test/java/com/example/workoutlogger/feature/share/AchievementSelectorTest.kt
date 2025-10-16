package com.example.workoutlogger.feature.share

import androidx.compose.ui.graphics.ImageBitmap
import com.example.workoutlogger.feature.share.data.AchievementSelection
import com.example.workoutlogger.feature.share.data.PreparedAchievement
import com.example.workoutlogger.feature.share.data.selectAchievements
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AchievementSelectorTest {

    @Test
    fun `auto selection prioritises recency and diversity`() {
        val baseDate = LocalDate.of(2024, 6, 1)
        val icon = ImageBitmap(32, 32)
        val achievements = listOf(
            PreparedAchievement("a", "Alpha", icon, baseDate.minusDays(1), tier = 1, category = "strength"),
            PreparedAchievement("b", "Bravo", icon, baseDate.minusDays(2), tier = 2, category = "strength"),
            PreparedAchievement("c", "Charlie", icon, baseDate.minusDays(3), tier = 1, category = "mobility"),
            PreparedAchievement("d", "Delta", icon, baseDate.minusDays(4), tier = 3, category = "cardio")
        )
        val range = baseDate.minusDays(10)..baseDate
        val selected = selectAchievements(AchievementSelection.AutoTopN(), achievements, range)
        assertEquals(3, selected.size)
        // Expect top pick to be newest
        assertEquals("a", selected.first().id)
        // Expect diversity to include mobility or cardio despite strength duplicates
        val categories = selected.mapNotNull { it.category }.toSet()
        org.junit.Assert.assertTrue(categories.size >= 2)
    }

    @Test
    fun `manual selection keeps ordering`() {
        val icon = ImageBitmap(32, 32)
        val achievements = listOf(
            PreparedAchievement("a", "Alpha", icon, null),
            PreparedAchievement("b", "Bravo", icon, null),
            PreparedAchievement("c", "Charlie", icon, null)
        )
        val selection = AchievementSelection.Manual(listOf("c", "a"))
        val picked = selectAchievements(selection, achievements, LocalDate.MIN..LocalDate.MAX)
        assertEquals(listOf("c", "a"), picked.map { it.id })
    }
}

