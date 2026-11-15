package com.aktawind.monnoel.feature.cooking.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.MealType
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.EditorScaffold
import com.aktawind.monnoel.core.ui.MoneyInput
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.feature.cooking.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MealEditRoute(onSaved: (String) -> Unit, onDeleted: () -> Unit, onBack: () -> Unit, viewModel: MealEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedId) {
        when (val id = state.savedId) {
            null -> Unit
            "" -> onDeleted()
            else -> onSaved(id)
        }
    }
    val form = state.form
    val editable = !state.readOnly
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var guest by rememberSaveable { mutableStateOf("") }
    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.meal_new_title else R.string.meal_edit_title),
        onBack = onBack,
        onSave = if (editable) ({ viewModel.save() }) else null,
        saveEnabled = form.name.isNotBlank() && state.costValid && state.edition != null,
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = stringResource(R.string.meal_delete_confirm),
        onDelete = if (state.isNew || !editable) null else ({ viewModel.delete() }),
    ) {
        if (state.readOnly) Text(stringResource(R.string.meal_read_only), color = LocalMonNoelColors.current.secondaryText)
        DropdownField(
            label = stringResource(R.string.field_meal_type),
            selected = form.type,
            options = MealType.entries,
            optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
            onSelect = viewModel::selectType,
            modifier = Modifier.fillMaxWidth(),
            enabled = editable,
        )
        TextInput(form.name, { v ->
            viewModel.update { copy(name = v) }
        }, stringResource(R.string.field_meal_name), Modifier.fillMaxWidth(), enabled = editable)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickDate = true }, enabled = editable) {
                Text(
                    form.date?.let { stringResource(R.string.meal_date_value, it.format(mealDateFormat)) }
                        ?: stringResource(R.string.meal_date_none),
                )
            }
            if (form.date != null && editable) {
                TextButton(onClick = { viewModel.update { copy(date = null) } }) { Text(stringResource(R.string.meal_date_clear)) }
            }
        }
        TextInput(form.place, { v ->
            viewModel.update { copy(place = v) }
        }, stringResource(R.string.field_meal_place), Modifier.fillMaxWidth(), enabled = editable)

        Text(
            stringResource(R.string.meal_guests_title, form.personIds.size + form.guests.size),
            style = MaterialTheme.typography.titleSmall,
        )
        if (state.persons.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.persons.forEach { person ->
                    FilterChip(
                        selected = person.id in form.personIds,
                        onClick = { viewModel.togglePerson(person.id) },
                        label = { Text(person.displayName) },
                        enabled = editable,
                    )
                }
            }
        }
        if (form.guests.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                form.guests.forEach { name ->
                    InputChip(
                        selected = true,
                        onClick = { if (editable) viewModel.removeGuest(name) },
                        label = { Text(name) },
                        trailingIcon = if (editable) {
                            { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.meal_guest_remove, name)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        if (editable) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextInput(guest, { guest = it }, stringResource(R.string.field_meal_guest), Modifier.weight(1f))
                Button(onClick = {
                    viewModel.addGuest(guest)
                    guest = ""
                }, enabled = guest.isNotBlank()) { Text(stringResource(R.string.meal_guest_add)) }
            }
        }
        MoneyInput(form.cost, { v ->
            viewModel.update { copy(cost = v) }
        }, stringResource(R.string.field_meal_cost), Modifier.fillMaxWidth(), enabled = editable)
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
    if (pickDate) {
        val default = form.date ?: state.edition?.year?.let { LocalDate.of(it, 12, 24) } ?: LocalDate.now()
        val pickerState =
            rememberDatePickerState(initialSelectedDateMillis = default.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val date = pickerState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    viewModel.update { copy(date = date) }
                    pickDate = false
                }) { Text(stringResource(R.string.meal_date_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickDate = false }) { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_cancel)) }
            },
        ) { DatePicker(pickerState) }
    }
}
