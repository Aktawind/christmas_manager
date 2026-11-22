package com.aktawind.monnoel.feature.traditions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.traditions.CraftIdea
import com.aktawind.monnoel.core.domain.traditions.DecorDraft
import com.aktawind.monnoel.core.model.Craft
import com.aktawind.monnoel.core.model.CraftStatus
import com.aktawind.monnoel.core.model.DecorIdea
import com.aktawind.monnoel.core.model.DecorPlace
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.EmptyState
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.R as UiR
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label

/** Filtre des bricolages : tout, ou un avancement (null dans [CraftFilter.Status] = idée). */
internal sealed interface CraftFilter {
    data object All : CraftFilter

    data class Status(val status: CraftStatus?) : CraftFilter
}

@Composable
internal fun CraftsTab(
    state: TraditionsUiState,
    onOpen: (String) -> Unit,
    onStatus: (String, CraftStatus?) -> Unit,
    onPick: () -> Unit,
) {
    var filter by remember { mutableStateOf<CraftFilter>(CraftFilter.All) }
    val visible = state.crafts.filter { craft ->
        when (val f = filter) {
            CraftFilter.All -> true
            is CraftFilter.Status -> state.craftProgram[craft.id]?.status == f.status
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(320.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.crafts_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    if (state.craftIdeas.isNotEmpty()) {
                        OutlinedButton(onClick = onPick) { Text(stringResource(R.string.ideas_pick, state.craftIdeas.size)) }
                    }
                }
                if (state.crafts.isNotEmpty() && state.edition != null) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filter == CraftFilter.All,
                            onClick = { filter = CraftFilter.All },
                            label = { Text(stringResource(R.string.filter_everything)) },
                        )
                        craftStatusChoices.forEach { s ->
                            FilterChip(
                                selected = filter == CraftFilter.Status(s),
                                onClick = { filter = CraftFilter.Status(s) },
                                label = { Text("${craftStatusEmoji(s)} ${stringResource(craftStatusLabel(s))}") },
                            )
                        }
                    }
                }
            }
        }
        if (state.crafts.isEmpty() && !state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("✂️", stringResource(R.string.crafts_empty_title), stringResource(R.string.crafts_empty_message))
            }
        } else if (visible.isEmpty() && !state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.crafts_filter_empty), color = LocalMonNoelColors.current.secondaryText)
            }
        }
        items(visible, key = { it.id }) { craft ->
            val supplies = state.craftSupplies[craft.id].orEmpty()
            CraftCard(
                craft,
                status = state.craftProgram[craft.id]?.status,
                photo = state.craftProgram[craft.id]?.resultPhotoPath ?: craft.photoPath,
                bought = supplies.count { it.id in state.craftBought },
                supplyCount = supplies.size,
                years = state.craftDoneYears[craft.id].orEmpty(),
                canChange = state.edition != null && !state.readOnly,
                onClick = { onOpen(craft.id) },
                onStatus = { onStatus(craft.id, it) },
            )
        }
    }
}

@Composable
private fun CraftCard(
    craft: Craft,
    status: CraftStatus?,
    photo: String?,
    bought: Int,
    supplyCount: Int,
    years: List<Int>,
    canChange: Boolean,
    onClick: () -> Unit,
    onStatus: (CraftStatus?) -> Unit,
) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(photo, null, size = 104.dp, shape = RectangleShape, placeholder = craft.category.emoji)
            Column(
                Modifier.weight(1f).padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(craft.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val details = listOfNotNull(
                    stringResource(craft.category.label),
                    if (supplyCount > 0) stringResource(R.string.craft_supplies_count, bought, supplyCount) else null,
                )
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Ligne toujours présente : toutes les cartes gardent la même hauteur.
                Text(
                    if (years.isEmpty()) "" else stringResource(R.string.craft_done_years, years.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    maxLines = 1,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (canChange) StatusChip(status, onStatus)
            }
        }
    }
}

/** Puce d'avancement : un appui ouvre la liste des choix. */
@Composable
internal fun StatusChip(status: CraftStatus?, onStatus: (CraftStatus?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = stringResource(craftStatusLabel(status))
    val description = stringResource(R.string.craft_status_change, label)
    Box {
        FilterChip(
            selected = status != null,
            onClick = { open = true },
            label = { Text("${craftStatusEmoji(status)} $label") },
            modifier = Modifier.semantics { contentDescription = description },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            craftStatusChoices.forEach { s ->
                DropdownMenuItem(
                    text = { Text("${craftStatusEmoji(s)}  ${stringResource(craftStatusLabel(s))}") },
                    onClick = {
                        open = false
                        onStatus(s)
                    },
                )
            }
        }
    }
}

@Composable
internal fun CraftIdeasDialog(ideas: List<CraftIdea>, onAdd: (List<CraftIdea>) -> Unit, onDismiss: () -> Unit) {
    var chosen by rememberSaveable { mutableStateOf(emptyList<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.craft_ideas_title)) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.craft_ideas_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                )
                ideas.forEach { idea ->
                    val selected = idea.title in chosen
                    Row(
                        Modifier.fillMaxWidth().clickable { chosen = if (selected) chosen - idea.title else chosen + idea.title },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = selected, onCheckedChange = {
                            chosen = if (selected) chosen - idea.title else chosen + idea.title
                        })
                        Text("${idea.category.emoji}  ${idea.title}", modifier = Modifier.weight(1f))
                        Text(
                            stringResource(idea.category.label),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalMonNoelColors.current.secondaryText,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(ideas.filter { it.title in chosen }) }, enabled = chosen.isNotEmpty()) {
                Text(stringResource(R.string.ideas_add, chosen.size))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { chosen = ideas.map { it.title } }) { Text(stringResource(R.string.ideas_all)) }
                TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.action_cancel)) }
            }
        },
    )
}

