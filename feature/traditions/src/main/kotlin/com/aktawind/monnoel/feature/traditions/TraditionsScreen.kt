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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.Inspiration
import com.aktawind.monnoel.core.model.InspirationKind
import com.aktawind.monnoel.core.model.Tradition
import com.aktawind.monnoel.core.model.TraditionYear
import com.aktawind.monnoel.core.ui.EmptyState
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.core.ui.resolve

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraditionsRoute(onOpenTradition: (String) -> Unit, onNewTradition: () -> Unit, viewModel: TraditionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 0 : au programme ; 1 : nos traditions ; 2 : inspirations.
    var tab by rememberSaveable { mutableStateOf(if (viewModel.initialInspirationId != null) 2 else 0) }
    var memoryFor by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
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
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_program)) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_traditions)) })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.tab_inspirations)) })
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (tab != 0) {
                ExtendedFloatingActionButton(
                    onClick = { if (tab == 1) onNewTradition() else inspirationOpen = "" },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(if (tab == 1) R.string.fab_tradition else R.string.fab_inspiration)) },
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
                        viewModel.saveMemory(id, done, year?.memoryPhotoPath, year?.memoryNote.orEmpty())
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
                else -> InspirationsTab(state, onOpen = {
                    inspirationOpen = it
                }, onDone = { id, done -> viewModel.setInspirationDone(id, done) })
            }
        }
    }

    memoryFor?.let { id ->
        val tradition = state.traditions.firstOrNull { it.id == id }
        if (tradition != null) {
            MemoryDialog(
                tradition = tradition,
                year = state.program[id],
                onImportPhoto = viewModel::importPhoto,
                onSave = { done, photo, note ->
                    viewModel.saveMemory(id, done, photo, note)
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
    inspirationOpen?.let { id ->
        val inspiration = state.inspirations.firstOrNull { it.id == id }
        if (id.isEmpty() || inspiration != null) {
            InspirationDialog(
                inspiration = inspiration,
                onImportPhoto = viewModel::importPhoto,
                onSave = { draft ->
                    viewModel.saveInspiration(inspiration?.id, draft)
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
        // Rangées par période : l'Avent, le réveillon, le jour de Noël, après Noël.
        lazyItems(planned.sortedBy { it.period.ordinal }, key = { it.id }) { tradition ->
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
                Text(tradition.name, style = MaterialTheme.typography.bodyLarge)
                val detail = year.memoryNote.ifBlank { stringResource(tradition.period.label) }
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
                        label = { Text(stringResource(if (planned) R.string.tradition_planned else R.string.tradition_plan)) },
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
private fun InspirationsTab(state: TraditionsUiState, onOpen: (String) -> Unit, onDone: (String, Boolean) -> Unit) {
    var kindFilter by rememberSaveable { mutableStateOf<InspirationKind?>(null) }
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
                    kindFilter = null
                }, label = { Text(stringResource(R.string.filter_everything)) })
                InspirationKind.entries.forEach { k ->
                    FilterChip(selected = kindFilter == k, onClick = {
                        kindFilter = k
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
                canCheck = state.edition != null && !state.readOnly,
                onClick = { onOpen(inspiration.id) },
                onDone = { onDone(inspiration.id, it) },
            )
        }
    }
}

@Composable
private fun InspirationCard(inspiration: Inspiration, done: Boolean, canCheck: Boolean, onClick: () -> Unit, onDone: (Boolean) -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(inspiration.photoPath, null, size = 84.dp, shape = RectangleShape, placeholder = inspiration.kind.emoji)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(inspiration.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(inspiration.kind.label),
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
