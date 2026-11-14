package com.aktawind.monnoel.feature.gifts

import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.icon.MonNoelIcons
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.ui.EmptyState
import com.aktawind.monnoel.core.ui.PersonAvatar
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.resolve
import com.aktawind.monnoel.feature.gifts.components.EnvelopeProgress
import com.aktawind.monnoel.feature.gifts.components.GiftRow
import com.aktawind.monnoel.feature.gifts.components.label

private enum class GiftsTab(@StringRes val label: Int) {
    GIFTS(
        R.string.tab_gifts,
    ),
    PERSONS(R.string.tab_persons),
    IDEAS(R.string.tab_ideas),
}

private enum class Grouping(@StringRes val label: Int) {
    PERSON(R.string.group_person),
    STATUS(R.string.group_status),
    CATEGORY(R.string.group_category),
}

private enum class QuickFilter(@StringRes val label: Int, val accept: (PlannedGift) -> Boolean) {
    ALL(R.string.filter_all, { true }),
    PAID(R.string.filter_paid, { it.status.isPaid }),
    TO_WRAP(R.string.filter_to_wrap, { it.status == GiftStatus.TO_WRAP }),
    NOT_RECEIVED(R.string.filter_not_received, { it.status.isAwaitingDelivery }),
}

@Composable
fun GiftsRoute(
    onOpenGift: (String) -> Unit,
    onNewGift: () -> Unit,
    onOpenPerson: (String) -> Unit,
    onNewPerson: () -> Unit,
    onOpenIdea: (String) -> Unit,
    onNewIdea: () -> Unit,
    viewModel: GiftsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GiftsScreen(
        state = state,
        onOpenGift = onOpenGift,
        onNewGift = onNewGift,
        onOpenPerson = onOpenPerson,
        onNewPerson = onNewPerson,
        onOpenIdea = onOpenIdea,
        onNewIdea = onNewIdea,
        onStatus = viewModel::setStatus,
        onOffer = viewModel::offer,
        onMessageShown = viewModel::messageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GiftsScreen(
    state: GiftsUiState,
    onOpenGift: (String) -> Unit,
    onNewGift: () -> Unit,
    onOpenPerson: (String) -> Unit,
    onNewPerson: () -> Unit,
    onOpenIdea: (String) -> Unit,
    onNewIdea: () -> Unit,
    onStatus: (String, GiftStatus) -> Unit,
    onOffer: (String, String?) -> Unit,
    onMessageShown: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(GiftsTab.GIFTS) }
    var offering by remember { mutableStateOf<GiftIdea?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it.resolve(context))
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            state.edition?.let { stringResource(R.string.gifts_title_year, it.year) }
                                ?: stringResource(R.string.gifts_title),
                        )
                    },
                )
                PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                    GiftsTab.entries.forEach { t ->
                        Tab(selected = tab == t, onClick = { tab = t }, text = { Text(stringResource(t.label)) })
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            val (label, action) = when (tab) {
                GiftsTab.GIFTS -> R.string.fab_new_gift to onNewGift
                GiftsTab.PERSONS -> R.string.fab_new_person to onNewPerson
                GiftsTab.IDEAS -> R.string.fab_new_idea to onNewIdea
            }
            if (!(tab == GiftsTab.GIFTS && state.readOnly)) {
                ExtendedFloatingActionButton(
                    onClick = action,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(label)) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                GiftsTab.GIFTS -> GiftsTabContent(state, onOpenGift, onStatus)
                GiftsTab.PERSONS -> PersonsTabContent(state, onOpenPerson)
                GiftsTab.IDEAS -> IdeasTabContent(state, onOpenIdea, onOffer = { offering = it })
            }
        }
    }

    offering?.let { idea ->
        OfferDialog(
            idea = idea,
            persons = state.persons,
            onPick = { personId ->
                onOffer(idea.id, personId)
                offering = null
            },
            onDismiss = { offering = null },
        )
    }
}

