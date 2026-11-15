package com.aktawind.monnoel.feature.cooking.production

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.ProductionStatus
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.EditorScaffold
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.feature.cooking.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductionEditRoute(onDone: () -> Unit, viewModel: ProductionEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    val form = state.form
    val editable = !state.readOnly
    val lessLabel = stringResource(R.string.quantity_less)
    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.production_new_title else R.string.production_edit_title),
        onBack = onDone,
        onSave = if (editable) ({ viewModel.save() }) else null,
        saveEnabled = form.name.isNotBlank() && state.edition != null,
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = stringResource(R.string.production_delete_confirm),
        onDelete = if (state.isNew || !editable) null else ({ viewModel.delete() }),
    ) {
        if (state.readOnly) {
            Text(stringResource(R.string.production_read_only), color = LocalMonNoelColors.current.secondaryText)
        }
        DropdownField(
            label = stringResource(R.string.field_recipe),
            selected = state.recipes.firstOrNull { it.id == form.recipeId },
            options = listOf(null) + state.recipes,
            optionLabel = { it?.name ?: stringResource(R.string.no_recipe) },
            onSelect = viewModel::selectRecipe,
            modifier = Modifier.fillMaxWidth(),
            enabled = editable,
        )
        TextInput(
            form.name,
            { v -> viewModel.update { copy(name = v) } },
            stringResource(R.string.field_production_name),
            Modifier.fillMaxWidth(),
            enabled = editable,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.field_quantity), style = MaterialTheme.typography.titleSmall)
            FilledTonalIconButton(
                onClick = { viewModel.update { copy(quantity = (quantity - 1).coerceAtLeast(1)) } },
                enabled = editable && form.quantity > 1,
            ) { Text("−", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { contentDescription = lessLabel }) }
            Text("${form.quantity}", style = MaterialTheme.typography.titleLarge)
            FilledTonalIconButton(
                onClick = { viewModel.update { copy(quantity = (quantity + 1).coerceAtMost(999)) } },
                enabled = editable,
            ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.quantity_more)) }
        }
        TextInput(
            form.destination,
            { v -> viewModel.update { copy(destination = v) } },
            stringResource(R.string.field_destination),
            Modifier.fillMaxWidth(),
            enabled = editable,
        )
        if (editable) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                stringArrayResource(R.array.destination_shortcuts).forEach { shortcut ->
                    AssistChip(onClick = { viewModel.update { copy(destination = shortcut) } }, label = { Text(shortcut) })
                }
            }
        }
        DropdownField(
            label = stringResource(R.string.field_status),
            selected = form.status,
            options = ProductionStatus.entries,
            optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
            onSelect = { s -> viewModel.update { copy(status = s) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = editable,
        )
        Text(stringResource(R.string.field_result_photo), style = MaterialTheme.typography.titleSmall)
        if (form.photoPath == null && state.recipePhotoPath != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoImage(state.recipePhotoPath, null, size = 56.dp, placeholder = "🧁")
                Text(
                    stringResource(R.string.production_recipe_photo_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                )
            }
        }
        PhotoField(
            path = form.photoPath,
            onPicked = viewModel::importPhoto,
            onRemove = { viewModel.update { copy(photoPath = null) } },
            enabled = editable,
            placeholder = "📸",
        )
        TextInput(
            form.notes,
            { v -> viewModel.update { copy(notes = v) } },
            stringResource(R.string.field_notes),
            Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 3,
            enabled = editable,
        )
    }
}
