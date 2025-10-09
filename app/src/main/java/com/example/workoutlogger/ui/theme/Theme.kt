package com.example.workoutlogger

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.workoutlogger.ui.theme.WorkoutShapes
import com.example.workoutlogger.ui.theme.WorkoutTypography
import com.example.workoutlogger.ui.theme.md_theme_dark_background
import com.example.workoutlogger.ui.theme.md_theme_dark_inverseOnSurface
import com.example.workoutlogger.ui.theme.md_theme_dark_inverseSurface
import com.example.workoutlogger.ui.theme.md_theme_dark_onBackground
import com.example.workoutlogger.ui.theme.md_theme_dark_onPrimary
import com.example.workoutlogger.ui.theme.md_theme_dark_onPrimaryContainer
import com.example.workoutlogger.ui.theme.md_theme_dark_onSecondary
import com.example.workoutlogger.ui.theme.md_theme_dark_onSecondaryContainer
import com.example.workoutlogger.ui.theme.md_theme_dark_onSurface
import com.example.workoutlogger.ui.theme.md_theme_dark_onSurfaceVariant
import com.example.workoutlogger.ui.theme.md_theme_dark_onTertiary
import com.example.workoutlogger.ui.theme.md_theme_dark_onTertiaryContainer
import com.example.workoutlogger.ui.theme.md_theme_dark_outline
import com.example.workoutlogger.ui.theme.md_theme_dark_primary
import com.example.workoutlogger.ui.theme.md_theme_dark_primaryContainer
import com.example.workoutlogger.ui.theme.md_theme_dark_secondary
import com.example.workoutlogger.ui.theme.md_theme_dark_secondaryContainer
import com.example.workoutlogger.ui.theme.md_theme_dark_surface
import com.example.workoutlogger.ui.theme.md_theme_dark_surfaceVariant
import com.example.workoutlogger.ui.theme.md_theme_dark_tertiary
import com.example.workoutlogger.ui.theme.md_theme_dark_tertiaryContainer
import com.example.workoutlogger.ui.theme.md_theme_light_background
import com.example.workoutlogger.ui.theme.md_theme_light_inverseOnSurface
import com.example.workoutlogger.ui.theme.md_theme_light_inverseSurface
import com.example.workoutlogger.ui.theme.md_theme_light_onBackground
import com.example.workoutlogger.ui.theme.md_theme_light_onPrimary
import com.example.workoutlogger.ui.theme.md_theme_light_onPrimaryContainer
import com.example.workoutlogger.ui.theme.md_theme_light_onSecondary
import com.example.workoutlogger.ui.theme.md_theme_light_onSecondaryContainer
import com.example.workoutlogger.ui.theme.md_theme_light_onSurface
import com.example.workoutlogger.ui.theme.md_theme_light_onSurfaceVariant
import com.example.workoutlogger.ui.theme.md_theme_light_onTertiary
import com.example.workoutlogger.ui.theme.md_theme_light_onTertiaryContainer
import com.example.workoutlogger.ui.theme.md_theme_light_outline
import com.example.workoutlogger.ui.theme.md_theme_light_primary
import com.example.workoutlogger.ui.theme.md_theme_light_primaryContainer
import com.example.workoutlogger.ui.theme.md_theme_light_secondary
import com.example.workoutlogger.ui.theme.md_theme_light_secondaryContainer
import com.example.workoutlogger.ui.theme.md_theme_light_surface
import com.example.workoutlogger.ui.theme.md_theme_light_surfaceVariant
import com.example.workoutlogger.ui.theme.md_theme_light_tertiary
import com.example.workoutlogger.ui.theme.md_theme_light_tertiaryContainer

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface
)

@Composable
fun WorkoutLoggerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WorkoutTypography,
        shapes = WorkoutShapes,
        content = content
    )
}