/** Filtre du tableau de déco : tout, les favorites ou un endroit. */
internal sealed interface DecorFilter {
    data object All : DecorFilter

    data object Favorites : DecorFilter

    data class Place(val place: DecorPlace) : DecorFilter
}

@Composable
internal fun DecorTab(state: TraditionsUiState, onOpen: (String) -> Unit, onFavorite: (DecorIdea) -> Unit) {
    var filter by remember { mutableStateOf<DecorFilter>(DecorFilter.All) }
    val visible = state.decor.filter {
        when (val f = filter) {
            DecorFilter.All -> true
            DecorFilter.Favorites -> it.favorite
            is DecorFilter.Place -> it.place == f.place
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(200.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.decor.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter == DecorFilter.All,
                        onClick = { filter = DecorFilter.All },
                        label = { Text(stringResource(R.string.filter_everything)) },
                    )
                    FilterChip(
                        selected = filter == DecorFilter.Favorites,
                        onClick = { filter = DecorFilter.Favorites },
                        label = { Text(stringResource(R.string.decor_favorites)) },
                    )
                    DecorPlace.entries.forEach { p ->
                        FilterChip(
                            selected = filter == DecorFilter.Place(p),
                            onClick = { filter = DecorFilter.Place(p) },
                            label = { Text("${p.emoji} ${stringResource(p.label)}") },
                        )
                    }
                }
            }
        } else if (!state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("🎄", stringResource(R.string.decor_empty_title), stringResource(R.string.decor_empty_message))
            }
        }
        items(visible, key = { it.id }) { decor ->
            DecorCard(decor, onClick = { onOpen(decor.id) }, onFavorite = { onFavorite(decor) })
        }
    }
}

@Composable
private fun DecorCard(decor: DecorIdea, onClick: () -> Unit, onFavorite: () -> Unit) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        PhotoImage(
            decor.photoPath,
            decor.title.ifBlank { null },
            modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
            size = 200.dp,
            shape = RectangleShape,
            placeholder = decor.place.emoji,
        )
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(decor.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, minLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${decor.place.emoji} ${stringResource(decor.place.label)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (decor.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(if (decor.favorite) R.string.decor_unfavorite else R.string.decor_favorite),
                    tint = if (decor.favorite) LocalMonNoelColors.current.burgundy else secondary,
                )
            }
        }
        // Ligne toujours présente : toutes les cartes gardent la même hauteur.
        Text(
            decor.comment,
            style = MaterialTheme.typography.bodySmall,
            color = secondary,
            maxLines = 1,
            minLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
        )
    }
}

@Composable
internal fun DecorDialog(
    decor: DecorIdea?,
    onImportPhoto: (String, (String) -> Unit) -> Unit,
    onSave: (DecorDraft) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(decor?.title.orEmpty()) }
    var place by rememberSaveable { mutableStateOf(decor?.place ?: DecorPlace.TREE) }
    var photo by rememberSaveable { mutableStateOf(decor?.photoPath) }
    var url by rememberSaveable { mutableStateOf(decor?.url.orEmpty()) }
    var comment by rememberSaveable { mutableStateOf(decor?.comment.orEmpty()) }
    var favorite by rememberSaveable { mutableStateOf(decor?.favorite ?: false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (decor == null) R.string.decor_new else R.string.decor_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoField(path = photo, onPicked = { uri ->
                    onImportPhoto(uri) { photo = it }
                }, onRemove = { photo = null }, placeholder = place.emoji)
                DropdownField(
                    label = stringResource(R.string.field_decor_place),
                    selected = place,
                    options = DecorPlace.entries,
                    optionLabel = { "${it.emoji}  ${stringResource(it.label)}" },
                    onSelect = { place = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextInput(title, { title = it }, stringResource(R.string.field_decor_title), Modifier.fillMaxWidth())
                TextInput(url, {
                    url = it
                }, stringResource(R.string.field_decor_url), Modifier.fillMaxWidth(), keyboardType = KeyboardType.Uri)
                TextInput(
                    comment,
                    { comment = it },
                    stringResource(R.string.field_decor_comment),
                    Modifier.fillMaxWidth(),
                    singleLine = false,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.decor_favorites), modifier = Modifier.weight(1f))
                    Switch(checked = favorite, onCheckedChange = { favorite = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(DecorDraft(title, place, photo, url, comment, favorite)) },
                enabled = title.isNotBlank() || photo != null,
            ) {
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

/** Petite puce d'aide : ouvre un lien. */
@Composable
internal fun LinkChip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}
