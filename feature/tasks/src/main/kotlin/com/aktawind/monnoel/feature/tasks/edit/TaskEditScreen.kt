package com.aktawind.monnoel.feature.tasks.edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.TaskModule
import com.aktawind.monnoel.core.model.TaskStatus
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.EditorScaffold
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.feature.tasks.R

@Composable
fun TaskEditRoute(onDone: () -> Unit, onOpenLink: (LinkType, String) -> Unit, viewModel: TaskEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    val form = state.form
    val enabled = !state.readOnly && !state.loading
    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.task_new_title else R.string.task_edit_title),
        onBack = onDone,
        onSave = if (state.readOnly) {
            null
        } else {
            { viewModel.save() }
        },
        saveEnabled = form.title.isNotBlank(),
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = stringResource(R.string.task_delete_confirm),
        onDelete = if (state.isNew || state.readOnly) {
            null
        } else {
            { viewModel.delete() }
        },
    ) {
        if (state.automatic) {
            Text(
                stringResource(R.string.task_auto_hint),
                style = MaterialTheme.typography.bodySmall,
                color = LocalMonNoelColors.current.secondaryText,
            )
        }
        TextInput(form.title, { v ->
            viewModel.update { copy(title = v) }
        }, stringResource(R.string.field_task_title), Modifier.fillMaxWidth(), enabled = enabled)
        DropdownField(
            label = stringResource(R.string.field_task_status),
            selected = form.status,
            options = TaskStatus.entries,
            optionLabel = { stringResource(it.label) },
            onSelect = { s -> viewModel.update { copy(status = s) } },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownField(
            label = stringResource(R.string.field_task_module),
            selected = form.module,
            options = TaskModule.entries,
            optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
            onSelect = { m -> viewModel.update { copy(module = m) } },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        TextInput(
            form.description,
            { v -> viewModel.update { copy(description = v) } },
            stringResource(R.string.field_task_description),
            Modifier.fillMaxWidth(),
            singleLine = false,
            enabled = enabled,
        )
        TextInput(
            form.notes,
            { v -> viewModel.update { copy(notes = v) } },
            stringResource(R.string.field_task_notes),
            Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 3,
            enabled = enabled,
        )
        val linkType = state.linkType
        val linkId = state.linkId
        if (linkType != null && linkId != null) {
            OutlinedButton(onClick = { onOpenLink(linkType, linkId) }) {
                Text(stringResource(if (linkType == LinkType.GIFT) R.string.task_open_gift else R.string.task_open_calendar))
            }
        }
    }
}
