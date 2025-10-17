package com.example.workoutlogger.feature.share.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.example.workoutlogger.feature.share.ShareConsistency
import com.example.workoutlogger.feature.share.data.AccentColor
import com.example.workoutlogger.feature.share.data.AchievementSelection
import com.example.workoutlogger.feature.share.data.AspectPreset
import com.example.workoutlogger.feature.share.data.DailyCount
import com.example.workoutlogger.feature.share.data.DateRange
import com.example.workoutlogger.feature.share.data.HeatmapExportRequest
import com.example.workoutlogger.feature.share.data.PreparedAchievement
import com.example.workoutlogger.feature.share.data.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

@Composable
fun SharePreview(
    request: HeatmapExportRequest,
    dailyCounts: List<DailyCount>,
    achievements: List<PreparedAchievement>,
    modifier: Modifier = Modifier,
    contentDescription: String = "Workout consistency heatmap preview"
) {
    var image by remember(request, dailyCounts, achievements) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(request, dailyCounts, achievements) { mutableStateOf(true) }

    LaunchedEffect(request, dailyCounts, achievements) {
        loading = true
        image = withContext(Dispatchers.Default) {
            ShareConsistency.render(request, dailyCounts, achievements)
        }
        loading = false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(request.aspect.width / request.aspect.height.toFloat())
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading -> CircularProgressIndicator()
            image != null -> Image(bitmap = image!!, contentDescription = contentDescription)
            else -> Text("Preview unavailable", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview
@Composable
private fun SharePreviewPreview() {
    val request = HeatmapExportRequest(
        range = DateRange.Last12Weeks,
        weekStart = java.time.DayOfWeek.MONDAY,
        theme = Theme.DARK,
        accent = AccentColor.EMERALD,
        aspect = AspectPreset.SQUARE,
        achievements = AchievementSelection.AutoTopN()
    )
    SharePreview(
        request = request,
        dailyCounts = sampleDailyCounts(),
        achievements = sampleAchievements(),
        modifier = Modifier.height(240.dp)
    )
}

private fun sampleDailyCounts(): List<DailyCount> {
    val today = LocalDate.now()
    return (0 until 120).map { offset ->
        val date = today.minusDays(offset.toLong())
        DailyCount(date, Random.nextInt(0, 7))
    }
}

private fun sampleAchievements(): List<PreparedAchievement> {
    val today = LocalDate.now()
    val placeholder = ImageBitmap(96, 96)
    return listOf(
        PreparedAchievement("1", "Streak Master", placeholder, today.minusDays(3)),
        PreparedAchievement("2", "Volume Crusher", placeholder, today.minusDays(30)),
        PreparedAchievement("3", "Cardio Captain", placeholder, today.minusDays(60))
    )
}

