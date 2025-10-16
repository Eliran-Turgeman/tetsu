package com.example.workoutlogger.feature.share.render

import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.workoutlogger.feature.share.data.HeatmapGrid
import com.example.workoutlogger.feature.share.data.weekdayInitials
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Configuration data for the heatmap renderer. */
data class HeatmapRenderConfig(
    val grid: HeatmapGrid,
    val colors: List<Color>,
    val weekStart: DayOfWeek,
    val showWeekdayInitials: Boolean,
    val monthLabelPaint: TextPaint,
    val weekdayPaint: TextPaint,
    val cellStrokeColor: Color
)

fun DrawScope.drawHeatmap(area: Rect, config: HeatmapRenderConfig) {
    val columns = config.grid.columnCount
    val rows = config.grid.rowCount

    val spacing = 4.dp.toPx()
    val monthLabelHeight = config.monthLabelPaint.textSize * 1.2f
    val weekdayWidth = if (config.showWeekdayInitials) config.weekdayPaint.measureText("W") * 1.6f else 0f

    val availableWidth = area.width - weekdayWidth
    val availableHeight = area.height - monthLabelHeight
    if (availableWidth <= 0f || availableHeight <= 0f || columns == 0) return

    val cellWidth = ((availableWidth - (columns - 1) * spacing) / columns).coerceAtLeast(1f)
    val cellHeight = ((availableHeight - (rows - 1) * spacing) / rows).coerceAtLeast(1f)
    val cellSize = minOf(cellWidth, cellHeight)

    val chartWidth = cellSize * columns + spacing * (columns - 1)
    val chartHeight = cellSize * rows + spacing * (rows - 1)

    val startX = area.left + weekdayWidth + (availableWidth - chartWidth) / 2f
    val startY = area.top + monthLabelHeight + (availableHeight - chartHeight) / 2f

    // Draw weekday labels
    if (config.showWeekdayInitials) {
        val initials = weekdayInitials(config.weekStart)
        initials.forEachIndexed { index, initial ->
            val y = startY + index * (cellSize + spacing) + cellSize / 2f + config.weekdayPaint.textSize / 3f
            drawContext.canvas.nativeCanvas.drawText(
                initial,
                area.left + config.weekdayPaint.measureText("W"),
                y,
                config.weekdayPaint
            )
        }
    }

    // Draw month labels at column centers
    config.grid.monthColumns.forEach { (columnIndex, month) ->
        val monthName = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val x = startX + columnIndex * (cellSize + spacing) + cellSize / 2f
        drawContext.canvas.nativeCanvas.drawText(
            monthName,
            x - config.monthLabelPaint.measureText(monthName) / 2f,
            area.top + config.monthLabelPaint.textSize,
            config.monthLabelPaint
        )
    }

    // Draw cells
    config.grid.columns.forEachIndexed { columnIndex, column ->
        column.forEachIndexed { rowIndex, cell ->
            if (cell != null) {
                val color = config.colors.getOrElse(cell.bucket) { config.colors.last() }
                val left = startX + columnIndex * (cellSize + spacing)
                val top = startY + rowIndex * (cellSize + spacing)
                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(cellSize, cellSize)
                )
                if (config.cellStrokeColor.alpha > 0f) {
                drawRect(
                    color = config.cellStrokeColor,
                    topLeft = Offset(left, top),
                    size = Size(cellSize, cellSize),
                    style = Stroke(width = 1.dp.toPx())
                )
                }
            }
        }
    }
}

