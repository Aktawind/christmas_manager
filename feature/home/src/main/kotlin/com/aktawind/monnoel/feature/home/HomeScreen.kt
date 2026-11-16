package com.aktawind.monnoel.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.component.GoldStar
import com.aktawind.monnoel.core.designsystem.component.StarDivider
import com.aktawind.monnoel.core.designsystem.theme.FirGreen
import com.aktawind.monnoel.core.designsystem.theme.FirGreenDark
import com.aktawind.monnoel.core.designsystem.theme.Ivory
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.designsystem.theme.MonNoelTheme
import com.aktawind.monnoel.core.domain.budget.YearBudget
import com.aktawind.monnoel.core.domain.countdown.CountdownState
import com.aktawind.monnoel.core.domain.countdown.PathStep
import com.aktawind.monnoel.core.domain.countdown.PathStepKind
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.ui.QuickIdeaDialog
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.resolve
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeRoute(
    onOpenGifts: () -> Unit,
    onOpenAdvent: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenCooking: () -> Unit,
    onOpenBudget: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var quickIdea by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val message = (state as? HomeUiState.Ready)?.message
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it.resolve(context))
            viewModel.messageShown()
        }
    }
    Box(Modifier.fillMaxSize()) {
        HomeScreen(
            state,
            onOpenGifts = onOpenGifts,
            onOpenAdvent = onOpenAdvent,
            onOpenTasks = onOpenTasks,
            onOpenCooking = onOpenCooking,
            onOpenBudget = onOpenBudget,
            onQuickIdea = { quickIdea = true },
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    if (quickIdea) {
        val persons by viewModel.persons.collectAsStateWithLifecycle()
        QuickIdeaDialog(
            persons = persons,
            onSave = { input ->
                viewModel.quickIdea(input)
                quickIdea = false
            },
            onDismiss = { quickIdea = false },
        )
    }
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onOpenGifts: () -> Unit = {},
    onOpenAdvent: () -> Unit = {},
    onOpenTasks: () -> Unit = {},
    onOpenCooking: () -> Unit = {},
    onOpenBudget: () -> Unit = {},
    onQuickIdea: () -> Unit = {},
) {
    when (state) {
        HomeUiState.Loading, HomeUiState.NoEdition -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is HomeUiState.Ready -> HomeContent(state, onOpenGifts, onOpenAdvent, onOpenTasks, onOpenCooking, onOpenBudget, onQuickIdea)
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Ready,
    onOpenGifts: () -> Unit,
    onOpenAdvent: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenCooking: () -> Unit,
    onOpenBudget: () -> Unit,
    onQuickIdea: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { CountdownCard(state) }
        item(span = { GridItemSpan(maxLineSpan) }) { DailyMessage(state.dayOfYear) }
        item(span = { GridItemSpan(maxLineSpan) }) { ChristmasPathCard(state.path) }
        item(span = { GridItemSpan(maxLineSpan) }) { TasksSummaryCard(state, onOpenTasks) }
        item(span = { GridItemSpan(maxLineSpan) }) { GiftsSummaryCard(state.gifts, onOpenGifts, onQuickIdea) }
        item(span = { GridItemSpan(maxLineSpan) }) { AdventSummaryCard(state.calendars, onOpenAdvent) }
        item(span = { GridItemSpan(maxLineSpan) }) { TableSummaryCard(state.table, onOpenCooking) }
        item(span = { GridItemSpan(maxLineSpan) }) { CookingSummaryCard(state.cooking, onOpenCooking) }
        state.budget?.let { b -> item(span = { GridItemSpan(maxLineSpan) }) { BudgetSummaryCard(b, onOpenBudget) } }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.home_upcoming_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.home_upcoming_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalMonNoelColors.current.secondaryText,
                )
            }
        }
        items(upcomingModules) { module -> UpcomingModuleCard(module) }
    }
}

@Composable
private fun CountdownCard(state: HomeUiState.Ready) {
    val gold = LocalMonNoelColors.current.gold
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
        color = FirGreen,
    ) {
        Box(
            Modifier
                .background(Brush.verticalGradient(listOf(FirGreen, FirGreenDark)))
                .padding(horizontal = 28.dp, vertical = 32.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GoldStar(size = 14.dp, color = gold)
                    Text(
                        stringResource(R.string.home_edition_title, state.year),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Ivory,
                        modifier = Modifier.semantics { heading() },
                    )
                    GoldStar(size = 14.dp, color = gold)
                }
                if (state.isArchived) {
                    Text(
                        stringResource(R.string.home_archived),
                        style = MaterialTheme.typography.labelLarge,
                        color = gold,
                    )
                }
                CountdownText(state.countdown)
            }
        }
    }
}

