package com.aktawind.monnoel.feature.advent.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.advent.MAX_BOX_COUNT
import com.aktawind.monnoel.core.ui.EditorScaffold
import com.aktawind.monnoel.core.ui.MoneyInput
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.R as UiR
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.feature.advent.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarEditRoute(onSaved: (String, Boolean) -> Unit, onCancel: () -> Unit, viewModel: CalendarEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.doneWithId) { state.doneWithId?.let { onSaved(it, state.isNew) } }
    val form = state.form
    var countText by rememberSaveable(state.loading) { mutableStateOf(form.boxCount.toString()) }
    var confirmShrink by rememberSaveable { mutableStateOf(false) }
    val countValid = countText.toIntOrNull()?.let { it in 1..MAX_BOX_COUNT } == true

    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.calendar_new_title else R.string.calendar_edit_title),
        onBack = onCancel,
        onSave = { if (state.boxesLost > 0) confirmShrink = true else viewModel.save() },
        saveEnabled = form.name.isNotBlank() && countValid,
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = null,
        onDelete = null,
    ) {
        TextInput(form.name, { v ->
            viewModel.update { copy(name = v) }
        }, stringResource(R.string.field_calendar_name), Modifier.fillMaxWidth())
        if (state.persons.isNotEmpty()) {
            Text(stringResource(R.string.field_for_whom), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.persons.forEach { p ->
                    FilterChip(selected = p.id in form.personIds, onClick = {
                        viewModel.togglePerson(p.id)
                    }, label = { Text(p.displayName) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextInput(
                countText,
                { v ->
                    countText = v.filter(Char::isDigit).take(2)
                    countText.toIntOrNull()?.takeIf { it in 1..MAX_BOX_COUNT }?.let { n -> viewModel.update { copy(boxCount = n) } }
                },
                stringResource(R.string.field_box_count),
                Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                isError = !countValid,
            )
            MoneyInput(form.budget, { v ->
                viewModel.update { copy(budget = v) }
            }, stringResource(R.string.field_calendar_budget), Modifier.weight(1f))
        }
        Text(
            stringResource(R.string.field_box_count_hint, MAX_BOX_COUNT),
            style = MaterialTheme.typography.bodySmall,
            color = LocalMonNoelColors.current.secondaryText,
        )
        TextInput(form.theme, { v -> viewModel.update { copy(theme = v) } }, stringResource(R.string.field_theme), Modifier.fillMaxWidth())
        PhotoField(
            path = form.coverPhotoPath,
            onPicked = viewModel::importPhoto,
            onRemove = { viewModel.update { copy(coverPhotoPath = null) } },
            placeholder = "🎄",
        )
        TextInput(
            form.description,
            { v -> viewModel.update { copy(description = v) } },
            stringResource(R.string.field_description),
            Modifier.fillMaxWidth(),
            singleLine = false,
        )
        TextInput(
            form.notes,
            { v -> viewModel.update { copy(notes = v) } },
            stringResource(R.string.field_notes),
            Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 3,
        )
    }

    if (confirmShrink) {
        AlertDialog(
            onDismissRequest = { confirmShrink = false },
            title = { Text(stringResource(R.string.calendar_shrink_title)) },
            text = { Text(stringResource(R.string.calendar_shrink_message, state.boxesLost)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmShrink = false
                    viewModel.save()
                }) { Text(stringResource(R.string.calendar_shrink_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmShrink = false }) { Text(stringResource(UiR.string.action_cancel)) } },
        )
    }
}
