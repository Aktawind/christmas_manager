package com.aktawind.monnoel.feature.advent.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.advent.BulkBoxChange
import com.aktawind.monnoel.core.domain.advent.SupplyDraft
import com.aktawind.monnoel.core.domain.gifts.centsToInput
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.domain.gifts.parseEuroToCents
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.SurpriseType
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.MoneyInput
import com.aktawind.monnoel.core.ui.PhotoField
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.R as UiR
import com.aktawind.monnoel.core.ui.TextInput
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.core.ui.resolve
import com.aktawind.monnoel.feature.advent.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarRoute(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenBox: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var askTemplate by rememberSaveable { mutableStateOf(false) }
    var editingSupply by remember { mutableStateOf<AdventSupply?>(null) }
    var newSupply by rememberSaveable { mutableStateOf(false) }
    var onlyToPrepare by rememberSaveable { mutableStateOf(false) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var bulkOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it.resolve(context))
            viewModel.messageShown()
        }
    }
    LaunchedEffect(state.deleted, state.loading, state.calendar) {
        if (state.deleted || (!state.loading && state.calendar == null)) onBack()
    }
    val calendar = state.calendar ?: return

    fun closeSelection() {
        selecting = false
        selected = emptyList()
    }
    BackHandler(enabled = selecting) { closeSelection() }

    Scaffold(
        topBar = {
            if (selecting) {
                SelectionBar(
                    count = selected.size,
                    onAll = { selected = state.boxes.map { it.id } },
                    onEmpty = { selected = state.boxes.filter { it.isEmpty }.map { it.id } },
                    onEdit = { bulkOpen = true },
                    onClose = ::closeSelection,
                )
                return@Scaffold
            }
            TopAppBar(
                title = { Text(calendar.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.action_back))
                    }
                },
                actions = {
                    if (!state.readOnly) {
                        IconButton(onClick = { onEdit(calendar.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(UiR.string.action_edit))
                        }
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.calendar_more))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.calendar_save_template)) },
                                onClick = {
                                    menu = false
                                    askTemplate = true
                                },
                            )
                            if (!state.readOnly) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(UiR.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menu = false
                                        confirmDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val columns = when {
                maxWidth >= 840.dp -> 6
                maxWidth >= 480.dp -> 4
                else -> 3
            }
            val visible = if (onlyToPrepare) state.boxes.filter { it.status != BoxStatus.READY } else state.boxes
            BoxGrid(
                columns = columns,
                boxes = visible,
                shares = state.shares,
                supplyPhotos = state.supplies.associate { it.id to it.photoPath },
                canSelect = !state.readOnly,
                canDrag = !state.readOnly && !onlyToPrepare,
                selected = if (selecting) selected.toSet() else null,
                onOpenBox = { id ->
                    if (selecting) {
                        selected = if (id in selected) selected - id else selected + id
                        if (selected.isEmpty()) selecting = false
                    } else {
                        onOpenBox(id)
                    }
                },
                // Appui long : la case est sélectionnée (mode sélection) ; si l'on glisse, elle se déplace.
                onLongPress = { id ->
                    val added = id !in selected
                    selecting = true
                    if (added) selected = selected + id
                    added
                },
                onLongPressBecameDrag = { id ->
                    selected = selected - id
                    if (selected.isEmpty()) selecting = false
                },
                onSwap = viewModel::swap,
                header = {
                    CalendarHeader(
                        state = state,
                        onlyToPrepare = onlyToPrepare,
                        onToggleFilter = { onlyToPrepare = !onlyToPrepare },
                    )
                },
                footer = {
                    SuppliesSection(
                        supplies = state.supplies,
                        boxes = state.boxes,
                        readOnly = state.readOnly,
                        onToggle = viewModel::toggleBought,
                        onEdit = { editingSupply = it },
                        onAdd = { newSupply = true },
                    )
                },
            )
        }
    }

    if (newSupply || editingSupply != null) {
        SupplyDialog(
            supply = editingSupply,
            boxes = state.boxes,
            onSave = { draft, boxIds ->
                viewModel.saveSupply(editingSupply?.id, draft, boxIds)
                newSupply = false
                editingSupply = null
            },
            onImportPhoto = viewModel::importSupplyPhoto,
            onDelete = editingSupply?.let { s ->
                {
                    viewModel.removeSupply(s.id)
                    editingSupply = null
                }
            },
            onDismiss = {
                viewModel.discardSupplyPhotos()
                newSupply = false
                editingSupply = null
            },
        )
    }
    if (bulkOpen) {
        BulkEditDialog(
            count = selected.size,
            supplies = state.supplies,
            onApply = { change ->
                viewModel.bulkUpdate(selected.toSet(), change)
                bulkOpen = false
                selecting = false
                selected = emptyList()
            },
            onDismiss = { bulkOpen = false },
        )
    }
    if (askTemplate) {
        var name by rememberSaveable { mutableStateOf(calendar.theme.ifBlank { calendar.name }) }
        AlertDialog(
            onDismissRequest = { askTemplate = false },
            title = { Text(stringResource(R.string.calendar_save_template)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.calendar_save_template_hint))
                    TextInput(name, { name = it }, stringResource(R.string.calendar_template_name), Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveTemplate(name)
                    askTemplate = false
                }, enabled = name.isNotBlank()) { Text(stringResource(UiR.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { askTemplate = false }) { Text(stringResource(UiR.string.action_cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.calendar_delete_title, calendar.name)) },
            text = { Text(stringResource(R.string.calendar_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(UiR.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(UiR.string.action_cancel)) } },
        )
    }
}

@Composable
private fun CalendarHeader(
    state: CalendarUiState,
    onlyToPrepare: Boolean,
    onToggleFilter: () -> Unit,
) {
    val calendar = state.calendar ?: return
    val summary = state.summary
    val secondary = LocalMonNoelColors.current.secondaryText
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.readOnly) Text(stringResource(R.string.calendar_read_only), color = MaterialTheme.colorScheme.tertiary)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(calendar.coverPhotoPath, null, size = 88.dp, placeholder = "🎄")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val line = listOfNotNull(
                    state.personNames.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.advent_for, it.joinToString(", ")) },
                    calendar.theme.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (line.isNotBlank()) Text(line, style = MaterialTheme.typography.bodyLarge)
                if (calendar.description.isNotBlank()) {
                    Text(
                        calendar.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondary,
                    )
                }
                if (summary != null) {
                    Text(
                        stringResource(R.string.calendar_status_line, summary.ready, summary.boxCount, summary.toPrepare, summary.toFind),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(
                        progress = { summary.progress },
                        color = LocalMonNoelColors.current.gold,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val budget = summary.budgetCents?.let { stringResource(R.string.calendar_budget_of, formatEuro(it)) }.orEmpty()
                    Text(
                        stringResource(R.string.calendar_budget_line, formatEuro(summary.plannedCents), formatEuro(summary.spentCents)) +
                            budget,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondary,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = onlyToPrepare, onClick = onToggleFilter, label = {
                Text(stringResource(R.string.calendar_filter_to_prepare))
            })
        }
    }
}

