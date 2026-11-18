package com.aktawind.monnoel.feature.gifts.person

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.IdeaRequest
import com.aktawind.monnoel.core.model.IdeaRequestKind
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.R as UiR
import com.aktawind.monnoel.core.ui.SectionTitle
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.feature.gifts.R

/**
 * Fiche d'une personne : qui attend des idées pour elle, et de qui on attend des idées.
 * Chaque demande a sa tâche dans Ma liste, cochée en même temps.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IdeaRequestSection(
    state: PersonUiState,
    onAdd: (IdeaRequestKind, String?, String) -> Unit,
    onDone: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    val person = state.person ?: return
    val edition = state.edition ?: return
    val first = person.firstName.ifBlank { person.displayName }
    val names = state.persons.associate { it.id to it.displayName }
    var adding by rememberSaveable { mutableStateOf<IdeaRequestKind?>(null) }

    SectionTitle(stringResource(R.string.requests_title, edition.year))
    state.requests.forEach { (request, done) ->
        RequestRow(requestLabel(request, first, names), done, state.readOnly, onDone = {
            onDone(request.id, it)
        }, onRemove = { onRemove(request.id) })
    }
    if (!state.readOnly) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { adding = IdeaRequestKind.TO_SEND }) { Text(stringResource(R.string.request_add_to_send, first)) }
            OutlinedButton(onClick = {
                adding = IdeaRequestKind.TO_RECEIVE
            }) { Text(stringResource(R.string.request_add_to_receive, first)) }
        }
    }
    adding?.let { kind ->
        RequestDialog(
            kind = kind,
            person = person,
            persons = state.persons,
            onSave = { otherId, otherName ->
                onAdd(kind, otherId, otherName)
                adding = null
            },
            onDismiss = { adding = null },
        )
    }
}

@Composable
private fun requestLabel(request: IdeaRequest, first: String, names: Map<String, String>): String {
    val other = request.otherPersonId?.let(names::get) ?: request.otherName
    return when {
        request.kind == IdeaRequestKind.TO_SEND -> stringResource(R.string.request_to_send, other, first)
        request.otherPersonId == request.personId -> stringResource(R.string.request_to_receive_self, first)
        else -> stringResource(R.string.request_to_receive, first, other)
    }
}

@Composable
private fun RequestRow(label: String, done: Boolean, readOnly: Boolean, onDone: (Boolean) -> Unit, onRemove: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = done, onCheckedChange = if (readOnly) null else onDone)
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) LocalMonNoelColors.current.secondaryText else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (!readOnly) {
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.request_remove)) }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

/** Choisir qui attend des idées (TO_SEND) ou pour qui on attend des idées (TO_RECEIVE). */
@Composable
private fun RequestDialog(
    kind: IdeaRequestKind,
    person: Person,
    persons: List<Person>,
    onSave: (String?, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val first = person.firstName.ifBlank { person.displayName }
    // Pour une attente d'idées, la personne elle-même est proposée en premier (« ses propres idées »).
    val options: List<Person?> = when (kind) {
        IdeaRequestKind.TO_SEND -> listOf(null) + persons.filter { it.id != person.id }
        IdeaRequestKind.TO_RECEIVE -> listOf(person) + persons.filter { it.id != person.id } + listOf(null)
    }
    var selectedId by rememberSaveable { mutableStateOf(if (kind == IdeaRequestKind.TO_RECEIVE) person.id else null) }
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (kind ==
                        IdeaRequestKind.TO_SEND
                    ) {
                        R.string.request_dialog_to_send
                    } else {
                        R.string.request_dialog_to_receive
                    },
                    first,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = stringResource(
                        if (kind ==
                            IdeaRequestKind.TO_SEND
                        ) {
                            R.string.field_request_who
                        } else {
                            R.string.field_request_for_whom
                        },
                    ),
                    selected = options.firstOrNull { it?.id == selectedId },
                    options = options,
                    optionLabel = {
                        when {
                            it == null -> stringResource(R.string.whisper_someone_else)
                            it.id == person.id -> stringResource(R.string.request_for_self, first)
                            else -> it.displayName
                        }
                    },
                    onSelect = { selectedId = it?.id },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (selectedId == null) {
                    TextInput(name, { name = it }, stringResource(R.string.field_whisper_given_name), Modifier.fillMaxWidth())
                }
                Text(
                    stringResource(R.string.request_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedId, name) }, enabled = selectedId != null || name.isNotBlank()) {
                Text(stringResource(R.string.request_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.action_cancel)) } },
    )
}
