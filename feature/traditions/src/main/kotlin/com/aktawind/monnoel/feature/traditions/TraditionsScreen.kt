package com.aktawind.monnoel.feature.traditions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.DecorPlace
import com.aktawind.monnoel.core.model.Inspiration
import com.aktawind.monnoel.core.model.InspirationKind
import com.aktawind.monnoel.core.model.Tradition
import com.aktawind.monnoel.core.model.TraditionYear
import com.aktawind.monnoel.core.ui.EmptyState
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.R as UiR
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.core.ui.longDateFormat
import com.aktawind.monnoel.core.ui.resolve
import com.aktawind.monnoel.core.ui.shortDateFormat
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraditionsRoute(
    onOpenTradition: (String) -> Unit,
    onNewTradition: () -> Unit,
    onOpenCraft: (String) -> Unit,
    onNewCraft: () -> Unit,
    viewModel: TraditionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 0 : au programme ; 1 : nos traditions ; 2 : inspirations ; 3 : bricolages ; 4 : décoration.
    var tab by rememberSaveable { mutableIntStateOf(viewModel.initialTab) }
    var memoryFor by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var pickingCrafts by rememberSaveable { mutableStateOf(false) }
    // null : fermé ; "" : nouvelle déco ; sinon son id.
    var decorOpen by rememberSaveable { mutableStateOf(viewModel.initialDecorId) }
    // Filtres remontés ici : « + Inspiration » / « + Déco » reprennent l'étiquette choisie.
    var kindFilter by rememberSaveable { mutableStateOf<InspirationKind?>(null) }
    var decorFilter by remember { mutableStateOf<DecorFilter>(DecorFilter.All) }
    val defaultDate = state.edition?.year?.let { defaultDateFor(it) } ?: LocalDate.now()
    val copyTitle = stringResource(UiR.string.copy_title, "%s")
    // null : fermé ; "" : nouvelle inspiration ; sinon son id.
    var inspirationOpen by rememberSaveable { mutableStateOf(viewModel.initialInspirationId) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it.resolve(context))
            viewModel.messageShown()
        }
    }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            state.edition?.let { stringResource(R.string.traditions_title_year, it.year) }
                                ?: stringResource(R.string.traditions_title),
                        )
                    },
                )
                PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
                    listOf(
                        R.string.tab_program,
                        R.string.tab_traditions,
                        R.string.tab_inspirations,
                        R.string.tab_crafts,
                        R.string.tab_decor,
                    )
                        .forEachIndexed { index, title ->
                            Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(title)) })
                        }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (tab != TAB_PROGRAM) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (tab) {
                            TAB_TRADITIONS -> onNewTradition()
                            TAB_INSPIRATIONS -> inspirationOpen = ""
                            TAB_CRAFTS -> onNewCraft()
                            else -> decorOpen = ""
                        }
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = {
                        val label = when (tab) {
                            TAB_TRADITIONS -> R.string.fab_tradition
                            TAB_INSPIRATIONS -> R.string.fab_inspiration
                            TAB_CRAFTS -> R.string.fab_craft
                            else -> R.string.fab_decor
                        }
                        Text(stringResource(label))
                    },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> ProgramTab(
                    state,
                    onDone = { id, done ->
                        val year = state.program[id]
                        viewModel.saveMemory(id, done, year?.memoryPhotoPath, year?.memoryNote.orEmpty(), year?.date)
                    },
                    onMemory = { memoryFor = it },
                    onGoToCatalog = { tab = 1 },
                )
                1 -> TraditionsTab(
                    state,
                    onOpen = onOpenTradition,
                    onFavorite = viewModel::toggleFavorite,
                    onPlanned = { id, planned -> viewModel.setPlanned(id, planned) },
                    onPick = { picking = true },
                )
                TAB_INSPIRATIONS -> InspirationsTab(
                    state,
                    kindFilter = kindFilter,
                    onFilter = { kindFilter = it },
                    onOpen = { inspirationOpen = it },
                    onDone = { id, done -> viewModel.setInspirationDone(id, done) },
                )
                TAB_CRAFTS -> CraftsTab(
                    state,
                    onOpen = onOpenCraft,
                    onStatus = { id, status -> viewModel.setCraftStatus(id, status) },
                    onPick = { pickingCrafts = true },
                )
                else -> DecorTab(
                    state,
                    filter = decorFilter,
                    onFilter = { decorFilter = it },
                    onOpen = { decorOpen = it },
                    onFavorite = viewModel::toggleDecorFavorite,
                )
            }
        }
    }

    memoryFor?.let { id ->
        val tradition = state.traditions.firstOrNull { it.id == id }
        if (tradition != null) {
            MemoryDialog(
                tradition = tradition,
                year = state.program[id],
                defaultDate = defaultDate,
                onImportPhoto = viewModel::importPhoto,
                onSave = { done, photo, note, date ->
                    viewModel.saveMemory(id, done, photo, note, date)
                    memoryFor = null
                },
                onDismiss = {
                    viewModel.discardPhotos()
                    memoryFor = null
                },
            )
        }
    }
    if (picking) {
        IdeasDialog(
            ideas = state.ideas,
            onAdd = {
                viewModel.addIdeas(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
    if (pickingCrafts) {
        CraftIdeasDialog(
            ideas = state.craftIdeas,
            onAdd = {
                viewModel.addCraftIdeas(it)
                pickingCrafts = false
            },
            onDismiss = { pickingCrafts = false },
        )
    }
    decorOpen?.let { id ->
        val decor = state.decor.firstOrNull { it.id == id }
        if (id.isEmpty() || decor != null) {
            DecorDialog(
                decor = decor,
                initialPlace = (decorFilter as? DecorFilter.Place)?.place ?: DecorPlace.TREE,
                initialFavorite = decorFilter == DecorFilter.Favorites,
                onDuplicate = decor?.let { d ->
                    { viewModel.duplicateDecor(d.id, if (d.title.isBlank()) "" else copyTitle.format(d.title)) { decorOpen = it } }
                },
                onImportPhoto = viewModel::importPhoto,
                onSave = { draft ->
                    viewModel.saveDecor(decor?.id, draft)
                    decorOpen = null
                },
                onDelete = decor?.let { d ->
                    {
                        viewModel.deleteDecor(d.id)
                        decorOpen = null
                    }
                },
                onDismiss = {
                    viewModel.discardPhotos()
                    decorOpen = null
                },
            )
        }
    }
    inspirationOpen?.let { id ->
        val inspiration = state.inspirations.firstOrNull { it.id == id }
        if (id.isEmpty() || inspiration != null) {
            InspirationDialog(
                inspiration = inspiration,
                initialKind = kindFilter ?: InspirationKind.FILM,
                date = inspiration?.let { state.inspirationDates[it.id] },
                canDate = state.edition != null && !state.readOnly,
                defaultDate = defaultDate,
                onDuplicate = inspiration?.let { i ->
                    { viewModel.duplicateInspiration(i.id, copyTitle.format(i.title)) { inspirationOpen = it } }
                },
                onImportPhoto = viewModel::importPhoto,
                onSave = { draft, date ->
                    viewModel.saveInspiration(inspiration?.id, draft, date)
                    inspirationOpen = null
                },
                onDelete = inspiration?.let { i ->
                    {
                        viewModel.deleteInspiration(i.id)
                        inspirationOpen = null
                    }
                },
                onDismiss = {
                    viewModel.discardPhotos()
                    inspirationOpen = null
                },
            )
        }
    }
}

@Composable
private fun ProgramTab(state: TraditionsUiState, onDone: (String, Boolean) -> Unit, onMemory: (String) -> Unit, onGoToCatalog: () -> Unit) {
    val secondary = LocalMonNoelColors.current.secondaryText
    val planned = state.traditions.filter { it.id in state.program }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val edition = state.edition
        if (edition == null || (planned.isEmpty() && !state.loading)) {
            item {
                EmptyState("🗓️", stringResource(R.string.program_empty_title), stringResource(R.string.program_empty_message)) {
                    OutlinedButton(onClick = onGoToCatalog) { Text(stringResource(R.string.program_go_catalog)) }
                }
            }
            return@LazyColumn
        }
        item {
            val done = planned.count { state.program[it.id]?.done == true }
            Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                Text(
                    stringResource(R.string.program_title, edition.year),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.program_progress, done, planned.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                )
            }
        }
        // À faire : d'abord celles qui ont une date (dans l'ordre), puis par période.
        // Les traditions faites descendent dans « Faites cette année ».
        val (doneList, todo) = planned.partition { state.program[it.id]?.done == true }
        val order = compareBy<Tradition>({ state.program[it.id]?.date ?: LocalDate.MAX }, { it.period.ordinal })
        lazyItems(todo.sortedWith(order), key = { it.id }) { tradition ->
            Box(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                ProgramRow(
                    tradition,
                    state.program.getValue(tradition.id),
                    state.readOnly,
                    onDone = { onDone(tradition.id, it) },
                    onClick = { onMemory(tradition.id) },
                )
            }
        }
        if (doneList.isNotEmpty()) {
            item(key = "done-title") {
                Text(
                    stringResource(R.string.program_done_group, doneList.size),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = secondary,
                    modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 20.dp, bottom = 4.dp).semantics { heading() },
                )
            }
            lazyItems(doneList.sortedWith(order), key = { it.id }) { tradition ->
                Box(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
                    ProgramRow(
                        tradition,
                        state.program.getValue(tradition.id),
                        state.readOnly,
                        onDone = { onDone(tradition.id, it) },
                        onClick = { onMemory(tradition.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TraditionsTab(
    state: TraditionsUiState,
    onOpen: (String) -> Unit,
    onFavorite: (Tradition) -> Unit,
    onPlanned: (String, Boolean) -> Unit,
    onPick: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(320.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        val edition = state.edition
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.catalog_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (state.ideas.isNotEmpty()) {
                    OutlinedButton(onClick = onPick) { Text(stringResource(R.string.ideas_pick, state.ideas.size)) }
                }
            }
        }
        if (state.traditions.isEmpty() && !state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("❤️", stringResource(R.string.traditions_empty_title), stringResource(R.string.traditions_empty_message))
            }
        }
        items(state.traditions, key = { it.id }) { tradition ->
            TraditionCard(
                tradition,
                planned = tradition.id in state.program,
                date = state.program[tradition.id]?.date,
                years = state.doneYears[tradition.id].orEmpty(),
                canPlan = edition != null && !state.readOnly,
                onClick = { onOpen(tradition.id) },
                onFavorite = { onFavorite(tradition) },
                onPlanned = { onPlanned(tradition.id, it) },
            )
        }
    }
}

@Composable
private fun ProgramRow(tradition: Tradition, year: TraditionYear, readOnly: Boolean, onDone: (Boolean) -> Unit, onClick: () -> Unit) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(checked = year.done, onCheckedChange = if (readOnly) null else onDone)
            PhotoImage(
                year.memoryPhotoPath ?: tradition.photoPath,
                null,
                size = 44.dp,
                shape = MaterialTheme.shapes.small,
                placeholder = tradition.category.emoji,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    tradition.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (year.done) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = if (year.done) secondary else MaterialTheme.colorScheme.onSurface,
                )
                val detail = listOfNotNull(
                    year.date?.let { "📅 " + it.format(longDateFormat) },
                    year.memoryNote.ifBlank { null } ?: stringResource(tradition.period.label),
                ).joinToString(" · ")
                Text(detail, style = MaterialTheme.typography.bodySmall, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun TraditionCard(
    tradition: Tradition,
    planned: Boolean,
    date: LocalDate?,
    years: List<Int>,
    canPlan: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onPlanned: (Boolean) -> Unit,
) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(tradition.photoPath, null, size = 96.dp, shape = RectangleShape, placeholder = tradition.category.emoji)
            Column(
                Modifier.weight(1f).padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(tradition.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${stringResource(tradition.category.label)} · ${stringResource(tradition.period.label)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (years.isNotEmpty()) {
                    Text(
                        stringResource(R.string.tradition_done_years, years.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondary,
                    )
                }
                if (canPlan) {
                    FilterChip(
                        selected = planned,
                        onClick = { onPlanned(!planned) },
                        label = {
                            val label = stringResource(if (planned) R.string.tradition_planned else R.string.tradition_plan)
                            Text(date?.let { "$label · 📅 ${it.format(shortDateFormat)}" } ?: label)
                        },
                    )
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (tradition.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(
                        if (tradition.favorite) R.string.tradition_unfavorite else R.string.tradition_favorite,
                    ),
                    tint = if (tradition.favorite) LocalMonNoelColors.current.burgundy else secondary,
                )
            }
        }
    }
}

@Composable
private fun InspirationsTab(
    state: TraditionsUiState,
    kindFilter: InspirationKind?,
    onFilter: (InspirationKind?) -> Unit,
    onOpen: (String) -> Unit,
    onDone: (String, Boolean) -> Unit,
) {
    val visible = state.inspirations.filter { kindFilter == null || it.kind == kindFilter }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kindFilter == null, onClick = {
                    onFilter(null)
                }, label = { Text(stringResource(R.string.filter_everything)) })
                InspirationKind.entries.forEach { k ->
                    FilterChip(selected = kindFilter == k, onClick = {
                        onFilter(k)
                    }, label = { Text("${k.emoji} ${stringResource(k.label)}") })
                }
            }
        }
        if (state.inspirations.isEmpty() && !state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("🎬", stringResource(R.string.inspirations_empty_title), stringResource(R.string.inspirations_empty_message))
            }
        }
        items(visible, key = { it.id }) { inspiration ->
            InspirationCard(
                inspiration,
                done = inspiration.id in state.inspirationsDone,
                date = state.inspirationDates[inspiration.id],
                canCheck = state.edition != null && !state.readOnly,
                onClick = { onOpen(inspiration.id) },
                onDone = { onDone(inspiration.id, it) },
            )
        }
    }
}

@Composable
private fun InspirationCard(
    inspiration: Inspiration,
    done: Boolean,
    date: LocalDate?,
    canCheck: Boolean,
    onClick: () -> Unit,
    onDone: (Boolean) -> Unit,
) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(inspiration.photoPath, null, size = 84.dp, shape = RectangleShape, placeholder = inspiration.kind.emoji)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(inspiration.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        stringResource(inspiration.kind.label),
                        date?.let {
                            "📅 " + it.format(shortDateFormat)
                        },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                )
                if (canCheck) {
                    FilterChip(
                        selected = done,
                        onClick = { onDone(!done) },
                        label = { Text(stringResource(if (done) R.string.inspiration_done else R.string.inspiration_todo)) },
                    )
                }
            }
        }
    }
}

/** Date proposée à l'ouverture du calendrier : aujourd'hui pendant la saison, sinon le 1er décembre. */
internal fun defaultDateFor(year: Int): LocalDate {
    val today = LocalDate.now()
    val start = LocalDate.of(year, 12, 1)
    return if (today.year == year && today.isAfter(start)) today else start
}