/**
 * Grille des cases. Appui long : la case est sélectionnée ; en glissant sans relâcher, elle se
 * déplace et s'échange avec la case où on la dépose.
 */
@Composable
private fun BoxGrid(
    columns: Int,
    boxes: List<AdventBox>,
    shares: Map<String, Long>,
    supplyPhotos: Map<String, String?>,
    canSelect: Boolean,
    canDrag: Boolean,
    /** Cases sélectionnées (mode sélection), ou null hors sélection. */
    selected: Set<String>?,
    onOpenBox: (String) -> Unit,
    /** Sélectionne la case ; renvoie vrai si elle vient d'être ajoutée à la sélection. */
    onLongPress: (String) -> Boolean,
    /** L'appui long devient un déplacement : on annule la sélection qu'il avait faite. */
    onLongPressBecameDrag: (String) -> Unit,
    onSwap: (String, String) -> Unit,
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hovered by remember { mutableStateOf<String?>(null) }
    val currentSwap by rememberUpdatedState(onSwap)
    val currentLongPress by rememberUpdatedState(onLongPress)
    val currentBecameDrag by rememberUpdatedState(onLongPressBecameDrag)
    val dragAllowed by rememberUpdatedState(canDrag)
    val moveThreshold = with(LocalDensity.current) { 16.dp.toPx() }
    // Case ajoutée à la sélection par le dernier appui long (annulé si l'appui devient un déplacement).
    var lastLongPressAdded by remember { mutableStateOf<String?>(null) }

    fun targetAt(id: String): String? {
        val start = bounds[id] ?: return null
        val center = start.center + dragOffset
        return bounds.entries.firstOrNull { it.key != id && it.value.contains(center) }?.key
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(boxes, key = { it.id }) { box ->
            val isDragged = dragging == box.id
            BoxCell(
                box = box,
                share = shares[box.id],
                highlighted = hovered == box.id,
                photoPath = box.photoPath ?: box.supplyId?.let { supplyPhotos[it] },
                selected = selected?.contains(box.id),
                modifier = Modifier
                    .onGloballyPositioned { if (dragging != box.id) bounds[box.id] = it.boundsInRoot() }
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        if (isDragged) {
                            translationX = dragOffset.x
                            translationY = dragOffset.y
                            scaleX = 1.08f
                            scaleY = 1.08f
                            shadowElevation = 16f
                        }
                    }
                    .then(
                        if (canSelect) {
                            Modifier.pointerInput(box.id) {
                                var total = Offset.Zero
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        total = Offset.Zero
                                        dragOffset = Offset.Zero
                                    },
                                    onDrag = { change, amount ->
                                        if (!dragAllowed) return@detectDragGesturesAfterLongPress
                                        change.consume()
                                        total += amount
                                        if (dragging == null && total.getDistance() > moveThreshold) {
                                            dragging = box.id
                                            if (lastLongPressAdded == box.id) currentBecameDrag(box.id)
                                        }
                                        if (dragging == box.id) {
                                            dragOffset = total
                                            hovered = targetAt(box.id)
                                        }
                                    },
                                    onDragEnd = {
                                        if (dragging == box.id) targetAt(box.id)?.let { currentSwap(box.id, it) }
                                        dragging = null
                                        hovered = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        dragging = null
                                        hovered = null
                                        dragOffset = Offset.Zero
                                    },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
                onClick = { onOpenBox(box.id) },
                onLongClick = if (canSelect) {
                    { lastLongPressAdded = if (currentLongPress(box.id)) box.id else null }
                } else {
                    null
                },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) { footer() }
    }
}

@Composable
private fun BoxCell(
    box: AdventBox,
    share: Long?,
    highlighted: Boolean,
    photoPath: String?,
    selected: Boolean?,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val gold = LocalMonNoelColors.current.gold
    val (container, border) = when (box.status) {
        BoxStatus.READY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        BoxStatus.TO_PREPARE -> MaterialTheme.colorScheme.tertiaryContainer to gold.copy(alpha = 0.6f)
        BoxStatus.TO_FIND -> MaterialTheme.colorScheme.surfaceContainerLow to MaterialTheme.colorScheme.outlineVariant
    }
    val description = stringResource(R.string.box_cell_description, box.number, box.title.ifBlank { "—" }, stringResource(box.status.label))
    val shape = MaterialTheme.shapes.medium
    Surface(
        color = container,
        shape = shape,
        border = BorderStroke(
            if (highlighted || selected == true) 3.dp else 1.dp,
            when {
                selected == true -> MaterialTheme.colorScheme.primary
                highlighted -> gold
                else -> border
            },
        ),
        modifier = modifier
            .aspectRatio(1.3f)
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.fillMaxSize()) {
            if (photoPath != null) {
                PhotoImage(
                    photoPath,
                    null,
                    size = 400.dp,
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        alpha =
                            0.35f
                    },
                )
            }
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        box.number.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected != null) {
                        Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.size(24.dp))
                    }
                    Text(
                        when (box.status) {
                            BoxStatus.READY -> "✓"
                            BoxStatus.TO_PREPARE -> "●"
                            BoxStatus.TO_FIND -> "○"
                        },
                        color = if (box.status == BoxStatus.TO_FIND) MaterialTheme.colorScheme.outline else gold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    listOfNotNull(box.type?.emoji, box.title.takeIf { it.isNotBlank() }).joinToString("  ").ifBlank { " " },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                val price = if (box.isEmpty) 0 else (box.priceCents ?: 0) + (share ?: 0)
                Text(
                    if (price > 0) formatEuro(price) else " ",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalMonNoelColors.current.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SuppliesSection(
    supplies: List<AdventSupply>,
    boxes: List<AdventBox>,
    readOnly: Boolean,
    onToggle: (AdventSupply) -> Unit,
    onEdit: (AdventSupply) -> Unit,
    onAdd: () -> Unit,
) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider(color = LocalMonNoelColors.current.gold.copy(alpha = 0.35f), modifier = Modifier.padding(bottom = 16.dp))
        Text(
            stringResource(R.string.supplies_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics {
                heading()
            },
        )
        supplies.forEach { supply ->
            val linked = boxes.filter { it.supplyId == supply.id }.map { it.number }.sorted()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = supply.bought, onCheckedChange = if (readOnly) null else { _ -> onToggle(supply) })
                if (supply.photoPath != null) PhotoImage(supply.photoPath, null, size = 40.dp, modifier = Modifier.padding(end = 10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (supply.quantity >
                            1
                        ) {
                            "${supply.name} × ${supply.quantity}"
                        } else {
                            supply.name
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val details = listOfNotNull(
                        supply.priceCents?.let { formatEuro(it) },
                        linked.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.supplies_shared, it.joinToString(", ")) },
                    ).joinToString(" · ")
                    if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, color = secondary)
                }
                if (!readOnly) {
                    IconButton(onClick = { onEdit(supply) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.supplies_edit, supply.name))
                    }
                }
            }
        }
        if (!readOnly) OutlinedButton(onClick = onAdd) { Text(stringResource(R.string.supplies_add)) }
    }
}

