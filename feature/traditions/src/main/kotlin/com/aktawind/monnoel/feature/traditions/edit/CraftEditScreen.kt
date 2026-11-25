package com.aktawind.monnoel.feature.traditions.edit

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.model.CraftCategory
import com.aktawind.monnoel.core.model.CraftStatus
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.EditorScaffold
import com.aktawind.monnoel.core.ui.MoneyInput
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.SectionTitle
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.feature.traditions.LinkChip
import com.aktawind.monnoel.feature.traditions.R
import com.aktawind.monnoel.feature.traditions.craftStatusChoices
import com.aktawind.monnoel.feature.traditions.craftStatusEmoji
import com.aktawind.monnoel.feature.traditions.craftStatusLabel
import com.aktawind.monnoel.feature.traditions.emoji
import com.aktawind.monnoel.feature.traditions.label

@Composable
fun CraftEditRoute(onDone: () -> Unit, onOpenCopy: (String) -> Unit, viewModel: CraftEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    LaunchedEffect(state.copyId) { state.copyId?.let(onOpenCopy) }
    val form = state.form
    val context = LocalContext.current
    val secondary = LocalMonNoelColors.current.secondaryText
    EditorScaffold(
        title = stringResource(if (state.isNew) R.string.craft_new_title else R.string.craft_edit_title),
        onBack = onDone,
        onSave = { viewModel.save() },
        saveEnabled = form.title.isNotBlank(),
        busy = state.busy,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = stringResource(R.string.craft_delete_confirm),
        onDelete = if (state.isNew) null else ({ viewModel.delete() }),
    ) {
        TextInput(
            form.title,
            { v -> viewModel.update { copy(title = v) } },
            stringResource(R.string.field_craft_title),
            Modifier.fillMaxWidth(),
        )
        PhotoField(path = form.photoPath, onPicked = viewModel::importPhoto, onRemove = {
            viewModel.update { copy(photoPath = null) }
        }, placeholder = form.category.emoji)
        DropdownField(
            label = stringResource(R.string.field_craft_category),
            selected = form.category,
            options = CraftCategory.entries,
            optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
            onSelect = { c -> viewModel.update { copy(category = c) } },
            modifier = Modifier.fillMaxWidth(),
        )
        TextInput(
            form.tutorialUrl,
            { v -> viewModel.update { copy(tutorialUrl = v) } },
            stringResource(R.string.field_craft_tutorial),
            Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Uri,
        )
        val url = form.tutorialUrl.trim()
        if (url.startsWith("http")) {
            LinkChip(stringResource(R.string.craft_open_tutorial)) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
            }
        }

        val edition = state.edition
        if (edition != null) {
            SectionTitle(stringResource(R.string.craft_year_title, edition.year))
            DropdownField(
                label = stringResource(R.string.field_craft_status),
                selected = state.status,
                options = craftStatusChoices,
                optionLabel = { "${craftStatusEmoji(it)}  ${stringResource(craftStatusLabel(it))}" },
                onSelect = viewModel::setStatus,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canEditYear,
            )
            if (state.status == CraftStatus.DONE) {
                Text(stringResource(R.string.craft_result_photo), style = MaterialTheme.typography.titleSmall)
                PhotoField(
                    path = state.resultPhotoPath,
                    onPicked = viewModel::importResultPhoto,
                    onRemove = viewModel::removeResultPhoto,
                    enabled = state.canEditYear,
                    placeholder = "📸",
                )
            }
        }

        SectionTitle(stringResource(R.string.craft_supplies))
        val canCheck = state.canEditYear && state.status != null
        if (canCheck && state.supplies.isNotEmpty()) {
            Text(stringResource(R.string.craft_supplies_hint), style = MaterialTheme.typography.bodySmall, color = secondary)
        }
        state.supplies.forEach { supply ->
            val checked = supply.id in state.bought
            val boughtLabel = stringResource(R.string.craft_supply_bought, supply.name)
            Row(
                Modifier.fillMaxWidth().then(if (canCheck) Modifier.clickable { viewModel.toggleBought(supply.id) } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canCheck) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { viewModel.toggleBought(supply.id) },
                        modifier = Modifier.semantics { contentDescription = boughtLabel },
                    )
                } else {
                    Text("•", modifier = Modifier.padding(horizontal = 16.dp))
                }
                Text(supply.name, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
                supply.priceCents?.let { Text(formatEuro(it), color = secondary) }
                IconButton(onClick = { viewModel.removeSupply(supply.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.craft_supply_remove, supply.name))
                }
            }
        }
        AddSupplyRow(onAdd = viewModel::addSupply)

        TextInput(
            form.notes,
            { v -> viewModel.update { copy(notes = v) } },
            stringResource(R.string.field_notes),
            Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 3,
        )
        if (!state.isNew) {
            val copyName = stringResource(com.aktawind.monnoel.core.ui.R.string.copy_title, form.title)
            OutlinedButton(onClick = { viewModel.duplicate(copyName) }) {
                Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_duplicate))
            }
        }
        if (state.history.isNotEmpty()) {
            SectionTitle(stringResource(R.string.craft_history))
            Text(stringResource(R.string.craft_done_years, state.history.sorted().joinToString(", ")), color = secondary)
        }
    }
}

@Composable
private fun AddSupplyRow(onAdd: (String, Long?) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    val priceCents = parseEuroToCents(price)
    val valid = name.isNotBlank() && (price.isBlank() || priceCents != null)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextInput(name, { name = it }, stringResource(R.string.field_supply_name), Modifier.weight(2f))
        MoneyInput(price, { price = it }, stringResource(R.string.field_supply_price), Modifier.weight(1f))
        IconButton(
            onClick = {
                onAdd(name, priceCents)
                name = ""
                price = ""
            },
            enabled = valid,
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.craft_supply_add))
        }
    }
}
