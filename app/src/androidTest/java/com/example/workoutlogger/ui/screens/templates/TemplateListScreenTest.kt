package com.example.workoutlogger.ui.screens.templates

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.workoutlogger.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shows_empty_state() {
        composeRule.setContent {
            TemplateListScreen(
                templates = emptyList(),
                snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                onCreateTemplate = {},
                onEditTemplate = {},
                onScheduleTemplate = {},
                onStartTemplate = {},
                onDeleteTemplate = {}
            )
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.label_empty_templates))
            .assertIsDisplayed()
    }
}
