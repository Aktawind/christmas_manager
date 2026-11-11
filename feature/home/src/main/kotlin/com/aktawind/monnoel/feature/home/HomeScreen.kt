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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state)
}

@Composable
internal fun HomeScreen(state: HomeUiState) {
    when (state) {
        HomeUiState.Loading, HomeUiState.NoEdition -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is HomeUiState.Ready -> HomeContent(state)
    }
}

@Composable
private fun HomeContent(state: HomeUiState.Ready) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { CountdownCard(state) }
        item(span = { GridItemSpan(maxLineSpan) }) { DailyMessage(state.dayOfYear) }
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
