package com.example.workoutlogger.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.workoutlogger.navigation.AppDestination
import kotlinx.coroutines.CoroutineScope

@Stable
class WorkoutLoggerAppState(
    val navController: NavHostController,
    val coroutineScope: CoroutineScope
) {
    val currentDestination: NavDestination?
        @Composable
        get() = navController.currentBackStackEntry?.destination

    fun navigateToBottomDestination(destination: AppDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun rememberWorkoutLoggerAppState(
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): WorkoutLoggerAppState = remember(navController, coroutineScope) {
    WorkoutLoggerAppState(navController, coroutineScope)
}
