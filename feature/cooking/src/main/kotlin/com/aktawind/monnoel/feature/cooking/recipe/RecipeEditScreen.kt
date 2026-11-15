package com.aktawind.monnoel.feature.cooking.recipe

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.feature.cooking.R
import com.aktawind.monnoel.feature.cooking.withScheme

@Composable
fun RecipeEditRoute(onDone: () -> Unit, onMake: (String) -> Unit, viewModel: RecipeEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.outcome) {
        when (val outcome = state.outcome) {
            RecipeEditOutcome.Closed -> onDone()
            is RecipeEditOutcome.Make -> onMake(outcome.recipeId)
            null -> Unit
        }
    }
    val form = state.form
    val uriHandler = LocalUriHandler.current
    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.recipe_new_title else R.string.recipe_edit_title),
        onBack = onDone,
        onSave = { viewModel.save() },
        saveEnabled = form.name.isNotBlank(),
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = stringResource(R.string.recipe_delete_confirm),
        onDelete = if (state.isNew) null else ({ viewModel.delete() }),
    ) {
        TextInput(form.name, { v ->
            viewModel.update { copy(name = v) }
        }, stringResource(R.string.field_recipe_name), Modifier.fillMaxWidth())
        PhotoField(path = form.photoPath, onPicked = viewModel::importPhoto, onRemove = {
            viewModel.update { copy(photoPath = null) }
        }, placeholder = "🧁")
        DropdownField(
            label = stringResource(R.string.field_category),
            selected = state.categories.firstOrNull { it.id == form.categoryId },
            options = listOf(null) + state.categories,
            optionLabel = { it?.name ?: stringResource(R.string.no_category) },
            onSelect = { c -> viewModel.update { copy(categoryId = c?.id) } },
            modifier = Modifier.fillMaxWidth(),
        )
        TextInput(
            form.url,
            { v -> viewModel.update { copy(url = v) } },
            stringResource(R.string.field_recipe_url),
            Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Uri,
        )
        if (form.url.isNotBlank()) {
            TextButton(onClick = {
                runCatching { uriHandler.openUri(form.url.withScheme()) }
            }) { Text(stringResource(R.string.open_recipe_link)) }
        }
        Text(
            stringResource(R.string.recipe_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LocalMonNoelColors.current.secondaryText,
        )
        val edition = state.edition
        if (state.canMake && edition != null) {
            Button(
                onClick = { viewModel.save(make = true) },
                enabled = form.name.isNotBlank() && !state.busy,
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(stringResource(R.string.recipe_make_for, edition.year)) }
        }
    }
}
