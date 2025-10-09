package com.example.workoutlogger

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

const val EXTRA_TEMPLATE_ID = "extra_template_id"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val startTemplateFlow = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startTemplateFlow.value = extractTemplateId(intent)
        setContent {
            val templateId by startTemplateFlow.collectAsState()
            WorkoutLoggerAppRoot(startWorkoutId = templateId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        startTemplateFlow.value = extractTemplateId(intent)
    }

    private fun extractTemplateId(intent: Intent?): Long? {
        val value = intent?.getLongExtra(EXTRA_TEMPLATE_ID, -1L) ?: -1L
        return value.takeIf { it > 0 }
    }
}
