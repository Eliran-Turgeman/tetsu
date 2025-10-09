package com.example.workoutlogger

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.workoutlogger.ui.theme.md_theme_dark_primary
import com.example.workoutlogger.ui.theme.md_theme_light_primary

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary
)

@Composable
fun WorkoutLoggerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
