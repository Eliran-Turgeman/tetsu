package com.example.workoutlogger.navigation

import androidx.annotation.StringRes
import com.example.workoutlogger.R

sealed class AppDestination(val route: String, @StringRes val labelRes: Int, val showInBottomBar: Boolean = false) {
    data object Dashboard : AppDestination("dashboard", R.string.nav_dashboard, true)
    data object Templates : AppDestination("templates", R.string.nav_templates, true)
    data object Heatmap : AppDestination("heatmap", R.string.nav_heatmap, true)
    data object Settings : AppDestination("settings", R.string.nav_settings, true)

    data object TemplateEditor : AppDestination("templateEditor/{templateId}", R.string.nav_template_editor)
    data object Session : AppDestination("session/{sessionId}", R.string.nav_session)
    data object Schedule : AppDestination("schedule/{templateId}", R.string.nav_schedule)

    companion object {
        val bottomDestinations = listOf(Dashboard, Templates, Heatmap, Settings)

        fun templateEditor(templateId: Long?) = "templateEditor/${templateId ?: "new"}"
        fun session(sessionId: Long) = "session/$sessionId"
        fun schedule(templateId: Long) = "schedule/$templateId"
    }
}
