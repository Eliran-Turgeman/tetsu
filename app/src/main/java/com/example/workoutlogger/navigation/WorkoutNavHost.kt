package com.example.workoutlogger.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.workoutlogger.ui.screens.dashboard.DashboardRoute
import com.example.workoutlogger.ui.screens.achievements.AchievementsRoute
import com.example.workoutlogger.ui.screens.schedule.ScheduleRoute
import com.example.workoutlogger.ui.screens.session.SessionRoute
import com.example.workoutlogger.ui.screens.settings.SettingsRoute
import com.example.workoutlogger.ui.screens.workouts.WorkoutEditorRoute
import com.example.workoutlogger.ui.screens.workouts.WorkoutListRoute

@Composable
fun WorkoutNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    pendingStartWorkoutId: Long? = null,
    onConsumedPendingStart: () -> Unit = {}
) {
    fun NavHostController.navigateBottom(route: String) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.Dashboard.route,
        modifier = modifier
    ) {
        composable(AppDestination.Dashboard.route) {
            DashboardRoute(
                onCreateWorkout = {
                    navController.navigate(AppDestination.templateEditor(null))
                },
                onEditWorkout = { workoutId ->
                    navController.navigate(AppDestination.templateEditor(workoutId))
                },
                onOpenSession = { sessionId ->
                    navController.navigate(AppDestination.session(sessionId))
                },
                onNavigateToSettings = {
                    navController.navigateBottom(AppDestination.Settings.route)
                },
                onViewAllTemplates = {
                    navController.navigateBottom(AppDestination.Templates.route)
                },
                onScheduleWorkout = { workoutId ->
                    navController.navigate(AppDestination.schedule(workoutId))
                }
            )
        }

        composable(AppDestination.Templates.route) {
            WorkoutListRoute(
                onCreateWorkout = {
                    navController.navigate(AppDestination.templateEditor(null))
                },
                onEditWorkout = { workoutId ->
                    navController.navigate(AppDestination.templateEditor(workoutId))
                },
                onScheduleWorkout = { workoutId ->
                    navController.navigate(AppDestination.schedule(workoutId))
                },
                onOpenSession = { sessionId ->
                    navController.navigate(AppDestination.session(sessionId))
                },
                pendingStartWorkoutId = pendingStartWorkoutId,
                onConsumedPendingStart = onConsumedPendingStart
            )
        }

        composable(
            route = AppDestination.Heatmap.route + "?openShare={openShare}",
            arguments = listOf(navArgument("openShare") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStackEntry ->
            val openShare = backStackEntry.arguments?.getBoolean("openShare") ?: false
            AchievementsRoute(
                onOpenSession = { sessionId ->
                    navController.navigate(AppDestination.session(sessionId))
                },
                onStartWorkout = {
                    navController.navigateBottom(AppDestination.Templates.route)
                },
                openShareOnLaunch = openShare
            )
        }

        composable(AppDestination.Settings.route) {
            val context = LocalContext.current
            SettingsRoute(
                onOpenNotificationSettings = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
                onOpenShareConsistency = {
                    navController.navigateBottom(AppDestination.Heatmap.route + "?openShare=true")
                }
            )
        }

        composable(AppDestination.TemplateEditor.route) { backStackEntry ->
            val workoutIdArg = backStackEntry.arguments?.getString("workoutId")
            val workoutId = workoutIdArg?.takeIf { it != "new" }?.toLongOrNull()
            WorkoutEditorRoute(
                workoutId = workoutId,
                onDone = { navController.popBackStack() }
            )
        }

        composable(AppDestination.Session.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toLongOrNull()
            if (sessionId != null) {
                SessionRoute(
                    sessionId = sessionId,
                    onExit = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = AppDestination.Schedule.route,
            arguments = listOf(navArgument("templateId") { type = NavType.LongType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong("templateId")
            if (templateId != null) {
                ScheduleRoute(
                    templateId = templateId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
