package com.aktawind.monnoel.feature.gifts.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.MoneyInput
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.feature.gifts.R
import com.aktawind.monnoel.feature.gifts.components.EditorScaffold
import com.aktawind.monnoel.feature.gifts.components.emoji
import com.aktawind.monnoel.feature.gifts.components.label
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftEditRoute(onDone: () -> Unit, viewModel: GiftEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    var pickIdea by rememberSaveable { mutableStateOf(false) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val form = state.form
    val enabled = !state.readOnly && !state.loading
    val uriHandler = LocalUriHandler.current

    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.gift_new_title else R.string.gift_edit_title),
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
        deleteConfirmation = stringResource(R.string.gift_delete_confirm),
        onDelete = if (state.isNew || state.readOnly) {
            null
        } else {
            { viewModel.delete() }
        },
    ) {
        if (state.readOnly) Text(stringResource(R.string.gifts_read_only), color = MaterialTheme.colorScheme.tertiary)
        state.year?.let {
            Text(stringResource(R.string.gift_for_year, it), style = MaterialTheme.typography.labelLarge)
        }
        if (state.isNew && state.ideas.isNotEmpty()) {
            OutlinedButton(onClick = { pickIdea = true }) { Text(stringResource(R.string.gift_from_idea)) }
        }
        DropdownField(
            label = stringResource(R.string.field_recipient),
            selected = state.persons.firstOrNull { it.id == form.personId },
            options = listOf(null) + state.persons,
            optionLabel = { it?.displayName ?: stringResource(R.string.no_recipient) },
            onSelect = { p -> viewModel.update { copy(personId = p?.id) } },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        TextInput(form.title, { v ->
            viewModel.update { copy(title = v) }
        }, stringResource(R.string.field_gift), Modifier.fillMaxWidth(), enabled = enabled)
        DropdownField(
            label = stringResource(R.string.field_status),
            selected = form.status,
            options = GiftStatus.entries,
            optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
            onSelect = { s -> viewModel.update { copy(status = s) } },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MoneyInput(form.price, { v ->
                viewModel.update { copy(price = v) }
            }, stringResource(R.string.field_price), Modifier.weight(1f), enabled)
            OutlinedButton(onClick = { pickDate = true }, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(
                    form.purchaseDate?.let { stringResource(R.string.field_purchase_date_value, dateFormat.format(it)) }
                        ?: stringResource(R.string.field_purchase_date),
                )
            }
        }
        DropdownField(
            label = stringResource(R.string.field_category),
            selected = state.categories.firstOrNull { it.id == form.categoryId },
            options = listOf(null) + state.categories,
            optionLabel = { it?.name ?: stringResource(R.string.no_category) },
            onSelect = { c -> viewModel.update { copy(categoryId = c?.id) } },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        TextInput(form.shop, { v ->
            viewModel.update { copy(shop = v) }
        }, stringResource(R.string.field_shop), Modifier.fillMaxWidth(), enabled = enabled)
        TextInput(
            form.orderUrl,
            { v -> viewModel.update { copy(orderUrl = v) } },
            stringResource(R.string.field_order_url),
            Modifier.fillMaxWidth(),
            enabled = enabled,
            keyboardType = KeyboardType.Uri,
        )
        if (form.orderUrl.isNotBlank()) {
            TextButton(onClick = { runCatching { uriHandler.openUri(form.orderUrl.withScheme()) } }) {
                Text(stringResource(R.string.open_link))
            }
        }
        TextInput(
            form.trackingNumber,
            { v -> viewModel.update { copy(trackingNumber = v) } },
            stringResource(R.string.field_tracking),
            Modifier.fillMaxWidth(),
            enabled = enabled,
        )
        PhotoField(
            path = form.photoPath,
            onPicked = viewModel::importPhoto,
            onRemove = { viewModel.update { copy(photoPath = null) } },
            enabled = enabled,
        )
        TextInput(
            form.description,
            { v -> viewModel.update { copy(description = v) } },
            stringResource(R.string.field_description),
            Modifier.fillMaxWidth(),
            singleLine = false,
            enabled = enabled,
        )
        TextInput(
            form.notes,
            { v -> viewModel.update { copy(notes = v) } },
            stringResource(R.string.field_notes),
            Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 3,
            enabled = enabled,
        )
    }

    if (pickIdea) {
        IdeaPickerDialog(
            ideas = state.ideas,
            onPick = {
                viewModel.useIdea(it)
                pickIdea = false
            },
            onDismiss = { pickIdea = false },
        )
    }
    if (pickDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (form.purchaseDate ?: LocalDate.now()).atStartOfDay().toInstant(
                ZoneOffset.UTC,
            ).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val date = pickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    viewModel.update { copy(purchaseDate = date) }
                    pickDate = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.update { copy(purchaseDate = null) }
                    pickDate = false
                }) { Text(stringResource(R.string.action_clear)) }
            },
        ) { DatePicker(pickerState) }
    }
}

internal fun String.withScheme(): String = if (startsWith("http://") ||
    startsWith("https://")
) {
    this
} else {
    "https://$this"
}

@Composable
private fun IdeaPickerDialog(ideas: List<GiftIdea>, onPick: (GiftIdea) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gift_from_idea)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ideas, key = { it.id }) { idea ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        modifier = Modifier.fillMaxWidth().clickable { onPick(idea) },
                    ) {
                        Row(
                            Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PhotoImage(idea.photoPath, null, size = 40.dp, placeholder = "💡")
                            Text(idea.title, Modifier.weight(1f))
                            idea.estimatedPriceCents?.let { Text(formatEuro(it)) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_cancel))
            }
        },
    )
}