@Composable
private fun CountdownText(countdown: CountdownState) {
    val gold = LocalMonNoelColors.current.gold
    when (countdown) {
        is CountdownState.DaysLeft -> {
            Text(
                countdown.days.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = gold,
                modifier = Modifier.clearAndSetSemantics { },
            )
            Text(
                stringResource(R.string.home_days_left),
                style = MaterialTheme.typography.titleMedium,
                color = Ivory,
                textAlign = TextAlign.Center,
            )
            countdown.adventDay?.let {
                Text(
                    stringResource(R.string.home_advent_day, it),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ivory.copy(alpha = 0.85f),
                )
            }
        }
        CountdownState.ChristmasEve -> BigMessage(stringResource(R.string.home_christmas_eve))
        CountdownState.ChristmasDay -> BigMessage(stringResource(R.string.home_christmas_day))
        is CountdownState.Past -> BigMessage(stringResource(R.string.home_past, countdown.year))
    }
}

@Composable
private fun BigMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineLarge,
        color = LocalMonNoelColors.current.gold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun DailyMessage(dayOfYear: Int) {
    val messages = stringArrayResource(R.array.home_daily_messages)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
        StarDivider()
        Text(
            messages[dayOfYear % messages.size],
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        StarDivider()
    }
}

/** « Le chemin vers Noël » : les étapes datées par nature, présentées comme un chemin, jamais comme des retards. */
@Composable
private fun ChristmasPathCard(path: List<PathStep>) {
    if (path.isEmpty()) return
    val gold = LocalMonNoelColors.current.gold
    val secondary = LocalMonNoelColors.current.secondaryText
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.home_path_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            path.forEachIndexed { index, step ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Hauteur fixe : la ligne reste droite quelle que soit la taille du point.
                    Row(Modifier.height(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(2.dp).background(if (index == 0) Color.Transparent else gold.copy(alpha = 0.4f)))
                        Surface(
                            shape = CircleShape,
                            color = when {
                                step.passed -> gold
                                step.isNext -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(if (step.isNext) 18.dp else 14.dp),
                        ) {}
                        Box(
                            Modifier.weight(1f).height(2.dp).background(
                                if (index ==
                                    path.lastIndex
                                ) {
                                    Color.Transparent
                                } else {
                                    gold.copy(alpha = 0.4f)
                                },
                            ),
                        )
                    }
                    Text(
                        stepEmoji(step.kind) + "  " + dateFormat.format(step.date),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (step.isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(stepLabel(step.kind)),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun stepEmoji(kind: PathStepKind) = when (kind) {
    PathStepKind.ADVENT_START -> "🗓️"
    PathStepKind.CHRISTMAS_EVE -> "🌟"
    PathStepKind.CHRISTMAS -> "🎄"
}

private fun stepLabel(kind: PathStepKind) = when (kind) {
    PathStepKind.ADVENT_START -> R.string.home_path_advent
    PathStepKind.CHRISTMAS_EVE -> R.string.home_path_eve
    PathStepKind.CHRISTMAS -> R.string.home_path_christmas
}

@Composable
private fun TasksSummaryCard(state: HomeUiState.Ready, onOpen: () -> Unit) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "📝  " + stringResource(R.string.home_tasks_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics {
                    heading()
                },
            )
            val gifts = state.gifts
            val readyBoxes = state.calendars.sumOf { it.ready }
            val totalBoxes = state.calendars.sumOf { it.boxCount }
            val progress = listOfNotNull(
                gifts.total.takeIf { it > 0 }?.let { stringResource(R.string.home_progress_gifts, gifts.given + gifts.wrapped, it) },
                totalBoxes.takeIf { it > 0 }?.let { stringResource(R.string.home_progress_boxes, readyBoxes, it) },
                state.doneTasks.takeIf { it > 0 }?.let { stringResource(R.string.home_progress_tasks, it) },
            ).joinToString(" · ")
            if (progress.isNotBlank()) Text(progress, style = MaterialTheme.typography.bodyMedium)
            if (state.openTasks.isEmpty()) {
                Text(stringResource(R.string.home_tasks_none), color = secondary)
            } else {
                state.openTasks.take(4).forEach { Text("○  $it", style = MaterialTheme.typography.bodyLarge, maxLines = 1) }
                if (state.openTasks.size > 4) Text(stringResource(R.string.home_tasks_more, state.openTasks.size - 4), color = secondary)
            }
            if (state.suggestionCount > 0) {
                Text(stringResource(R.string.home_tasks_suggestions, state.suggestionCount), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BudgetSummaryCard(budget: YearBudget, onOpen: () -> Unit) {
    val colors = LocalMonNoelColors.current
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "💰  " + stringResource(R.string.home_budget_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.home_budget_line, formatEuro(budget.plannedCents), formatEuro(budget.spentCents)),
                style = MaterialTheme.typography.bodyLarge,
            )
            val global = budget.globalBudgetCents
            val margin = budget.marginCents
            if (global != null && margin != null) {
                LinearProgressIndicator(
                    progress = { if (global > 0) (budget.plannedCents.toFloat() / global).coerceIn(0f, 1f) else 1f },
                    color = colors.gold,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (margin >= 0) {
                        stringResource(R.string.home_budget_margin, formatEuro(global), formatEuro(margin))
                    } else {
                        stringResource(R.string.home_budget_over, formatEuro(global), formatEuro(-margin))
                    },
                    color = if (margin >= 0) colors.secondaryText else colors.burgundy,
                )
            }
        }
    }
}

@Composable
private fun TableSummaryCard(summary: TableSummary, onOpen: () -> Unit) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "🍽️  " + stringResource(R.string.home_table_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            val next = summary.next
            if (next == null) {
                Text(stringResource(R.string.home_table_empty), color = secondary)
            } else {
                val date = next.date?.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                Text(
                    listOfNotNull("${next.type.emoji} ${next.name}", date).joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    listOf(
                        pluralStringResource(R.plurals.home_table_guests, next.guestCount, next.guestCount),
                        pluralStringResource(R.plurals.home_table_dishes, summary.nextDishes, summary.nextDishes),
                    ).joinToString(" · "),
                    color = secondary,
                )
                if (summary.toOrder > 0) {
                    Text(
                        pluralStringResource(R.plurals.home_table_to_order, summary.toOrder, summary.toOrder),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CookingSummaryCard(summary: CookingSummary, onOpen: () -> Unit) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "🧁  " + stringResource(R.string.home_cooking_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            when {
                summary.total == 0 -> Text(stringResource(R.string.home_cooking_empty), color = secondary)
                summary.done == summary.total -> Text(
                    stringResource(R.string.home_cooking_all_done, summary.total),
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> {
                    Text(
                        stringResource(R.string.home_cooking_progress, summary.done, summary.total),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        summary.next.take(3).joinToString(" · "),
                        color = secondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdventSummaryCard(calendars: List<CalendarProgress>, onOpen: () -> Unit) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "🗓️  " + stringResource(R.string.home_advent_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (calendars.isEmpty()) {
                Text(stringResource(R.string.home_advent_empty), color = secondary)
            }
            calendars.forEach { c ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.home_advent_line, c.name, c.ready, c.boxCount), style = MaterialTheme.typography.bodyLarge)
                    if (c.todayNumber != null) {
                        val content = c.todayBox?.let { box ->
                            listOfNotNull(box.type?.emoji, box.title.takeIf { it.isNotBlank() }).joinToString(" ")
                        }
                            .orEmpty()
                            .ifBlank { stringResource(R.string.home_advent_today_empty) }
                        Text(stringResource(R.string.home_advent_today, c.todayNumber, content), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GiftsSummaryCard(summary: GiftsSummary, onOpen: () -> Unit, onQuickIdea: () -> Unit) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🎁  " + stringResource(R.string.home_gifts_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                FilledTonalButton(onClick = onQuickIdea) { Text(stringResource(R.string.home_quick_idea)) }
            }
            if (summary.total == 0) {
                Text(
                    stringResource(R.string.home_gifts_empty, summary.ideas),
                    color = LocalMonNoelColors.current.secondaryText,
                )
            } else {
                Text(
                    stringResource(R.string.home_gifts_line, summary.total, formatEuro(summary.plannedCents)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                val details = listOfNotNull(
                    summary.toBuy.takeIf { it > 0 }?.let { stringResource(R.string.home_gifts_to_buy, it) },
                    summary.awaiting.takeIf { it > 0 }?.let { stringResource(R.string.home_gifts_awaiting, it) },
                    summary.toWrap.takeIf { it > 0 }?.let { stringResource(R.string.home_gifts_to_wrap, it) },
                    summary.given.takeIf { it > 0 }?.let { stringResource(R.string.home_gifts_given, it) },
                ).joinToString(" · ")
                if (details.isNotBlank()) Text(details, color = LocalMonNoelColors.current.secondaryText)
            }
        }
    }
}

@Composable
private fun UpcomingModuleCard(module: UpcomingModule) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                module.emoji,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.clearAndSetSemantics {
                },
            )
            Text(
                stringResource(module.title),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.home_coming_in, module.version),
                style = MaterialTheme.typography.labelMedium,
                color = LocalMonNoelColors.current.secondaryText,
            )
        }
    }
}

@Preview(widthDp = 900, heightDp = 700)
@Composable
private fun HomePreview() {
    MonNoelTheme {
        Surface {
            HomeScreen(HomeUiState.Ready(2026, false, CountdownState.DaysLeft(44, null), 315))
        }
    }
}
