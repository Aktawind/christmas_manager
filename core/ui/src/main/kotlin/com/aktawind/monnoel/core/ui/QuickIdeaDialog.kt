package com.aktawind.monnoel.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.gifts.centsToInput
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.model.Person

/**
 * Noter une idée en quelques secondes : un titre suffit ; on peut aussi indiquer pour qui
 * elle est (une ou plusieurs personnes). Le reste se complète plus tard.
 */
/** Ce qui a été saisi dans « Noter une idée ». */
data class QuickIdeaInput(val title: String, val url: String, val priceCents: Long?, val personIds: Set<String>)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickIdeaDialog(
    persons: List<Person>,
    onSave: (QuickIdeaInput) -> Unit,
    onDismiss: () -> Unit,
    initialTitle: String = "",
    initialUrl: String = "",
    initialPriceCents: Long? = null,
    photoPath: String? = null,
    /** Petite explication quand la page n'a pas tout donné (photo, prix…). */
    note: String? = null,
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    var price by rememberSaveable { mutableStateOf(centsToInput(initialPriceCents)) }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_idea_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (photoPath != null) {
                    PhotoImage(photoPath, stringResource(R.string.photo_current), size = 120.dp, placeholder = "💡")
                }
                if (note != null) {
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalMonNoelColors.current.secondaryText,
                    )
                }
                TextInput(title, { title = it }, stringResource(R.string.quick_idea_name), Modifier.fillMaxWidth())
                MoneyInput(price, { price = it }, stringResource(R.string.quick_idea_price), Modifier.fillMaxWidth())
                TextInput(
                    url,
                    { url = it },
                    stringResource(R.string.quick_idea_link),
                    Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Uri,
                )
                if (persons.isNotEmpty()) {
                    Text(stringResource(R.string.quick_idea_for_whom), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        persons.forEach { person ->
                            FilterChip(
                                selected = person.id in selected,
                                onClick = {
                                    selected =
                                        if (person.id in selected) selected - person.id else selected + person.id
                                },
                                label = { Text(person.displayName) },
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.quick_idea_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                )
            }
        },
        confirmButton = {
            val priceValid = price.isBlank() || parseEuroToCents(price) != null
            TextButton(
                onClick = { onSave(QuickIdeaInput(title, url, parseEuroToCents(price), selected.toSet())) },
                enabled = title.isNotBlank() && priceValid,
            ) {
                Text(stringResource(R.string.quick_idea_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
