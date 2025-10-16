package com.example.workoutlogger.feature.share.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

/** Representation of a single rendered heatmap cell. */
data class HeatmapCell(
    val date: LocalDate,
    val count: Int,
    val bucket: Int
)

data class HeatmapGrid(
    val columns: List<List<HeatmapCell?>>,
    val monthColumns: Map<Int, YearMonth>
) {
    val columnCount: Int get() = columns.size
    val rowCount: Int get() = 7
}

/** Calculates the inclusive date range for the export request. */
fun resolveRange(range: DateRange, reference: LocalDate = LocalDate.now()): ClosedRange<LocalDate> {
    val end = when (range) {
        is DateRange.Custom -> range.endInclusive
        else -> reference
    }
    val start = when (range) {
        DateRange.Last4Weeks -> end.minusWeeks(4).plusDays(1)
        DateRange.Last12Weeks -> end.minusWeeks(12).plusDays(1)
        DateRange.YTD -> YearMonth.of(end.year, 1).atDay(1)
        DateRange.Last365 -> end.minusDays(364)
        is DateRange.Custom -> range.start
    }
    return start..end
}

/** Generate the ordered week columns covering the requested range. */
fun generateColumns(range: ClosedRange<LocalDate>, weekStart: DayOfWeek): List<List<LocalDate?>> {
    val start = range.start
    val end = range.endInclusive
    val offsetToWeekStart = ((start.dayOfWeek.ordinal - weekStart.ordinal) % 7 + 7) % 7
    val firstCell = start.minusDays(offsetToWeekStart.toLong())
    val offsetToWeekEnd = ((weekStart.ordinal + 6 - end.dayOfWeek.ordinal) % 7 + 7) % 7
    val lastCell = end.plusDays(offsetToWeekEnd.toLong())

    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(firstCell, lastCell) + 1
    val columnCount = ceil(totalDays / 7.0).toInt()

    return (0 until columnCount).map { columnIndex ->
        (0 until 7).map { row ->
            val current = firstCell.plusDays((columnIndex * 7L) + row)
            if (current in range) current else null
        }
    }
}

fun mapCountsByDate(dailyCounts: List<DailyCount>): Map<LocalDate, Int> = buildMap {
    dailyCounts.forEach { count ->
        put(count.date, (get(count.date) ?: 0) + count.count)
    }
}

fun bucketForCount(count: Int, thresholds: IntArray): Int {
    require(thresholds.size == 5) { "Bucket thresholds must provide five entries" }
    for (index in thresholds.indices.reversed()) {
        if (count >= thresholds[index]) return index
    }
    return 0
}

fun buildHeatmapGrid(
    range: ClosedRange<LocalDate>,
    weekStart: DayOfWeek,
    dailyCounts: List<DailyCount>,
    thresholds: IntArray
): HeatmapGrid {
    val columns = generateColumns(range, weekStart)
    val counts = mapCountsByDate(dailyCounts)
    val columnData = columns.map { column ->
        column.map { date ->
            date?.let {
                val value = counts[it] ?: 0
                HeatmapCell(it, value, bucketForCount(value, thresholds))
            }
        }
    }

    return HeatmapGrid(columnData, monthLabels(columnData))
}

private fun monthLabels(columns: List<List<HeatmapCell?>>): Map<Int, YearMonth> {
    val labels = mutableMapOf<Int, YearMonth>()
    var previousMonth: YearMonth? = null
    columns.forEachIndexed { index, column ->
        val firstCell = column.firstOrNull { it != null }
        if (firstCell != null) {
            val month = YearMonth.from(firstCell.date)
            if (month != previousMonth) {
                labels[index] = month
                previousMonth = month
            }
        }
    }
    return labels
}

fun dateRangeLabel(range: ClosedRange<LocalDate>): String {
    val start = range.start
    val end = range.endInclusive
    return if (start.year == end.year && start.month == end.month) {
        start.format(java.time.format.DateTimeFormatter.ofPattern("LLL d, yyyy"))
    } else {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("LLL d, yyyy")
        "${start.format(formatter)} — ${end.format(formatter)}"
    }
}

fun weekdayInitials(weekStart: DayOfWeek): List<String> {
    val days = DayOfWeek.values().toList()
    val startIndex = weekStart.ordinal
    return (0 until 7).map { index ->
        val day = days[(startIndex + index) % 7]
        day.name.first().toString()
    }
}

