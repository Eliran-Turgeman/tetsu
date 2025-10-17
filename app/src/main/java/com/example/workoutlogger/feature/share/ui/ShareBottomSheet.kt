package com.example.workoutlogger.feature.share.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.workoutlogger.feature.share.data.AccentColor
import com.example.workoutlogger.feature.share.data.AchievementSelection
import com.example.workoutlogger.feature.share.data.AspectPreset
import com.example.workoutlogger.feature.share.data.DateRange
import com.example.workoutlogger.feature.share.data.HeatmapExportRequest
import com.example.workoutlogger.feature.share.data.Theme
import java.time.DayOfWeek

@Composable
fun ShareBottomSheet(
    state: ShareSheetState,
    onStateChange: (ShareSheetState) -> Unit,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false,
    shareEnabled: Boolean = true
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Share my consistency",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "No account. No identifiers. Only your activity and badges.",
            style = MaterialTheme.typography.bodySmall
        )

        OptionSection(
            title = "Range",
            options = listOf(
                OptionItem("Last 4 weeks", DateRange.Last4Weeks),
                OptionItem("Last 12 weeks", DateRange.Last12Weeks),
                OptionItem("YTD", DateRange.YTD),
                OptionItem("Last 365 days", DateRange.Last365)
            ),
            selected = state.range,
            onSelect = { onStateChange(state.copy(range = it)) }
        )

        OptionSection(
            title = "Theme",
            options = Theme.entries.map { OptionItem(it.name.lowercase().replaceFirstChar { c -> c.titlecase() }, it) },
            selected = state.theme,
            onSelect = { onStateChange(state.copy(theme = it)) }
        )

        OptionSection(
            title = "Accent",
            options = AccentColor.entries.map { OptionItem(it.displayName, it) },
            selected = state.accent,
            onSelect = { onStateChange(state.copy(accent = it)) }
        )

        OptionSection(
            title = "Aspect",
            options = AspectPreset.entries.map { OptionItem(it.name, it) },
            selected = state.aspect,
            onSelect = { onStateChange(state.copy(aspect = it)) }
        )

        OptionSection(
            title = "Achievements",
            options = listOf(
                OptionItem("Auto top 3", AchievementSelection.AutoTopN()),
                OptionItem("Manual", AchievementSelection.Manual(emptyList()))
            ),
            selected = state.selection,
            onSelect = { onStateChange(state.copy(selection = it)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPreview,
                modifier = Modifier.weight(1f),
                enabled = !isProcessing
            ) {
                Text("Preview")
            }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                enabled = shareEnabled && !isProcessing
            ) {
                Text("Share")
            }
        }
    }
}

data class ShareSheetState(
    val range: DateRange = DateRange.Last12Weeks,
    val theme: Theme = Theme.DARK,
    val accent: AccentColor = AccentColor.EMERALD,
    val aspect: AspectPreset = AspectPreset.SQUARE,
    val selection: AchievementSelection = AchievementSelection.AutoTopN()
) {
    fun toRequest(): HeatmapExportRequest = HeatmapExportRequest(
        range = range,
        weekStart = DayOfWeek.MONDAY,
        theme = theme,
        accent = accent,
        aspect = aspect,
        achievements = selection
    )
}

private data class OptionItem<T>(val label: String, val value: T)

@Composable
private fun <T> OptionSection(
    title: String,
    options: List<OptionItem<T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option.value == selected,
                    onClick = { onSelect(option.value) },
                    label = { Text(option.label) },
                    modifier = Modifier
                )
            }
        }
    }
}

@Preview
@Composable
private fun ShareBottomSheetPreview() {
    var state by remember { mutableStateOf(ShareSheetState()) }
    ShareBottomSheet(
        state = state,
        onStateChange = { state = it },
        onPreview = {},
        onShare = {},
        modifier = Modifier.fillMaxWidth()
    )
}

