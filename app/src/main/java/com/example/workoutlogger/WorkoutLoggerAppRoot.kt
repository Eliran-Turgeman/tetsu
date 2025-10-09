package com.example.workoutlogger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Square
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.workoutlogger.navigation.AppDestination
import com.example.workoutlogger.navigation.WorkoutNavHost
import com.example.workoutlogger.ui.state.rememberWorkoutLoggerAppState
import com.example.workoutlogger.ui.theme.playful_gradient_end
import com.example.workoutlogger.ui.theme.playful_gradient_start

@Composable
fun WorkoutLoggerAppRoot(startWorkoutId: Long? = null) {
    WorkoutLoggerTheme {
        val appState = rememberWorkoutLoggerAppState()
        var pendingWorkoutId by remember { mutableStateOf(startWorkoutId) }
        val navBackStackEntry by appState.navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        LaunchedEffect(pendingWorkoutId) {
            if (pendingWorkoutId != null) {
                appState.navigateToBottomDestination(AppDestination.Templates)
            }
        }

        val gradient = Brush.verticalGradient(
            colors = listOf(
                playful_gradient_start.copy(alpha = 0.65f),
                MaterialTheme.colorScheme.background,
                playful_gradient_end.copy(alpha = 0.6f)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
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
                                            AppDestination.Dashboard -> Icons.Default.Home
                                            AppDestination.Templates -> Icons.Default.DirectionsRun
                                            AppDestination.Heatmap -> Icons.Default.BarChart
                                            AppDestination.Settings -> Icons.Default.Settings
                                            AppDestination.Schedule -> Icons.Default.Schedule
                                            AppDestination.Session -> Icons.Default.Square
                                            AppDestination.TemplateEditor -> Icons.Default.Edit
                                        },
                                        contentDescription = null
                                    )
                                },
                                label = { Text(text = stringResource(id = destination.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                WorkoutNavHost(
                    navController = appState.navController,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    pendingStartWorkoutId = pendingWorkoutId,
                    onConsumedPendingStart = { pendingWorkoutId = null }
                )
            }
        }
    }
}
