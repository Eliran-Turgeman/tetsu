package com.example.workoutlogger.feature.share

import com.example.workoutlogger.feature.share.data.DailyCount
import com.example.workoutlogger.feature.share.data.DateRange
import com.example.workoutlogger.feature.share.data.buildHeatmapGrid
import com.example.workoutlogger.feature.share.data.resolveRange
import com.example.workoutlogger.feature.share.data.bucketForCount
import com.example.workoutlogger.feature.share.data.generateColumns
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class BucketingTest {

    @Test
    fun `bucket mapping respects thresholds`() {
        val thresholds = intArrayOf(0, 1, 3, 5, 6)
        assertEquals(0, bucketForCount(0, thresholds))
        assertEquals(1, bucketForCount(1, thresholds))
        assertEquals(2, bucketForCount(3, thresholds))
        assertEquals(3, bucketForCount(5, thresholds))
        assertEquals(4, bucketForCount(8, thresholds))
    }

    @Test
    fun `last 365 generates 53 columns`() {
        val reference = LocalDate.of(2024, 12, 31)
        val range = resolveRange(DateRange.Last365, reference)
        val columns = generateColumns(range, DayOfWeek.MONDAY)
        assertEquals(53, columns.size)
        assertEquals(7, columns.first().size)
    }

    @Test
    fun `grid buckets align with provided counts`() {
        val reference = LocalDate.of(2024, 1, 31)
        val range = resolveRange(DateRange.Last4Weeks, reference)
        val counts = listOf(
            DailyCount(reference.minusDays(1), 3),
            DailyCount(reference.minusDays(2), 5),
            DailyCount(reference.minusDays(3), 0)
        )
        val grid = buildHeatmapGrid(range, DayOfWeek.MONDAY, counts, intArrayOf(0, 1, 3, 5, 6))
        val flat = grid.columns.flatten().filterNotNull()
        val bucketForThree = flat.first { it.count == 3 }.bucket
        val bucketForFive = flat.first { it.count == 5 }.bucket
        val bucketForZero = flat.first { it.count == 0 }.bucket
        assertEquals(2, bucketForThree)
        assertEquals(3, bucketForFive)
        assertEquals(0, bucketForZero)
    }

    @Test
    fun `month labels start at first column for month`() {
        val reference = LocalDate.of(2024, 3, 31)
        val range = resolveRange(DateRange.Last12Weeks, reference)
        val grid = buildHeatmapGrid(range, DayOfWeek.MONDAY, emptyList(), intArrayOf(0, 1, 3, 5, 6))
        val januaryColumn = grid.monthColumns.values.firstOrNull { it.month.value == 1 }
        val febColumn = grid.monthColumns.values.firstOrNull { it.month.value == 2 }
        assertEquals(1, januaryColumn?.month?.value)
        assertEquals(2, febColumn?.month?.value)
    }
}

