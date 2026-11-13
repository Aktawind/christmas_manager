package com.aktawind.monnoel.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalContext
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
import com.aktawind.monnoel.core.domain.countdown.CountdownState
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.ui.QuickIdeaDialog
import com.aktawind.monnoel.core.ui.resolve

@Composable
fun HomeRoute(onOpenGifts: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
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
        HomeScreen(state, onOpenGifts = onOpenGifts, onQuickIdea = { quickIdea = true })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    if (quickIdea) {
        val persons by viewModel.persons.collectAsStateWithLifecycle()
        QuickIdeaDialog(
            persons = persons,
            onSave = { title, url, personIds ->
                viewModel.quickIdea(title, url, personIds)
                quickIdea = false
            },
            onDismiss = { quickIdea = false },
        )
    }
}

@Composable
internal fun HomeScreen(state: HomeUiState, onOpenGifts: () -> Unit = {}, onQuickIdea: () -> Unit = {}) {
    when (state) {
        HomeUiState.Loading, HomeUiState.NoEdition -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is HomeUiState.Ready -> HomeContent(state, onOpenGifts, onQuickIdea)
    }
}

@Composable
private fun HomeContent(state: HomeUiState.Ready, onOpenGifts: () -> Unit, onQuickIdea: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { CountdownCard(state) }
        item(span = { GridItemSpan(maxLineSpan) }) { DailyMessage(state.dayOfYear) }
        item(span = { GridItemSpan(maxLineSpan) }) { GiftsSummaryCard(state.gifts, onOpenGifts, onQuickIdea) }
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
