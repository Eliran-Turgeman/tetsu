package com.example.workoutlogger.feature.share

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import com.example.workoutlogger.feature.share.data.DailyCount
import com.example.workoutlogger.feature.share.data.HeatmapExportRequest
import com.example.workoutlogger.feature.share.data.ImageFormat
import com.example.workoutlogger.feature.share.data.PreparedAchievement
import com.example.workoutlogger.feature.share.export.ImageExporter
import com.example.workoutlogger.feature.share.render.ExportComposer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShareConsistency {

    suspend fun render(
        request: HeatmapExportRequest,
        dailyCounts: List<DailyCount>,
        achievements: List<PreparedAchievement>,
    ): ImageBitmap = withContext(Dispatchers.Default) {
        ExportComposer.render(request, dailyCounts, achievements)
    }

    suspend fun exportToCache(
        context: Context,
        bitmap: ImageBitmap,
        format: ImageFormat
    ): Uri = withContext(Dispatchers.IO) {
        ImageExporter.exportToCache(context, bitmap, format)
    }

    suspend fun share(
        context: Context,
        uri: Uri,
        mime: String
    ) = withContext(Dispatchers.Main) {
        ImageExporter.share(context, uri, mime)
    }
}