/** Barre du haut en mode sélection (comme partout) : fermer, nombre, tout / les vides, « Modifier… ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBar(count: Int, onAll: () -> Unit, onEmpty: () -> Unit, onEdit: () -> Unit, onClose: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.selection_close)) }
        },
        title = { Text(stringResource(R.string.selection_count, count)) },
        actions = {
            TextButton(onClick = onAll) { Text(stringResource(R.string.selection_all)) }
            TextButton(onClick = onEmpty) { Text(stringResource(R.string.selection_empty)) }
            Button(onClick = onEdit, enabled = count > 0, modifier = Modifier.padding(end = 8.dp)) {
                Text(stringResource(R.string.selection_edit))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    )
}

/** Modifier plusieurs cases d'un coup. Chaque champ laissé sur « ne pas changer » est ignoré. */
@Composable
private fun BulkEditDialog(count: Int, supplies: List<AdventSupply>, onApply: (BulkBoxChange) -> Unit, onDismiss: () -> Unit) {
    var type by rememberSaveable { mutableStateOf<SurpriseType?>(null) }
    var status by rememberSaveable { mutableStateOf<BoxStatus?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var supplyId by rememberSaveable { mutableStateOf<String?>(null) }
    val keep = stringResource(R.string.bulk_keep)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bulk_title, count)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (supplies.isNotEmpty()) {
                    DropdownField(
                        label = stringResource(R.string.field_group_purchase),
                        selected = supplies.firstOrNull { it.id == supplyId },
                        options = listOf(null) + supplies,
                        optionLabel = { it?.name ?: keep },
                        onSelect = { supplyId = it?.id },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.bulk_supply_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalMonNoelColors.current.secondaryText,
                    )
                }
                DropdownField(
                    label = stringResource(R.string.field_surprise_type),
                    selected = type,
                    options = listOf<SurpriseType?>(null) + SurpriseType.entries,
                    optionLabel = { t -> t?.let { "${it.emoji}  ${stringResource(it.label)}" } ?: keep },
                    onSelect = { type = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextInput(title, { title = it }, stringResource(R.string.bulk_content), Modifier.fillMaxWidth())
                MoneyInput(price, { price = it }, stringResource(R.string.bulk_price), Modifier.fillMaxWidth())
                DropdownField(
                    label = stringResource(R.string.field_box_status),
                    selected = status,
                    options = listOf<BoxStatus?>(null) + BoxStatus.entries,
                    optionLabel = { st -> st?.let { stringResource(it.label) } ?: keep },
                    onSelect = { status = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            val priceValid = price.isBlank() || parseEuroToCents(price) != null
            TextButton(
                onClick = {
                    onApply(
                        BulkBoxChange(
                            type = type,
                            status = status,
                            title = title.takeIf { it.isNotBlank() },
                            priceCents = parseEuroToCents(price),
                            supplyId = supplyId,
                        ),
                    )
                },
                enabled = priceValid,
            ) { Text(stringResource(R.string.bulk_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupplyDialog(
    supply: AdventSupply?,
    boxes: List<AdventBox>,
    onSave: (SupplyDraft, Set<String>) -> Unit,
    onImportPhoto: (String, (String) -> Unit) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(supply?.name.orEmpty()) }
    var quantity by rememberSaveable { mutableStateOf((supply?.quantity ?: 1).toString()) }
    var price by rememberSaveable { mutableStateOf(centsToInput(supply?.priceCents)) }
    var bought by rememberSaveable { mutableStateOf(supply?.bought ?: false) }
    var photoPath by rememberSaveable { mutableStateOf(supply?.photoPath) }
    var type by rememberSaveable { mutableStateOf(supply?.type) }
    var boxTitle by rememberSaveable { mutableStateOf(supply?.boxTitle.orEmpty()) }
    var supplier by rememberSaveable { mutableStateOf(supply?.supplier.orEmpty()) }
    var shared by rememberSaveable { mutableStateOf(boxes.filter { it.supplyId != null && it.supplyId == supply?.id }.map { it.id }) }
    val qty = quantity.toIntOrNull()
    val valid = name.isNotBlank() && qty != null && qty >= 1 && (price.isBlank() || parseEuroToCents(price) != null)
    val secondary = LocalMonNoelColors.current.secondaryText
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (supply == null) R.string.supplies_new else R.string.supplies_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput(name, { name = it }, stringResource(R.string.supplies_name), Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput(
                        quantity,
                        { quantity = it.filter(Char::isDigit).take(3) },
                        stringResource(R.string.supplies_quantity),
                        Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                    )
                    MoneyInput(price, { price = it }, stringResource(R.string.supplies_price), Modifier.weight(1f))
                }
                TextInput(supplier, { supplier = it }, stringResource(R.string.field_supplier), Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.supplies_bought), modifier = Modifier.weight(1f))
                    Switch(checked = bought, onCheckedChange = { bought = it })
                }
                PhotoField(
                    path = photoPath,
                    onPicked = { uri -> onImportPhoto(uri) { photoPath = it } },
                    onRemove = { photoPath = null },
                    placeholder = "📦",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.supplies_share_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.supplies_share_hint), style = MaterialTheme.typography.bodySmall, color = secondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { shared = boxes.map { it.id } }) { Text(stringResource(R.string.selection_all)) }
                    TextButton(onClick = { shared = boxes.filter { it.isEmpty || it.supplyId == supply?.id }.map { it.id } }) {
                        Text(stringResource(R.string.selection_empty))
                    }
                    TextButton(onClick = { shared = emptyList() }) { Text(stringResource(R.string.selection_none)) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    boxes.forEach { box ->
                        FilterChip(
                            selected = box.id in shared,
                            onClick = { shared = if (box.id in shared) shared - box.id else shared + box.id },
                            label = { Text(box.number.toString()) },
                        )
                    }
                }
                if (shared.isNotEmpty()) {
                    Text(stringResource(R.string.supplies_fill_hint), style = MaterialTheme.typography.bodySmall, color = secondary)
                    DropdownField(
                        label = stringResource(R.string.field_surprise_type),
                        selected = type,
                        options = listOf<SurpriseType?>(null) + SurpriseType.entries,
                        optionLabel = { t ->
                            t?.let { "${it.emoji}  ${stringResource(it.label)}" } ?: stringResource(R.string.surprise_none)
                        },
                        onSelect = { type = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextInput(boxTitle, {
                        boxTitle = it
                    }, stringResource(R.string.supplies_box_title, name.ifBlank { "…" }), Modifier.fillMaxWidth())
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(UiR.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        SupplyDraft(name, qty ?: 1, parseEuroToCents(price), bought, photoPath, type, boxTitle, supplier),
                        shared.toSet(),
                    )
                },
                enabled = valid,
            ) { Text(stringResource(UiR.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.action_cancel)) } },
    )
}
