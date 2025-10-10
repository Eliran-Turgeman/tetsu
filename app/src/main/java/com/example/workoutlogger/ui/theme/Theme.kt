package com.example.workoutlogger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TetsuGreen,
    onPrimary = Color(0xFF082812),
    primaryContainer = TetsuGreenContainer,
    onPrimaryContainer = TetsuGreenOnContainer,
    secondary = OnSurfaceDim,
    onSecondary = Surface0,
    secondaryContainer = Surface3,
    onSecondaryContainer = OnSurface,
    tertiary = Color(0xFF4AC2FF),
    onTertiary = Surface0,
    tertiaryContainer = Color(0xFF102F42),
    onTertiaryContainer = Color(0xFFCCE9FF),
    background = Surface0,
    onBackground = OnSurface,
    surface = Surface1,
    surfaceVariant = Surface2,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceDim,
    outline = Outline,
    outlineVariant = Outline.copy(alpha = 0.6f),
    inverseSurface = OnSurface,
    inverseOnSurface = Surface1,
    surfaceTint = TetsuGreen,
    scrim = Shadow.copy(alpha = 0.7f),
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = Color(0xFF1F2937),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E9F2),
    onSecondaryContainer = Color(0xFF111A2A),
    tertiary = Color(0xFF2264C7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9E5FF),
    onTertiaryContainer = Color(0xFF041C38),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    inverseSurface = Color(0xFF1D2530),
    inverseOnSurface = Color(0xFFF0F3F8),
    surfaceTint = LightPrimary,
    scrim = Shadow.copy(alpha = 0.5f),
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun WorkoutLoggerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = WorkoutTypography,
        shapes = WorkoutShapes,
        content = content
    )
}