@Composable
private fun GiftsTabContent(state: GiftsUiState, onOpenGift: (String) -> Unit, onStatus: (String, GiftStatus) -> Unit) {
    var grouping by rememberSaveable { mutableStateOf(Grouping.PERSON) }
    var filter by rememberSaveable { mutableStateOf(QuickFilter.ALL) }
    val categoryNames = state.categories.associate { it.id to it.name }
    val noCategory = stringResource(R.string.no_category)
    val noRecipient = stringResource(R.string.no_recipient)
    val visible = state.gifts.filter(filter.accept)
    val groups: List<Pair<String, List<PlannedGift>>> = when (grouping) {
        Grouping.PERSON -> visible.groupBy { it.personId }
            .map { (personId, list) ->
                (
                    state.persons.firstOrNull { it.id == personId }?.displayName
                        ?: list.first().recipientName.ifBlank { noRecipient }
                    ) to
                    list
            }
            .sortedBy { it.first.lowercase() }
        Grouping.STATUS -> GiftStatus.entries.mapNotNull { status ->
            visible.filter { it.status == status }.takeIf { it.isNotEmpty() }?.let { status to it }
        }.map { (status, list) -> "" + status.ordinal to list }
        Grouping.CATEGORY -> visible.groupBy { it.categoryId }
            .map { (id, list) -> (id?.let(categoryNames::get) ?: noCategory) to list }
            .sortedBy { it.first.lowercase() }
    }
    val statusLabels = GiftStatus.entries.map { stringResource(it.label) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
    ) {
        if (state.readOnly) item { ReadOnlyBanner() }
        item { BudgetCard(state) }
        item {
            ChipRow(Modifier.padding(top = 12.dp)) {
                GroupingSelector(grouping, onSelect = { grouping = it })
                QuickFilter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = {
                        filter = f
                    }, label = { Text(stringResource(f.label)) })
                }
            }
        }
        if (state.gifts.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    emoji = "🎁",
                    title = stringResource(R.string.gifts_empty_title),
                    message = stringResource(R.string.gifts_empty_message),
                )
            }
        } else if (visible.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.gifts_filter_empty),
                    color = LocalMonNoelColors.current.secondaryText,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        groups.forEachIndexed { index, (title, list) ->
            item(key = "header-$grouping-$title") {
                val header = if (grouping == Grouping.STATUS) statusLabels[title.toInt()] else title
                val personId = if (grouping == Grouping.PERSON) list.first().personId else null
                GroupHeader(
                    title = header,
                    count = list.size,
                    person = personId?.let { id -> state.persons.firstOrNull { it.id == id } },
                    budget = personId?.let { state.budget.byPerson[it] },
                    showAvatar = grouping == Grouping.PERSON,
                    first = index == 0,
                )
            }
            items(list, key = { it.id }) { gift ->
                GiftRow(
                    gift = gift,
                    showRecipient = grouping != Grouping.PERSON,
                    readOnly = state.readOnly,
                    onClick = { onOpenGift(gift.id) },
                    onStatus = { onStatus(gift.id, it) },
                    showDivider = gift.id != list.last().id,
                )
            }
        }
    }
}

@Composable
private fun ChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun ReadOnlyBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Text(stringResource(R.string.gifts_read_only), modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun BudgetCard(state: GiftsUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            BudgetFigure(R.string.budget_planned, formatEuro(state.budget.plannedCents))
            BudgetFigure(R.string.budget_spent, formatEuro(state.budget.spentCents))
            BudgetFigure(R.string.budget_envelopes, formatEuro(state.budget.envelopesCents))
            BudgetFigure(R.string.budget_count, state.gifts.size.toString())
        }
    }
}

