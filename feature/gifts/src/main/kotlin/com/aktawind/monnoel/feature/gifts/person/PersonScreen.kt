package com.aktawind.monnoel.feature.gifts.person

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.gifts.centsToInput
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.ui.EditorScaffold
import com.aktawind.monnoel.core.ui.MoneyInput
import com.aktawind.monnoel.core.ui.PersonAvatar
import com.aktawind.monnoel.core.ui.SectionTitle
import com.aktawind.monnoel.feature.gifts.R
import com.aktawind.monnoel.feature.gifts.components.EnvelopeProgress
import com.aktawind.monnoel.feature.gifts.components.GiftRow
import com.aktawind.monnoel.feature.gifts.components.emoji

@Composable
fun PersonRoute(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenGift: (String) -> Unit,
    onNewGift: (String) -> Unit,
    onOpenIdea: (String) -> Unit,
    onNewIdea: (String) -> Unit,
    viewModel: PersonViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val person = state.person
    LaunchedEffect(state.loading, person) { if (!state.loading && person == null) onBack() }
    if (person == null) return
    val secondary = LocalMonNoelColors.current.secondaryText

    EditorScaffold(
        title = person.displayName,
        onBack = onBack,
        onSave = null,
        saveEnabled = false,
        busy = false,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        deleteConfirmation = null,
        onDelete = null,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            PersonAvatar(person.photoPath, person.initials, size = 96.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(person.displayName, style = MaterialTheme.typography.headlineMedium)
                if (person.interests.isNotBlank()) {
                    Text(
                        "❤️ ${person.interests}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (person.notes.isNotBlank()) {
                    Text(
                        person.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondary,
                    )
                }
            }
            OutlinedButton(onClick = {
                onEdit(person.id)
            }) { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_edit)) }
        }

        state.edition?.let { edition ->
            SectionTitle(stringResource(R.string.person_envelope_title, edition.year))
            var envelope by rememberSaveable(state.budget?.envelopeCents) {
                mutableStateOf(centsToInput(state.budget?.envelopeCents))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MoneyInput(envelope, {
                    envelope = it
                }, stringResource(R.string.person_envelope_label), Modifier.weight(1f), enabled = !state.readOnly)
                Button(onClick = { viewModel.saveEnvelope(envelope) }, enabled = !state.readOnly) {
                    Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_save))
                }
            }
            EnvelopeProgress(state.budget, Modifier.fillMaxWidth())

            SectionTitle(stringResource(R.string.person_gifts_title, edition.year))
            if (state.currentGifts.isEmpty()) Text(stringResource(R.string.person_no_gift), color = secondary)
            state.currentGifts.forEach { gift ->
                GiftRow(
                    gift = gift,
                    showRecipient = false,
                    readOnly = state.readOnly,
                    onClick = { onOpenGift(gift.id) },
                    onStatus = { viewModel.setStatus(gift.id, it) },
                )
            }
            if (!state.readOnly) {
                OutlinedButton(onClick = { onNewGift(person.id) }) { Text(stringResource(R.string.person_add_gift)) }
            }
        }

        SectionTitle(stringResource(R.string.person_ideas_title))
        if (state.ideas.isEmpty()) Text(stringResource(R.string.person_no_idea), color = secondary)
        state.ideas.forEach { idea ->
            Card(
                onClick = { onOpenIdea(idea.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp)) {
                    Text("💡 ${idea.title}", modifier = Modifier.weight(1f))
                    idea.estimatedPriceCents?.let { Text(formatEuro(it), color = secondary) }
                }
            }
        }
        OutlinedButton(onClick = { onNewIdea(person.id) }) { Text(stringResource(R.string.person_add_idea)) }

        if (state.history.isNotEmpty()) {
            SectionTitle(stringResource(R.string.person_history_title))
            state.history.forEach { (year, gifts) ->
                Text(stringResource(R.string.person_history_year, year), style = MaterialTheme.typography.titleSmall)
                gifts.forEach { gift ->
                    Text(
                        "${gift.status.emoji} ${gift.title}" + (gift.priceCents?.let { " · ${formatEuro(it)}" } ?: ""),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
