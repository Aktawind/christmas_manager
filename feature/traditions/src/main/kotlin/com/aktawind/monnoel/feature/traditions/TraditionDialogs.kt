package com.aktawind.monnoel.feature.traditions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.traditions.InspirationDraft
import com.aktawind.monnoel.core.domain.traditions.TraditionIdea
import com.aktawind.monnoel.core.model.Inspiration
import com.aktawind.monnoel.core.model.InspirationKind
import com.aktawind.monnoel.core.model.Tradition
import com.aktawind.monnoel.core.model.TraditionYear
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.R as UiR
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label

/** Faite cette année ? Avec une photo souvenir et un petit mot. */
@Composable
internal fun MemoryDialog(
    tradition: Tradition,
    year: TraditionYear?,
    onImportPhoto: (String, (String) -> Unit) -> Unit,
    onSave: (Boolean, String?, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var done by rememberSaveable { mutableStateOf(year?.done ?: true) }
    var photo by rememberSaveable { mutableStateOf(year?.memoryPhotoPath) }
    var note by rememberSaveable { mutableStateOf(year?.memoryNote.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tradition.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = done, onCheckedChange = { done = it })
                    Text(stringResource(R.string.memory_done))
                }
                PhotoField(path = photo, onPicked = { uri ->
                    onImportPhoto(uri) { photo = it }
                }, onRemove = { photo = null }, placeholder = "📸")
                TextInput(note, {
                    note = it
                }, stringResource(R.string.memory_note), Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(done, photo, note) }) { Text(stringResource(UiR.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.action_cancel)) } },
    )
}

/** Piocher dans les idées de traditions : on coche celles qui plaisent. */
@Composable
internal fun IdeasDialog(ideas: List<TraditionIdea>, onAdd: (List<TraditionIdea>) -> Unit, onDismiss: () -> Unit) {
    var chosen by rememberSaveable { mutableStateOf(emptyList<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ideas_title)) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.ideas_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                )
                ideas.forEach { idea ->
                    val selected = idea.name in chosen
                    Row(
                        Modifier.fillMaxWidth().clickable { chosen = if (selected) chosen - idea.name else chosen + idea.name },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = selected, onCheckedChange = {
                            chosen = if (selected) chosen - idea.name else chosen + idea.name
                        })
                        Text("${idea.category.emoji}  ${idea.name}", modifier = Modifier.weight(1f))
                        Text(
                            stringResource(idea.period.label),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalMonNoelColors.current.secondaryText,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(ideas.filter { it.name in chosen }) }, enabled = chosen.isNotEmpty()) {
                Text(stringResource(R.string.ideas_add, chosen.size))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { chosen = ideas.map { it.name } }) { Text(stringResource(R.string.ideas_all)) }
                TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.action_cancel)) }
            }
        },
    )
}

@Composable
internal fun InspirationDialog(
    inspiration: Inspiration?,
    onImportPhoto: (String, (String) -> Unit) -> Unit,
    onSave: (InspirationDraft) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(inspiration?.title.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(inspiration?.kind ?: InspirationKind.FILM) }
    var photo by rememberSaveable { mutableStateOf(inspiration?.photoPath) }
    var url by rememberSaveable { mutableStateOf(inspiration?.url.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(inspiration?.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (inspiration == null) R.string.inspiration_new else R.string.inspiration_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = stringResource(R.string.field_inspiration_kind),
                    selected = kind,
                    options = InspirationKind.entries,
                    optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
                    onSelect = { kind = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextInput(title, { title = it }, stringResource(R.string.field_inspiration_title), Modifier.fillMaxWidth())
                TextInput(url, {
                    url = it
                }, stringResource(R.string.field_inspiration_url), Modifier.fillMaxWidth(), keyboardType = KeyboardType.Uri)
                PhotoField(path = photo, onPicked = { uri ->
                    onImportPhoto(uri) { photo = it }
                }, onRemove = { photo = null }, placeholder = kind.emoji)
                TextInput(notes, { notes = it }, stringResource(R.string.field_notes), Modifier.fillMaxWidth(), singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(InspirationDraft(title, kind, photo, url, notes)) }, enabled = title.isNotBlank()) {
                Text(stringResource(UiR.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(UiR.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.action_cancel)) }
            }
        },
    )
}