@Composable
private fun BudgetFigure(@StringRes label: Int, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** En-tête de groupe sobre : pas d'encadré, juste un titre aéré (et la personne). */
@Composable
private fun GroupHeader(
    title: String,
    count: Int,
    person: Person?,
    budget: com.aktawind.monnoel.core.domain.gifts.PersonGiftBudget?,
    showAvatar: Boolean,
    first: Boolean,
) {
    Column(
        Modifier.padding(top = if (first) 20.dp else 36.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showAvatar) PersonAvatar(person?.photoPath, person?.initials ?: "", size = 40.dp)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(
                pluralStringResource(R.plurals.group_count, count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalMonNoelColors.current.secondaryText,
            )
        }
        if (showAvatar && budget != null) EnvelopeProgress(budget, Modifier.fillMaxWidth())
    }
}

/** Choix du regroupement, en liste déroulante compacte. */
@Composable
private fun GroupingSelector(selected: Grouping, onSelect: (Grouping) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = true,
            onClick = { open = true },
            label = { Text(stringResource(R.string.group_label, stringResource(selected.label))) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Grouping.entries.forEach { g ->
                DropdownMenuItem(
                    text = { Text(stringResource(g.label)) },
                    onClick = {
                        onSelect(g)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PersonsTabContent(state: GiftsUiState, onOpenPerson: (String) -> Unit) {
    if (state.persons.isEmpty() && !state.loading) {
        EmptyState(
            emoji = "🧑‍🎄",
            title = stringResource(R.string.persons_empty_title),
            message = stringResource(R.string.persons_empty_message),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.persons, key = { it.id }) { person ->
            PersonCard(person, state.budget.byPerson[person.id], onClick = { onOpenPerson(person.id) })
        }
    }
}

@Composable
private fun PersonCard(
    person: Person,
    budget: com.aktawind.monnoel.core.domain.gifts.PersonGiftBudget?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonAvatar(person.photoPath, person.initials, size = 84.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    person.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.person_gift_count, budget?.giftCount ?: 0),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMonNoelColors.current.secondaryText,
                )
                EnvelopeProgress(budget)
            }
        }
    }
}

@Composable
private fun IdeasTabContent(state: GiftsUiState, onOpenIdea: (String) -> Unit, onOffer: (GiftIdea) -> Unit) {
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = state.ideas.filter { categoryFilter == null || it.categoryId == categoryFilter }
    val personNames = state.persons.associate { it.id to it.firstName.ifBlank { it.displayName } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth {
            ChipRow {
                FilterChip(selected = categoryFilter == null, onClick = {
                    categoryFilter = null
                }, label = { Text(stringResource(R.string.filter_all)) })
                state.categories.filter { c -> state.ideas.any { it.categoryId == c.id } }.forEach { c ->
                    FilterChip(selected = categoryFilter == c.id, onClick = {
                        categoryFilter = c.id
                    }, label = { Text(c.name) })
                }
            }
        }
        if (state.ideas.isEmpty() && !state.loading) {
            fullWidth {
                EmptyState(
                    emoji = "💡",
                    title = stringResource(R.string.ideas_empty_title),
                    message = stringResource(R.string.ideas_empty_message),
                )
            }
        }
        items(visible, key = { it.id }) { idea ->
            IdeaCard(
                idea = idea,
                forNames = idea.personIds.mapNotNull(personNames::get).sorted(),
                canOffer = !state.readOnly && state.edition != null,
                onClick = { onOpenIdea(idea.id) },
                onOffer = { onOffer(idea) },
            )
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** Carte d'idée compacte : beaucoup d'idées tiennent à l'écran, la photo reste bien visible. */
@Composable
private fun IdeaCard(
    idea: GiftIdea,
    forNames: List<String>,
    canOffer: Boolean,
    onClick: () -> Unit,
    onOffer: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(idea.photoPath, null, size = 84.dp, shape = RectangleShape, placeholder = "💡")
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    idea.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = listOfNotNull(
                    idea.estimatedPriceCents?.let { formatEuro(it) },
                    forNames.takeIf {
                        it.isNotEmpty()
                    }?.let { stringResource(R.string.idea_for, it.joinToString(", ")) },
                ).joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(
                        details,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalMonNoelColors.current.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (canOffer) {
                IconButton(onClick = onOffer) {
                    Icon(MonNoelIcons.Gift, contentDescription = stringResource(R.string.idea_offer_to, idea.title))
                }
            }
        }
    }
}

@Composable
private fun OfferDialog(idea: GiftIdea, persons: List<Person>, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val suggested = persons.sortedByDescending { it.id in idea.personIds }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.offer_title, idea.title)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(suggested, key = { it.id }) { person ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(person.id) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PersonAvatar(person.photoPath, person.initials, size = 40.dp)
                        Text(person.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                item {
                    TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.offer_no_recipient)) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_cancel))
            }
        },
    )
}
