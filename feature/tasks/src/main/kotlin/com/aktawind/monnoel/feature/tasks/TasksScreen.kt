package com.aktawind.monnoel.feature.tasks

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.tasks.Suggestion
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.LutinTask
import com.aktawind.monnoel.core.model.TaskModule
import com.aktawind.monnoel.core.model.TaskStatus
import com.aktawind.monnoel.core.ui.EmptyState
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.core.ui.resolve

private enum class TaskFilter(@StringRes val label: Int, val accept: (LutinTask) -> Boolean) {
    OPEN(R.string.filter_open, { it.isOpen }),
    DONE(R.string.filter_done, { it.status == TaskStatus.DONE || it.status == TaskStatus.CANCELLED }),
    ALL(R.string.filter_all, { true }),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksRoute(
    onOpenTask: (String) -> Unit,
    onNewTask: (TaskModule) -> Unit,
    onOpenLink: (LinkType, String) -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(TaskFilter.OPEN) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it.resolve(context))
            viewModel.messageShown()
        }
    }
    val lutin = state.lutin
    // Un onglet par thème : ce qui reste à faire (tâches ouvertes + suggestions) en compteur.
    val pending = TaskModule.entries.associateWith { m ->
        lutin.tasks.count { it.module == m && it.isOpen } + lutin.suggestions.count { it.module == m }
    }
    var tabName by rememberSaveable { mutableStateOf<String?>(null) }
    val tab = tabName?.let { n -> TaskModule.entries.firstOrNull { it.name == n } }
        ?: TaskModule.entries.firstOrNull { (pending[it] ?: 0) > 0 }
        ?: TaskModule.GIFTS
    val labeled = lutin.suggestions.filter { it.module == tab }.map { it to it.label() }
    val visible = lutin.tasks.filter { it.module == tab }.filter(filter.accept)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            lutin.edition?.let { stringResource(R.string.tasks_title_year, it.year) }
                                ?: stringResource(R.string.tasks_title),
                        )
                    },
                )
                PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 16.dp) {
                    TaskModule.entries.forEach { m ->
                        val count = pending[m] ?: 0
                        Tab(
                            selected = tab == m,
                            onClick = { tabName = m.name },
                            text = {
                                Text(
                                    "${m.emoji}  ${stringResource(m.label)}" + if (count > 0) " · $count" else "",
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (!state.readOnly && lutin.edition != null) {
                ExtendedFloatingActionButton(
                    onClick = { onNewTask(tab) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.tasks_new)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (labeled.isNotEmpty()) {
                item {
                    SuggestionsCard(
                        labeled = labeled,
                        onAccept = viewModel::accept,
                        onDismiss = viewModel::dismiss,
                        onAcceptAll = { viewModel.acceptAll(labeled) },
                        onOpenLink = onOpenLink,
                    )
                }
            }
            item {
                Row(
                    Modifier.widthIn(
                        max = 840.dp,
                    ).fillMaxWidth().padding(top = 16.dp, bottom = 4.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TaskFilter.entries.forEach { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(stringResource(f.label)) })
                    }
                }
            }
            if (lutin.tasks.none { it.module == tab } && !state.loading) {
                item {
                    EmptyState(
                        emoji = "📝",
                        title = stringResource(R.string.tasks_tab_empty_title, stringResource(tab.label)),
                        message = stringResource(R.string.tasks_empty_message),
                    )
                }
            } else if (visible.isEmpty() && !state.loading) {
                item {
                    Text(
                        stringResource(if (filter == TaskFilter.OPEN) R.string.tasks_all_done else R.string.tasks_filter_empty),
                        color = LocalMonNoelColors.current.secondaryText,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            item { Spacer(Modifier.padding(top = 8.dp)) }
            items(visible, key = { it.id }) { task ->
                TaskRow(task, state.readOnly, onToggle = { viewModel.toggle(task.id, it) }, onClick = { onOpenTask(task.id) })
            }
        }
    }
}

@Composable
private fun SuggestionsCard(
    labeled: List<Pair<Suggestion, String>>,
    onAccept: (Suggestion, String) -> Unit,
    onDismiss: (Suggestion) -> Unit,
    onAcceptAll: () -> Unit,
    onOpenLink: (LinkType, String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.suggestions_title, labeled.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (labeled.size > 1) TextButton(onClick = onAcceptAll) { Text(stringResource(R.string.suggestions_accept_all)) }
            }
            Text(
                stringResource(R.string.suggestions_hint),
                style = MaterialTheme.typography.bodySmall,
                color = LocalMonNoelColors.current.secondaryText,
            )
            labeled.forEach { (suggestion, title) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${suggestion.module.emoji}  $title",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).clickable {
                            onOpenLink(suggestion.linkType, suggestion.linkId)
                        }.padding(vertical = 8.dp),
                    )
                    FilledTonalButton(onClick = { onAccept(suggestion, title) }) { Text(stringResource(R.string.suggestions_accept)) }
                    IconButton(onClick = { onDismiss(suggestion) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.suggestions_dismiss, title))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: LutinTask, readOnly: Boolean, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val finished = task.status == TaskStatus.DONE || task.status == TaskStatus.CANCELLED
    val secondary = LocalMonNoelColors.current.secondaryText
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.status == TaskStatus.DONE, onCheckedChange = if (readOnly) null else onToggle)
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (finished) TextDecoration.LineThrough else null,
                    color = if (finished) secondary else MaterialTheme.colorScheme.onSurface,
                )
                val details = listOfNotNull(
                    task.status.takeIf { it == TaskStatus.IN_PROGRESS || it == TaskStatus.CANCELLED }?.let { stringResource(it.label) },
                    task.suggestionKey?.let { stringResource(R.string.task_auto) },
                    task.description.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, color = secondary, maxLines = 1)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
