package com.aktawind.monnoel.feature.gifts.person

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.aktawind.monnoel.core.domain.gifts.WhisperDraft
import com.aktawind.monnoel.core.domain.gifts.centsToInput
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.domain.gifts.whisperDuplicates
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.WhisperedIdea
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.MoneyInput
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.R as UiR
import com.aktawind.monnoel.core.ui.SectionTitle
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.feature.gifts.R

/** Fiche d'une personne : les idées soufflées pour elle, et de la part de qui, pour le Noël affiché. */
@Composable
internal fun WhisperSections(
    state: PersonUiState,
    onSave: (String?, WhisperDraft) -> Unit,
    onDelete: (String) -> Unit,
    onImportPhoto: (String, (String) -> Unit) -> Unit,
    onDiscardPhotos: () -> Unit,
    /** Idée de la liste à transformer en idée soufflée (fenêtre pré-remplie), ou null. */
    fromIdea: GiftIdea? = null,
    onFromIdeaDone: () -> Unit = {},
) {
    val person = state.person ?: return
    val edition = state.edition ?: return
    val secondary = LocalMonNoelColors.current.secondaryText
    val names = state.persons.associate { it.id to it.displayName }
    // null : fenêtre fermée ; "" : nouvelle idée ; sinon l'id de l'idée ouverte.
    var open by rememberSaveable { mutableStateOf<String?>(null) }

    SectionTitle(stringResource(R.string.whispers_for_title, edition.year))
    if (state.whispersFor.isEmpty()) Text(stringResource(R.string.whispers_none), color = secondary)
    state.whispersFor.forEach { w ->
        val duplicates =
            whisperDuplicates(state.editionWhispers, w.editionId, w.recipientId, w.title, w.givenToPersonId, w.givenToName, w.id)
        WhisperRow(
            title = w.title,
            detail = listOfNotNull(
                stringResource(R.string.whisper_given_to, w.givenToPersonId?.let(names::get) ?: w.givenToName),
                w.priceCents?.let { formatEuro(it) },
            ).joinToString(" · "),
            warning = duplicates.takeIf { it.isNotEmpty() }?.let { d ->
                stringResource(R.string.whisper_also_given, d.joinToString(", ") { it.givenToPersonId?.let(names::get) ?: it.givenToName })
            },
            photoPath = w.photoPath,
            onClick = { open = w.id },
        )
    }
    if (!state.readOnly) {
        OutlinedButton(onClick = { open = "" }) { Text(stringResource(R.string.whisper_add)) }
    }

    if (fromIdea != null && open == null) {
        WhisperDialog(
            whisper = null,
            recipientId = person.id,
            persons = state.persons.filter { it.id != person.id },
            editionWhispers = state.editionWhispers,
            editionId = edition.id,
            readOnly = state.readOnly,
            initialTitle = fromIdea.title,
            initialPriceCents = fromIdea.estimatedPriceCents,
            initialUrl = fromIdea.url,
            initialPhotoPath = fromIdea.photoPath,
            onImportPhoto = onImportPhoto,
            onSave = { draft ->
                onSave(null, draft)
                onFromIdeaDone()
            },
            onDelete = null,
            onDismiss = {
                onDiscardPhotos()
                onFromIdeaDone()
            },
        )
    }
    val current = open
    if (current != null) {
        val whisper = state.editionWhispers.firstOrNull { it.id == current }
        val recipientId = whisper?.recipientId ?: person.id
        WhisperDialog(
            whisper = whisper,
            recipientId = recipientId,
            persons = state.persons.filter { it.id != recipientId },
            editionWhispers = state.editionWhispers,
            editionId = edition.id,
            readOnly = state.readOnly,
            onImportPhoto = onImportPhoto,
            onSave = { draft ->
                onSave(whisper?.id, draft)
                open = null
            },
            onDelete = whisper?.let { w ->
                {
                    onDelete(w.id)
                    open = null
                }
            },
            onDismiss = {
                onDiscardPhotos()
                open = null
            },
        )
    }
}

