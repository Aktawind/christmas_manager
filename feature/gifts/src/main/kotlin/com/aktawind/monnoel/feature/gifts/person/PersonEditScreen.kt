package com.aktawind.monnoel.feature.gifts.person

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.feature.gifts.R
import com.aktawind.monnoel.feature.gifts.components.EditorScaffold

@Composable
fun PersonEditRoute(
    onSaved: (id: String, isNew: Boolean) -> Unit,
    onDeleted: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PersonEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.doneWithId) {
        when (val id = state.doneWithId) {
            null -> Unit
            "" -> onDeleted()
            else -> onSaved(id, state.isNew)
        }
    }
    val form = state.form
    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.person_new_title else R.string.person_edit_title),
        onBack = onCancel,
        onSave = { viewModel.save() },
        saveEnabled = form.firstName.isNotBlank() || form.lastName.isNotBlank(),
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = stringResource(R.string.person_delete_confirm),
        onDelete = if (state.isNew) {
            null
        } else {
            { viewModel.delete() }
        },
    ) {
        TextInput(form.firstName, { v ->
            viewModel.update { copy(firstName = v) }
        }, stringResource(R.string.field_first_name), Modifier.fillMaxWidth())
        TextInput(form.lastName, { v ->
            viewModel.update { copy(lastName = v) }
        }, stringResource(R.string.field_last_name), Modifier.fillMaxWidth())
        PhotoField(path = form.photoPath, onPicked = viewModel::importPhoto, onRemove = {
            viewModel.update { copy(photoPath = null) }
        }, placeholder = "🧑‍🎄")
        TextInput(
            form.interests,
            { v -> viewModel.update { copy(interests = v) } },
            stringResource(R.string.field_interests),
            Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
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
}
