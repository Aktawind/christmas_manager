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
import com.aktawind.monnoel.core.model.InspirationKind
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
    /** Si renseigné, on peut aussi ranger le partage comme recette (nom, photo, lien). */
    onSaveRecipe: ((title: String, url: String) -> Unit)? = null,
    /** Si renseigné, on peut aussi ranger le partage comme inspiration (film, livre, sortie…). */
    onSaveInspiration: ((title: String, url: String, kind: InspirationKind) -> Unit)? = null,
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    var price by rememberSaveable { mutableStateOf(centsToInput(initialPriceCents)) }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    // 0 : idée cadeau ; 1 : recette ; 2 : inspiration.
    var target by rememberSaveable { mutableStateOf(0) }
    var kind by rememberSaveable { mutableStateOf(InspirationKind.FILM) }
    val asRecipe = target == 1
    val asIdea = target == 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when (target) {
                        1 -> R.string.quick_recipe_title
                        2 -> R.string.quick_inspiration_title
                        else -> R.string.quick_idea_title
                    },
                ),
            )
        },
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
                val targets = listOfNotNull(0, 1.takeIf { onSaveRecipe != null }, 2.takeIf { onSaveInspiration != null })
                if (targets.size > 1) {
                    DropdownField(
                        label = stringResource(R.string.quick_share_kind),
                        selected = target,
                        options = targets,
                        optionLabel = {
                            stringResource(
                                when (it) {
                                    1 -> R.string.quick_share_recipe
                                    2 -> R.string.quick_share_inspiration
                                    else -> R.string.quick_share_idea
                                },
                            )
                        },
                        onSelect = { target = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (target == 2) {
                    DropdownField(
                        label = stringResource(R.string.quick_inspiration_kind),
                        selected = kind,
                        options = InspirationKind.entries,
                        optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
                        onSelect = { kind = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextInput(
                    title,
                    { title = it },
                    stringResource(
                        when (target) {
                            1 -> R.string.quick_recipe_name
                            2 -> R.string.quick_recipe_name_generic
                            else -> R.string.quick_idea_name
                        },
                    ),
                    Modifier.fillMaxWidth(),
                )
                if (asIdea) MoneyInput(price, { price = it }, stringResource(R.string.quick_idea_price), Modifier.fillMaxWidth())
                TextInput(
                    url,
                    { url = it },
                    stringResource(R.string.quick_idea_link),
                    Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Uri,
                )
                if (persons.isNotEmpty() && asIdea) {
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
                    stringResource(
                        when (target) {
                            1 -> R.string.quick_recipe_hint
                            2 -> R.string.quick_inspiration_hint
                            else -> R.string.quick_idea_hint
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                )
            }
        },
        confirmButton = {
            val priceValid = price.isBlank() || parseEuroToCents(price) != null
            TextButton(
                onClick = {
                    if (asRecipe && onSaveRecipe != null) {
                        onSaveRecipe(title, url)
                    } else if (target == 2 && onSaveInspiration != null) {
                        onSaveInspiration(title, url, kind)
                    } else {
                        onSave(QuickIdeaInput(title, url, parseEuroToCents(price), selected.toSet()))
                    }
                },
                enabled = title.isNotBlank() && (!asIdea || priceValid),
            ) {
                Text(stringResource(R.string.quick_idea_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
