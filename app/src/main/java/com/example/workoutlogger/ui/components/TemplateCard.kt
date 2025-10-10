package com.example.workoutlogger.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.workoutlogger.R

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TemplateCard(
    template: TemplateUi,
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onSchedule: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.animateContentSize(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 0.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                template.meta?.let { meta ->
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                template.secondaryMeta?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PrimaryButton(
                text = stringResource(id = R.string.action_start_workout),
                icon = Icons.Rounded.PlayArrow,
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = stringResource(id = R.string.action_edit_template),
                    icon = Icons.Rounded.Edit,
                    onClick = onEdit
                )
                onSchedule?.let {
                    SecondaryButton(
                        text = stringResource(id = R.string.action_open_schedule),
                        icon = Icons.Rounded.Schedule,
                        onClick = it
                    )
                }
                onDelete?.let {
                    DangerTextButton(
                        text = stringResource(id = R.string.action_delete),
                        dialogTitle = stringResource(id = R.string.dialog_delete_template_title),
                        dialogMessage = stringResource(id = R.string.dialog_delete_template_message),
                        onConfirm = it
                    )
                }
            }
        }
    }
}

data class TemplateUi(
    val id: Long? = null,
    val name: String,
    val meta: String? = null,
    val secondaryMeta: String? = null
)
