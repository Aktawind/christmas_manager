package com.aktawind.monnoel.feature.cooking.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
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
import com.aktawind.monnoel.core.model.CourseType
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.EditorScaffold
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.feature.cooking.R

@Composable
fun DishEditRoute(onDone: () -> Unit, viewModel: DishEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    val form = state.form
    val editable = !state.readOnly
    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.dish_new_title else R.string.dish_edit_title),
        onBack = onDone,
        onSave = if (editable) ({ viewModel.save() }) else null,
        saveEnabled = form.name.isNotBlank(),
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = stringResource(R.string.dish_delete_confirm),
        onDelete = if (state.isNew || !editable) null else ({ viewModel.delete() }),
    ) {
        if (state.mealName.isNotBlank()) {
            Text(stringResource(R.string.dish_for_meal, state.mealName), color = LocalMonNoelColors.current.secondaryText)
        }
        DropdownField(
            label = stringResource(R.string.field_course),
            selected = form.course,
            options = CourseType.entries,
            optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
            onSelect = { c -> viewModel.update { copy(course = c) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = editable,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.photoPath != null) PhotoImage(state.photoPath, null, size = 56.dp, placeholder = form.course.emoji)
            DropdownField(
                label = stringResource(R.string.field_recipe),
                selected = state.recipes.firstOrNull { it.id == form.recipeId },
                options = listOf(null) + state.recipes,
                optionLabel = { it?.name ?: stringResource(R.string.no_recipe) },
                onSelect = viewModel::selectRecipe,
                modifier = Modifier.weight(1f),
                enabled = editable,
            )
        }
        TextInput(form.name, { v ->
            viewModel.update { copy(name = v) }
        }, stringResource(R.string.field_dish_name), Modifier.fillMaxWidth(), enabled = editable)
        TextInput(
            form.broughtBy,
            { v -> viewModel.update { copy(broughtBy = v) } },
            stringResource(R.string.field_brought_by),
            Modifier.fillMaxWidth(),
            enabled = editable,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.field_to_order), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = form.toOrder, onCheckedChange = { v ->
                viewModel.update { copy(toOrder = v, ordered = ordered && v) }
            }, enabled = editable)
        }
        if (form.toOrder) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = form.ordered, onCheckedChange = { v -> viewModel.update { copy(ordered = v) } }, enabled = editable)
                Text(stringResource(R.string.field_ordered), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Text(
            stringResource(R.string.dish_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LocalMonNoelColors.current.secondaryText,
        )
    }
}
