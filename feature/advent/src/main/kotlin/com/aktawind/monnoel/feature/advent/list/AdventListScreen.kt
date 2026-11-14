package com.aktawind.monnoel.feature.advent.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.AdventTemplate
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.ui.EmptyState
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.core.ui.resolve
import com.aktawind.monnoel.feature.advent.R

private enum class NewChoice { EMPTY, TEMPLATE, COPY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventListRoute(onOpenCalendar: (String) -> Unit, onNewCalendar: () -> Unit, viewModel: AdventListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var choosing by rememberSaveable { mutableStateOf(false) }
    var pickTemplate by rememberSaveable { mutableStateOf(false) }
    var pickCopy by rememberSaveable { mutableStateOf(false) }
    var fromTemplate by remember { mutableStateOf<AdventTemplate?>(null) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it.resolve(context))
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.edition?.let {
                            stringResource(R.string.advent_title_year, it.year)
                        } ?: stringResource(R.string.advent_title),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (!state.readOnly && state.edition != null) {
                ExtendedFloatingActionButton(
                    onClick = { if (state.templates.isEmpty() && state.others.isEmpty()) onNewCalendar() else choosing = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.advent_new)) },
                )
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.cards.isEmpty() && !state.loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        emoji = "🎄",
                        title = stringResource(R.string.advent_empty_title),
                        message = stringResource(R.string.advent_empty_message),
                    )
                }
            }
            items(state.cards, key = { it.calendar.id }) { card -> CalendarCardView(card, onClick = { onOpenCalendar(card.calendar.id) }) }
            if (state.templates.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { TemplatesSection(state.templates, viewModel::deleteTemplate) }
            }
        }
    }

    if (choosing) {
        NewCalendarChoiceDialog(
            hasTemplates = state.templates.isNotEmpty(),
            hasOthers = state.others.isNotEmpty(),
            onChoose = { choice ->
                choosing = false
                when (choice) {
                    NewChoice.EMPTY -> onNewCalendar()
                    NewChoice.TEMPLATE -> pickTemplate = true
                    NewChoice.COPY -> pickCopy = true
                }
            },
            onDismiss = { choosing = false },
        )
    }
    if (pickTemplate) {
        PickDialog(
            title = stringResource(R.string.advent_pick_template),
            items = state.templates.map { it.id to "${it.name} · ${stringResource(R.string.advent_boxes_count, it.boxCount)}" },
            onPick = { id ->
                pickTemplate = false
                fromTemplate = state.templates.firstOrNull { it.id == id }
            },
            onDismiss = { pickTemplate = false },
        )
    }
    fromTemplate?.let { template ->
        FromTemplateDialog(
            template = template,
            persons = state.persons,
            onCreate = { name, personIds ->
                viewModel.createFromTemplate(template.id, name, personIds)
                fromTemplate = null
            },
            onDismiss = { fromTemplate = null },
        )
    }
    if (pickCopy) {
        PickDialog(
            title = stringResource(R.string.advent_pick_copy),
            items = state.others.map { it.calendar.id to stringResource(R.string.advent_other_label, it.calendar.name, it.year) },
            onPick = { id ->
                pickCopy = false
                viewModel.copyCalendar(id)
            },
            onDismiss = { pickCopy = false },
        )
    }
}

@Composable
private fun CalendarCardView(card: CalendarCard, onClick: () -> Unit) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(card.calendar.coverPhotoPath, null, size = 104.dp, shape = RectangleShape, placeholder = "🎄")
            Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(card.calendar.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val details = listOfNotNull(
                    card.personNames.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.advent_for, it.joinToString(", ")) },
                    card.calendar.theme.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(
                        details,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    stringResource(R.string.advent_progress, card.summary.ready, card.summary.boxCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { card.summary.progress },
                    color = LocalMonNoelColors.current.gold,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TemplatesSection(templates: List<AdventTemplate>, onDelete: (String) -> Unit) {
    var toDelete by remember { mutableStateOf<AdventTemplate?>(null) }
    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider(color = LocalMonNoelColors.current.gold.copy(alpha = 0.35f), modifier = Modifier.padding(bottom = 16.dp))
        Text(
            stringResource(R.string.advent_templates_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics {
                heading()
            },
        )
        Text(
            stringResource(R.string.advent_templates_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LocalMonNoelColors.current.secondaryText,
        )
        templates.forEach { template ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "📐 ${template.name} · ${stringResource(R.string.advent_boxes_count, template.boxCount)}",
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { toDelete = template }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.advent_template_delete, template.name))
                }
            }
        }
    }
    toDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.advent_template_delete, template.name)) },
            text = { Text(stringResource(R.string.advent_template_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(template.id)
                    toDelete = null
                }) { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun NewCalendarChoiceDialog(hasTemplates: Boolean, hasOthers: Boolean, onChoose: (NewChoice) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.advent_new)) },
        text = {
            Column {
                ChoiceRow("✨", stringResource(R.string.advent_new_empty)) { onChoose(NewChoice.EMPTY) }
                if (hasTemplates) ChoiceRow("📐", stringResource(R.string.advent_new_template)) { onChoose(NewChoice.TEMPLATE) }
                if (hasOthers) ChoiceRow("📋", stringResource(R.string.advent_new_copy)) { onChoose(NewChoice.COPY) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_cancel)) } },
    )
}

@Composable
private fun ChoiceRow(emoji: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PickDialog(title: String, items: List<Pair<String, String>>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(items, key = { it.first }) { (id, label) ->
                    Text(label, Modifier.fillMaxWidth().clickable { onPick(id) }.padding(vertical = 14.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FromTemplateDialog(
    template: AdventTemplate,
    persons: List<Person>,
    onCreate: (String, Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.advent_from_template_title, template.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput(name, { name = it }, stringResource(R.string.field_calendar_name), Modifier.fillMaxWidth())
                if (persons.isNotEmpty()) {
                    Text(stringResource(R.string.field_for_whom), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        persons.forEach { p ->
                            FilterChip(
                                selected = p.id in selected,
                                onClick = { selected = if (p.id in selected) selected - p.id else selected + p.id },
                                label = { Text(p.displayName) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.ifBlank { template.name }, selected.toSet()) }) {
                Text(stringResource(R.string.advent_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(com.aktawind.monnoel.core.ui.R.string.action_cancel)) } },
    )
}
