package com.example.workoutlogger.feature.share.render

import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.drawRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutlogger.feature.share.data.AspectPreset
import com.example.workoutlogger.feature.share.data.DailyCount
import com.example.workoutlogger.feature.share.data.HeatmapExportRequest
import com.example.workoutlogger.feature.share.data.PreparedAchievement
import com.example.workoutlogger.feature.share.data.Theme
import com.example.workoutlogger.feature.share.data.buildHeatmapGrid
import com.example.workoutlogger.feature.share.data.dateRangeLabel
import com.example.workoutlogger.feature.share.data.resolveRange
import com.example.workoutlogger.feature.share.data.selectAchievements
import com.example.workoutlogger.feature.share.render.AchievementLayout.GRID
import com.example.workoutlogger.feature.share.render.AchievementLayout.ROW

object ExportComposer {

    fun render(
        request: HeatmapExportRequest,
        dailyCounts: List<DailyCount>,
        achievements: List<PreparedAchievement>,
        referenceDate: java.time.LocalDate = java.time.LocalDate.now()
    ): ImageBitmap {
        val (width, height) = request.aspect.size
        val bitmap = ImageBitmap(width, height)
        val canvas = Canvas(bitmap)
        val density = Density(width / 1080f)
        val drawScope = CanvasDrawScope()
        val range = resolveRange(request.range, referenceDate)
        val grid = buildHeatmapGrid(range, request.weekStart, dailyCounts, request.bucketThresholds)
        val selectedAchievements = selectAchievements(request.achievements, achievements, range)

        val title = "tetsu · ${dateRangeLabel(range)}"

        val palette = request.accent.bucketColors(request.theme)
        val background = request.accent.background(request.theme)
        val titleColor = request.accent.titleColor(request.theme)
        val secondaryColor = request.accent.secondaryTextColor(request.theme)
        val strokeColor = request.accent.strokeColor(request.theme)

        val monthPaint = createTextPaint(density, 16f, secondaryColor)
        val weekdayPaint = createTextPaint(density, 14f, secondaryColor)
        val titlePaint = createTextPaint(density, 24f, titleColor, bold = true)
        val subtitlePaint = createTextPaint(density, 14f, secondaryColor)
        val watermarkPaint = createTextPaint(density, 12f, secondaryColor.copy(alpha = 0.4f))

        drawScope.draw(density, LayoutDirection.Ltr, canvas, Size(width.toFloat(), height.toFloat())) {
            drawRect(color = background, size = size)

            val margin = 48.dp.toPx()
            val inner = Rect(margin, margin, size.width - margin, size.height - margin)

            // Title row
            drawContext.canvas.nativeCanvas.drawText(
                title,
                inner.left,
                inner.top - 12.dp.toPx(),
                titlePaint
            )

            val contentTop = inner.top + 12.dp.toPx()
            val contentArea = Rect(inner.left, contentTop, inner.right, inner.bottom - 40.dp.toPx())

            val gap = 24.dp.toPx()
            val isLandscape = request.aspect == AspectPreset.LANDSCAPE || request.aspect == AspectPreset.OG

            val (heatmapArea, achievementArea) = if (selectedAchievements.isNotEmpty()) {
                if (isLandscape) {
                    val heatmapWidth = contentArea.width * 0.58f
                    val heatmapRect = Rect(
                        contentArea.left,
                        contentArea.top,
                        contentArea.left + heatmapWidth,
                        contentArea.bottom
                    )
                    val achievementsRect = Rect(
                        heatmapRect.right + gap,
                        contentArea.top,
                        contentArea.right,
                        contentArea.bottom
                    )
                    heatmapRect to achievementsRect
                } else {
                    val heatmapHeight = contentArea.height * 0.55f
                    val heatmapRect = Rect(
                        contentArea.left,
                        contentArea.top,
                        contentArea.right,
                        contentArea.top + heatmapHeight
                    )
                    val achievementsRect = Rect(
                        contentArea.left,
                        heatmapRect.bottom + gap,
                        contentArea.right,
                        contentArea.bottom
                    )
                    heatmapRect to achievementsRect
                }
            } else {
                contentArea to Rect(0f, 0f, 0f, 0f)
            }

            val heatmapConfig = HeatmapRenderConfig(
                grid = grid,
                colors = palette,
                weekStart = request.weekStart,
                showWeekdayInitials = request.theme == Theme.HIGH_CONTRAST,
                monthLabelPaint = monthPaint,
                weekdayPaint = weekdayPaint,
                cellStrokeColor = strokeColor
            )
            drawHeatmap(heatmapArea, heatmapConfig)

            if (selectedAchievements.isNotEmpty() && !achievementArea.isEmpty) {
                val layout = if (isLandscape) ROW else GRID
                val achievementConfig = AchievementRenderConfig(
                    achievements = selectedAchievements,
                    layout = layout,
                    titlePaint = TextPaint(titlePaint),
                    subtitlePaint = subtitlePaint,
                    iconBackground = palette.last(),
                    badgeStroke = strokeColor,
                    maxColumns = if (isLandscape) 6 else 3
                )
                drawAchievements(achievementArea, achievementConfig)
            }

            // Watermark
            val watermark = "tetsu"
            val textWidth = watermarkPaint.measureText(watermark)
            drawContext.canvas.nativeCanvas.drawText(
                watermark,
                size.width - margin - textWidth,
                size.height - margin / 2f,
                watermarkPaint
            )
        }

        return bitmap
    }

    private fun createTextPaint(
        density: Density,
        textSizeSp: Float,
        color: Color,
        bold: Boolean = false
    ): TextPaint = TextPaint().apply {
        isAntiAlias = true
        density.run { textSize = textSizeSp.sp.toPx() }
        this.color = color.toArgb()
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }
}

