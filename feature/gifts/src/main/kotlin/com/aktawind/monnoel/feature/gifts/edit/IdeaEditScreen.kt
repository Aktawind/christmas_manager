package com.aktawind.monnoel.feature.gifts.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.EditorScaffold
import com.aktawind.monnoel.core.ui.MoneyInput
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.feature.gifts.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdeaEditRoute(onDone: () -> Unit, viewModel: IdeaEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    val form = state.form
    val uriHandler = LocalUriHandler.current

    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.idea_new_title else R.string.idea_edit_title),
        onBack = onDone,
        onSave = { viewModel.save() },
        saveEnabled = form.title.isNotBlank(),
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = stringResource(R.string.idea_delete_confirm),
        onDelete = if (state.isNew) {
            null
        } else {
            { viewModel.delete() }
        },
    ) {
        TextInput(form.title, { v ->
            viewModel.update { copy(title = v) }
        }, stringResource(R.string.field_idea), Modifier.fillMaxWidth())
        MoneyInput(form.price, { v ->
            viewModel.update { copy(price = v) }
        }, stringResource(R.string.field_estimated_price), Modifier.fillMaxWidth())
        DropdownField(
            label = stringResource(R.string.field_category),
            selected = state.categories.firstOrNull { it.id == form.categoryId },
            options = listOf(null) + state.categories,
            optionLabel = { it?.name ?: stringResource(R.string.no_category) },
            onSelect = { c -> viewModel.update { copy(categoryId = c?.id) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.field_for_whom), style = MaterialTheme.typography.titleSmall)
        if (state.persons.isEmpty()) {
            Text(stringResource(R.string.idea_no_persons), color = LocalMonNoelColors.current.secondaryText)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.persons.forEach { person ->
                FilterChip(
                    selected = person.id in form.personIds,
                    onClick = { viewModel.togglePerson(person.id) },
                    label = { Text(person.displayName) },
                )
            }
        }
        TextInput(form.url, { v ->
            viewModel.update { copy(url = v) }
        }, stringResource(R.string.field_url), Modifier.fillMaxWidth(), keyboardType = KeyboardType.Uri)
        if (form.url.isNotBlank()) {
            TextButton(onClick = {
                runCatching { uriHandler.openUri(form.url.withScheme()) }
            }) { Text(stringResource(R.string.open_link)) }
        }
        TextInput(form.shop, { v ->
            viewModel.update { copy(shop = v) }
        }, stringResource(R.string.field_shop), Modifier.fillMaxWidth())
        PhotoField(path = form.photoPath, onPicked = viewModel::importPhoto, onRemove = {
            viewModel.update { copy(photoPath = null) }
        }, placeholder = "💡")
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
        if (!state.isNew) {
            Text(
                stringResource(R.string.idea_offer_hint),
                style = MaterialTheme.typography.bodySmall,
                color = LocalMonNoelColors.current.secondaryText,
            )
        }
    }
}
