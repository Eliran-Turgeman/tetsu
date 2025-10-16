package com.example.workoutlogger.feature.share.render

import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.workoutlogger.feature.share.data.PreparedAchievement
import com.example.workoutlogger.feature.share.data.truncateName
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

enum class AchievementLayout { GRID, ROW }

data class AchievementRenderConfig(
    val achievements: List<PreparedAchievement>,
    val layout: AchievementLayout,
    val titlePaint: TextPaint,
    val subtitlePaint: TextPaint,
    val iconBackground: Color,
    val badgeStroke: Color = Color.Transparent,
    val maxColumns: Int = 3
)

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

fun DrawScope.drawAchievements(area: Rect, config: AchievementRenderConfig) {
    if (config.achievements.isEmpty()) return

    val spacing = 16.dp.toPx()
    val columns = when (config.layout) {
        AchievementLayout.GRID -> config.maxColumns
        AchievementLayout.ROW -> minOf(config.achievements.size, 6)
    }.coerceAtLeast(1)
    val rows = ceil(config.achievements.size / columns.toFloat()).toInt().coerceAtLeast(1)

    val cellWidth = (area.width - spacing * (columns - 1)) / columns
    val cellHeight = (area.height - spacing * (rows - 1)) / rows
    val iconSize = minOf(cellWidth, cellHeight) * 0.4f
    val iconRadius = iconSize / 2f

    config.achievements.forEachIndexed { index, achievement ->
        val column = index % columns
        val row = index / columns
        val left = area.left + column * (cellWidth + spacing)
        val top = area.top + row * (cellHeight + spacing)

        val centerX = left + iconRadius + 8.dp.toPx()
        val iconCenterY = top + iconRadius + 8.dp.toPx()

        // Icon background circle
        drawCircle(
            color = config.iconBackground,
            radius = iconRadius,
            center = Offset(centerX, iconCenterY)
        )
        if (config.badgeStroke != Color.Transparent) {
            drawCircle(
                color = config.badgeStroke,
                radius = iconRadius,
                center = Offset(centerX, iconCenterY),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        val iconLeft = centerX - iconRadius
        val iconTop = iconCenterY - iconRadius
        val iconSizePx = Size(iconSize, iconSize)
        drawImage(
            image = achievement.icon,
            topLeft = Offset(iconLeft, iconTop),
            dstSize = IntSize(iconSizePx.width.toInt(), iconSizePx.height.toInt())
        )

        val title = truncateName(achievement.name)
        val subtitle = achievement.achievedAt?.format(DATE_FORMATTER) ?: achievement.inProgressText
        val textStartX = centerX + iconRadius + 16.dp.toPx()
        val titleBaseline = iconCenterY - config.titlePaint.textSize / 2f
        drawContext.canvas.nativeCanvas.drawText(title, textStartX, titleBaseline, config.titlePaint)
        subtitle?.let {
            val subtitleBaseline = titleBaseline + config.subtitlePaint.textSize * 1.4f
            drawContext.canvas.nativeCanvas.drawText(it, textStartX, subtitleBaseline, config.subtitlePaint)
        }
    }
}

