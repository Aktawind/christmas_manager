package com.aktawind.monnoel.feature.traditions.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.TraditionCategory
import com.aktawind.monnoel.core.model.TraditionPeriod
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.EditorScaffold
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.SectionTitle
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.feature.traditions.R
import com.aktawind.monnoel.feature.traditions.emoji
import com.aktawind.monnoel.feature.traditions.label

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TraditionEditRoute(onDone: () -> Unit, viewModel: TraditionEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    val form = state.form
    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.tradition_new_title else R.string.tradition_edit_title),
        onBack = onDone,
        onSave = { viewModel.save() },
        saveEnabled = form.name.isNotBlank(),
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = stringResource(R.string.tradition_delete_confirm),
        onDelete = if (state.isNew) null else ({ viewModel.delete() }),
    ) {
        TextInput(form.name, { v ->
            viewModel.update { copy(name = v) }
        }, stringResource(R.string.field_tradition_name), Modifier.fillMaxWidth())
        PhotoField(path = form.photoPath, onPicked = viewModel::importPhoto, onRemove = {
            viewModel.update { copy(photoPath = null) }
        }, placeholder = form.category.emoji)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DropdownField(
                label = stringResource(R.string.field_tradition_category),
                selected = form.category,
                options = TraditionCategory.entries,
                optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
                onSelect = { c -> viewModel.update { copy(category = c) } },
                modifier = Modifier.weight(1f),
            )
            DropdownField(
                label = stringResource(R.string.field_tradition_period),
                selected = form.period,
                options = TraditionPeriod.entries,
                optionLabel = { stringResource(it.label) },
                onSelect = { p -> viewModel.update { copy(period = p) } },
                modifier = Modifier.weight(1f),
            )
        }
        if (state.persons.isNotEmpty()) {
            Text(stringResource(R.string.field_tradition_persons), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.persons.forEach { p ->
                    FilterChip(selected = p.id in form.personIds, onClick = {
                        viewModel.togglePerson(p.id)
                    }, label = { Text(p.displayName) })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.field_tradition_favorite),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = form.favorite, onCheckedChange = { v -> viewModel.update { copy(favorite = v) } })
        }
        TextInput(
            form.notes,
            { v -> viewModel.update { copy(notes = v) } },
            stringResource(R.string.field_notes),
            Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 3,
        )
        if (state.history.isNotEmpty()) {
            SectionTitle(stringResource(R.string.tradition_history))
            state.history.forEach { (year, note) ->
                Text(
                    stringResource(R.string.tradition_history_line, year) + note.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty(),
                    color = LocalMonNoelColors.current.secondaryText,
                )
            }
        }
    }
}
