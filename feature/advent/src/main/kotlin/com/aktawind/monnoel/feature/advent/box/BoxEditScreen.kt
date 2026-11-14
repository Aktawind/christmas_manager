package com.aktawind.monnoel.feature.advent.box

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.SurpriseType
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.EditorScaffold
import com.aktawind.monnoel.core.ui.MoneyInput
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.R as UiR
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.feature.advent.R

@Composable
fun BoxEditRoute(onDone: () -> Unit, viewModel: BoxEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    var pickSource by rememberSaveable { mutableStateOf(false) }
    val form = state.form
    val enabled = !state.readOnly && !state.loading
    val uriHandler = LocalUriHandler.current

    EditorScaffold(
        title = stringResource(R.string.box_title, state.number),
        onBack = onDone,
        onSave = if (state.readOnly) {
            null
        } else {
            { viewModel.save() }
        },
        saveEnabled = true,
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = null,
        onDelete = null,
    ) {
        Text(state.calendarName, style = MaterialTheme.typography.labelLarge, color = LocalMonNoelColors.current.secondaryText)
        if (state.readOnly) Text(stringResource(R.string.calendar_read_only), color = MaterialTheme.colorScheme.tertiary)
        if (enabled && state.sources.isNotEmpty()) {
            OutlinedButton(onClick = { pickSource = true }) { Text(stringResource(R.string.box_reuse)) }
        }
        DropdownField(
            label = stringResource(R.string.field_box_status),
            selected = form.status,
            options = BoxStatus.entries,
            optionLabel = { stringResource(it.label) },
            onSelect = { s -> viewModel.update { copy(status = s) } },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownField(
            label = stringResource(R.string.field_surprise_type),
            selected = form.type,
            options = listOf<SurpriseType?>(null) + SurpriseType.entries,
            optionLabel = { t -> t?.let { "${it.emoji}  ${stringResource(it.label)}" } ?: stringResource(R.string.surprise_none) },
            onSelect = { t -> viewModel.update { copy(type = t) } },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        TextInput(form.title, { v ->
            viewModel.update { copy(title = v) }
        }, stringResource(R.string.field_box_content), Modifier.fillMaxWidth(), enabled = enabled)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MoneyInput(form.price, { v ->
                viewModel.update { copy(price = v) }
            }, stringResource(R.string.field_price), Modifier.weight(1f), enabled)
            TextInput(form.supplier, { v ->
                viewModel.update { copy(supplier = v) }
            }, stringResource(R.string.field_supplier), Modifier.weight(1f), enabled = enabled)
        }
        if (state.supplies.isNotEmpty()) {
            DropdownField(
                label = stringResource(R.string.field_group_purchase),
                selected = state.supplies.firstOrNull { it.id == form.supplyId },
                options = listOf(null) + state.supplies,
                optionLabel = { it?.name ?: stringResource(R.string.group_purchase_none) },
                onSelect = { s -> viewModel.update { copy(supplyId = s?.id) } },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextInput(form.url, { v ->
            viewModel.update { copy(url = v) }
        }, stringResource(R.string.field_url), Modifier.fillMaxWidth(), enabled = enabled, keyboardType = KeyboardType.Uri)
        if (form.url.isNotBlank()) {
            TextButton(onClick = {
                val url = if (form.url.startsWith("http")) form.url else "https://${form.url}"
                runCatching { uriHandler.openUri(url) }
            }) { Text(stringResource(R.string.open_link)) }
        }
        PhotoField(path = form.photoPath, onPicked = viewModel::importPhoto, onRemove = {
            viewModel.update { copy(photoPath = null) }
        }, enabled = enabled, placeholder = "🎁")
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

    if (pickSource) {
        AlertDialog(
            onDismissRequest = { pickSource = false },
            title = { Text(stringResource(R.string.box_reuse)) },
            text = {
                LazyColumn {
                    items(state.sources, key = { it.key }) { source ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                viewModel.useSource(source)
                                pickSource = false
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(source.type?.emoji ?: "🎁", style = MaterialTheme.typography.titleLarge)
                            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                Text(source.title.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
                                val price = source.priceCents?.let { " · ${formatEuro(it)}" }.orEmpty()
                                Text(
                                    stringResource(R.string.box_reuse_origin, source.origin, source.number) + price,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalMonNoelColors.current.secondaryText,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickSource = false }) { Text(stringResource(UiR.string.action_cancel)) } },
        )
    }
}
