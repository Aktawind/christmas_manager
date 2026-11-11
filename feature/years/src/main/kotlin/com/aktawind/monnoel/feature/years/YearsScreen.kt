package com.aktawind.monnoel.feature.years

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.ui.EmptyState
import com.aktawind.monnoel.core.ui.resolve

@Composable
fun YearsRoute(onYearSelected: () -> Unit, viewModel: YearsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    YearsScreen(
        state = state,
        onCreate = viewModel::create,
        onSelect = {
            viewModel.select(it.id)
            onYearSelected()
        },
        onArchive = viewModel::setArchived,
        onDelete = viewModel::delete,
        onMessageShown = viewModel::messageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YearsScreen(
    state: YearsUiState,
    onCreate: (Int) -> Unit,
    onSelect: (ChristmasEdition) -> Unit,
    onArchive: (ChristmasEdition, Boolean) -> Unit,
    onDelete: (ChristmasEdition) -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ChristmasEdition?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it.resolve(context))
            onMessageShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.years_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.years_new)) },
            )
        },
    ) { padding ->
        if (!state.loading && state.editions.isEmpty()) {
            EmptyState(
                emoji = "🎄",
                title = stringResource(R.string.years_empty_title),
                message = stringResource(R.string.years_empty_message),
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(state.editions, key = { it.id }) { edition ->
                    EditionCard(
                        edition = edition,
                        isCurrent = edition.id == state.currentId,
                        onSelect = { onSelect(edition) },
                        onArchive = { onArchive(edition, !edition.isArchived) },
                        onDelete = { toDelete = edition },
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateYearDialog(
            suggestedYear = state.suggestedYear,
            onCreate = {
                onCreate(it)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }
    toDelete?.let { edition ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.years_delete_title, edition.year)) },
            text = { Text(stringResource(R.string.years_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(edition)
                    toDelete = null
                }) { Text(stringResource(R.string.years_delete_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.years_cancel)) }
            },
        )
    }
}

@Composable
private fun EditionCard(
    edition: ChristmasEdition,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        onClick = onSelect,
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            Modifier.padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.years_edition, edition.year),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isCurrent) {
                        AssistChip(onClick = onSelect, label = {
                            Text(stringResource(R.string.years_current))
                        })
                    }
                    if (edition.isArchived) {
                        AssistChip(onClick = onArchive, label = {
                            Text(stringResource(R.string.years_status_archived))
                        })
                    }
                }
                if (!isCurrent) {
                    Text(
                        stringResource(R.string.years_tap_to_show),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalMonNoelColors.current.secondaryText,
                    )
                }
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.years_actions, edition.year))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (edition.isArchived) R.string.years_unarchive else R.string.years_archive,
                            ),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onArchive()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.years_delete), color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}