@Composable
private fun WhisperRow(title: String, detail: String, warning: String?, photoPath: String?, onClick: () -> Unit) {
    val colors = LocalMonNoelColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoImage(photoPath, null, size = 44.dp, shape = MaterialTheme.shapes.small, placeholder = "🤫")
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
                if (warning != null) Text(warning, style = MaterialTheme.typography.bodySmall, color = colors.burgundy)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun WhisperDialog(
    whisper: WhisperedIdea?,
    recipientId: String,
    persons: List<Person>,
    editionWhispers: List<WhisperedIdea>,
    editionId: String,
    readOnly: Boolean,
    onSave: (WhisperDraft) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    initialTitle: String = "",
    initialPriceCents: Long? = null,
    initialUrl: String = "",
    initialPhotoPath: String? = null,
    onImportPhoto: (String, (String) -> Unit) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(whisper?.title ?: initialTitle) }
    var giverId by rememberSaveable { mutableStateOf(whisper?.givenToPersonId) }
    var giverName by rememberSaveable { mutableStateOf(if (whisper?.givenToPersonId == null) whisper?.givenToName.orEmpty() else "") }
    var price by rememberSaveable { mutableStateOf(centsToInput(whisper?.priceCents ?: initialPriceCents)) }
    var url by rememberSaveable { mutableStateOf(whisper?.url ?: initialUrl) }
    var photo by rememberSaveable { mutableStateOf(whisper?.photoPath ?: initialPhotoPath) }
    var notes by rememberSaveable { mutableStateOf(whisper?.notes.orEmpty()) }
    val names = persons.associate { it.id to it.displayName }
    val duplicates = whisperDuplicates(editionWhispers, editionId, recipientId, title, giverId, giverName, whisper?.id)
    val priceValid = price.isBlank() || parseEuroToCents(price) != null
    val hasGiver = giverId != null || giverName.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whisper_dialog_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput(title, { title = it }, stringResource(R.string.field_whisper_title), Modifier.fillMaxWidth(), enabled = !readOnly)
                DropdownField(
                    label = stringResource(R.string.field_whisper_given_to),
                    selected = persons.firstOrNull { it.id == giverId },
                    options = listOf(null) + persons,
                    optionLabel = { it?.displayName ?: stringResource(R.string.whisper_someone_else) },
                    onSelect = { p -> giverId = p?.id },
                    enabled = !readOnly,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (giverId == null) {
                    TextInput(giverName, {
                        giverName = it
                    }, stringResource(R.string.field_whisper_given_name), Modifier.fillMaxWidth(), enabled = !readOnly)
                }
                if (duplicates.isNotEmpty()) {
                    Text(
                        stringResource(
                            R.string.whisper_duplicate,
                            duplicates.joinToString(", ") {
                                it.givenToPersonId?.let(names::get)
                                    ?: it.givenToName
                            },
                        ),
                        color = LocalMonNoelColors.current.burgundy,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                MoneyInput(price, {
                    price = it
                }, stringResource(R.string.field_whisper_price), Modifier.fillMaxWidth(), enabled = !readOnly)
                TextInput(
                    url,
                    { url = it },
                    stringResource(R.string.field_whisper_url),
                    Modifier.fillMaxWidth(),
                    enabled = !readOnly,
                    keyboardType = KeyboardType.Uri,
                )
                PhotoField(
                    path = photo,
                    onPicked = { uri -> onImportPhoto(uri) { photo = it } },
                    onRemove = { photo = null },
                    enabled = !readOnly,
                    placeholder = "🤫",
                )
                TextInput(notes, { notes = it }, stringResource(R.string.field_notes), Modifier.fillMaxWidth(), enabled = !readOnly)
            }
        },
        confirmButton = {
            if (!readOnly) {
                TextButton(
                    onClick = {
                        onSave(
                            WhisperDraft(
                                recipientId = recipientId,
                                title = title,
                                givenToPersonId = giverId,
                                givenToName = if (giverId == null) giverName else "",
                                priceCents = parseEuroToCents(price),
                                url = url,
                                received = whisper?.received ?: false,
                                notes = notes,
                                photoPath = photo,
                                // Photo reprise de l'idée de la liste : elle sera copiée.
                                photoFromIdea = whisper == null && photo != null && photo == initialPhotoPath,
                            ),
                        )
                    },
                    enabled = title.isNotBlank() && hasGiver && priceValid,
                ) { Text(stringResource(UiR.string.action_save)) }
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null && !readOnly) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(UiR.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.action_cancel)) }
            }
        },
    )
}
