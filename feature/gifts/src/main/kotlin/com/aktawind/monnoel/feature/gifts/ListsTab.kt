package com.aktawind.monnoel.feature.gifts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.IdeaRequest
import com.aktawind.monnoel.core.model.IdeaRequestKind
import com.aktawind.monnoel.core.ui.EmptyState

/**
 * Toutes les listes au Père Noël du Noël affiché : celles à envoyer (quelqu'un attend nos idées)
 * et celles qu'on attend. Elles se créent depuis la fiche de chaque personne.
 */
@Composable
internal fun ListsTabContent(state: GiftsUiState, onOpenPerson: (String) -> Unit, onDone: (String, Boolean) -> Unit) {
    val names = state.persons.associate { it.id to it.displayName }
    val toSend = state.requests.filter { it.first.kind == IdeaRequestKind.TO_SEND }
    val toReceive = state.requests.filter { it.first.kind == IdeaRequestKind.TO_RECEIVE }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.requests.isEmpty() && !state.loading) {
            item { EmptyState("📜", stringResource(R.string.lists_empty_title), stringResource(R.string.lists_empty_message)) }
        }
        listOf(R.string.lists_to_send to toSend, R.string.lists_to_receive to toReceive).forEach { (title, rows) ->
            if (rows.isEmpty()) return@forEach
            item(key = "h-$title") {
                Text(
                    stringResource(title, rows.count { !it.second }),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 16.dp, bottom = 4.dp).semantics { heading() },
                )
            }
            items(rows, key = { it.first.id }) { (request, done) ->
                RequestLine(
                    label = requestLine(request, names),
                    done = done,
                    readOnly = state.readOnly,
                    onDone = { onDone(request.id, it) },
                    onClick = { onOpenPerson(request.personId) },
                )
            }
        }
        if (state.requests.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.lists_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                    modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun requestLine(request: IdeaRequest, names: Map<String, String>): String {
    val person = names[request.personId].orEmpty()
    val other = request.otherPersonId?.let(names::get) ?: request.otherName
    return when {
        request.kind == IdeaRequestKind.TO_SEND -> stringResource(R.string.lists_line_to_send, person, other)
        request.otherPersonId == request.personId -> stringResource(R.string.lists_line_to_receive_self, person)
        else -> stringResource(R.string.lists_line_to_receive, other, person)
    }
}

@Composable
private fun RequestLine(label: String, done: Boolean, readOnly: Boolean, onDone: (Boolean) -> Unit, onClick: () -> Unit) {
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = done, onCheckedChange = if (readOnly) null else onDone)
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) LocalMonNoelColors.current.secondaryText else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
