package com.example.workoutlogger

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.workoutlogger.navigation.AppDestination
import com.example.workoutlogger.navigation.WorkoutNavHost
import com.example.workoutlogger.ui.state.rememberWorkoutLoggerAppState
import com.example.workoutlogger.ui.theme.WorkoutLoggerTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun WorkoutLoggerAppRoot(startWorkoutId: Long? = null) {
    WorkoutLoggerTheme {
        val appState = rememberWorkoutLoggerAppState()
        var pendingWorkoutId by remember { mutableStateOf(startWorkoutId) }
        val navBackStackEntry by appState.navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val systemUiController = rememberSystemUiController()
        val darkIcons = !isSystemInDarkTheme()

        SideEffect {
            systemUiController.setStatusBarColor(color = Color.Transparent, darkIcons = darkIcons)
            systemUiController.setNavigationBarColor(color = Color.Transparent, darkIcons = darkIcons)
        }

        LaunchedEffect(pendingWorkoutId) {
            if (pendingWorkoutId != null) {
                appState.navigateToBottomDestination(AppDestination.Templates)
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }

        val colorScheme = MaterialTheme.colorScheme
        val isLightTheme = colorScheme.background.luminance() > 0.5f
        val selectedContentColor = if (isLightTheme) {
            colorScheme.onPrimaryContainer
        } else {
            colorScheme.onPrimary
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 0.dp
                ) {
                    AppDestination.bottomDestinations.forEach { destination ->
                        val selected = currentDestination?.route == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { appState.navigateToBottomDestination(destination) },
                            icon = {
                                Icon(
                                    imageVector = when (destination) {
                                        AppDestination.Dashboard -> Icons.Rounded.Home
                                        AppDestination.Templates -> Icons.Rounded.DirectionsRun
                                        AppDestination.Heatmap -> Icons.Rounded.EmojiEvents
                                        AppDestination.Settings -> Icons.Rounded.Settings
                                        else -> Icons.Rounded.Home
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(text = stringResource(id = destination.labelRes)) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = selectedContentColor,
                                selectedTextColor = selectedContentColor,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                WorkoutNavHost(
                    navController = appState.navController,
                    modifier = Modifier.fillMaxSize(),
                    pendingStartWorkoutId = pendingWorkoutId,
                    onConsumedPendingStart = { pendingWorkoutId = null }
                )
            }
        }
    }
}
