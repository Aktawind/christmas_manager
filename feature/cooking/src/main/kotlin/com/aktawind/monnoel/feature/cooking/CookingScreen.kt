package com.aktawind.monnoel.feature.cooking

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.gifts.formatEuro
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.MealDish
import com.aktawind.monnoel.core.model.Production
import com.aktawind.monnoel.core.model.ProductionStatus
import com.aktawind.monnoel.core.model.Recipe
import com.aktawind.monnoel.core.ui.EmptyState
import com.aktawind.monnoel.core.ui.PhotoImage
import com.aktawind.monnoel.core.ui.emoji
import com.aktawind.monnoel.core.ui.label
import com.aktawind.monnoel.core.ui.resolve
import com.aktawind.monnoel.feature.cooking.meal.mealDateFormat

private enum class CookingTab(@StringRes val label: Int, @StringRes val fab: Int) {
    MEALS(R.string.tab_meals, R.string.fab_new_meal),
    PRODUCTIONS(R.string.tab_productions, R.string.fab_new_production),
    RECIPES(R.string.tab_recipes, R.string.fab_new_recipe),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingRoute(
    onOpenRecipe: (String) -> Unit,
    onNewRecipe: () -> Unit,
    onOpenProduction: (String) -> Unit,
    onNewProduction: () -> Unit,
    onOpenMeal: (String) -> Unit,
    onNewMeal: () -> Unit,
    viewModel: CookingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(CookingTab.MEALS) }
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
                            state.edition?.let { stringResource(R.string.cooking_title_year, it.year) }
                                ?: stringResource(R.string.cooking_title),
                        )
                    },
                )
                PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                    CookingTab.entries.forEach { t ->
                        Tab(selected = tab == t, onClick = { tab = t }, text = { Text(stringResource(t.label)) })
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            val showFab = tab == CookingTab.RECIPES || (!state.readOnly && state.edition != null)
            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = when (tab) {
                        CookingTab.MEALS -> onNewMeal
                        CookingTab.PRODUCTIONS -> onNewProduction
                        CookingTab.RECIPES -> onNewRecipe
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(tab.fab)) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                CookingTab.MEALS -> MealsTab(state, onOpenMeal)
                CookingTab.PRODUCTIONS -> ProductionsTab(state, onOpenProduction, viewModel::setProductionStatus)
                CookingTab.RECIPES -> RecipesTab(state, onOpenRecipe)
            }
        }
    }
}

@Composable
private fun ProductionsTab(state: CookingUiState, onOpen: (String) -> Unit, onStatus: (String, ProductionStatus) -> Unit) {
    val done = state.productions.count { it.status == ProductionStatus.DONE }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.productions.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    emoji = "🧁",
                    title = stringResource(R.string.productions_empty_title),
                    message = stringResource(R.string.productions_empty_message),
                )
            }
        } else if (state.productions.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.productions_progress, done, state.productions.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LocalMonNoelColors.current.secondaryText,
                    modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(bottom = 8.dp),
                )
            }
        }
        ProductionStatus.entries.forEachIndexed { index, status ->
            val list = state.productions.filter { it.status == status }
            if (list.isEmpty()) return@forEachIndexed
            item(key = "h-$status") {
                Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(top = 16.dp)) {
                    if (index > 0 && state.productions.any { it.status.ordinal < status.ordinal }) {
                        HorizontalDivider(Modifier.padding(bottom = 16.dp), color = LocalMonNoelColors.current.gold.copy(alpha = 0.35f))
                    }
                    Text(
                        "${status.emoji}  ${stringResource(status.label)} · ${list.size}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
            items(list, key = { it.id }) { production ->
                ProductionRow(production, state.readOnly, onClick = { onOpen(production.id) }, onStatus = { onStatus(production.id, it) })
            }
        }
    }
}

@Composable
private fun ProductionRow(production: Production, readOnly: Boolean, onClick: () -> Unit, onStatus: (ProductionStatus) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoImage(
                production.photoPath ?: production.recipePhotoPath,
                null,
                size = 52.dp,
                shape = MaterialTheme.shapes.small,
                placeholder = "🧁",
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (production.quantity > 1) "${production.recipeName} × ${production.quantity}" else production.recipeName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (production.destination.isNotBlank()) {
                    Text(
                        production.destination,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalMonNoelColors.current.secondaryText,
                    )
                }
            }
            Box {
                val color = if (production.status ==
                    ProductionStatus.DONE
                ) {
                    LocalMonNoelColors.current.gold
                } else {
                    MaterialTheme.colorScheme.primary
                }
                TextButton(
                    onClick = { menu = true },
                    enabled = !readOnly,
                    colors = ButtonDefaults.textButtonColors(contentColor = color, disabledContentColor = color),
                ) {
                    Text(
                        "${production.status.emoji} ${stringResource(production.status.label)}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (!readOnly) Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.production_change_status))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    ProductionStatus.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text("${s.emoji}  ${stringResource(s.label)}") },
                            onClick = {
                                menu = false
                                onStatus(s)
                            },
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun RecipesTab(state: CookingUiState, onOpen: (String) -> Unit) {
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = state.recipes.filter { categoryFilter == null || it.categoryId == categoryFilter }
    val categoryNames = state.categories.associate { it.id to it.name }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = categoryFilter == null, onClick = {
                    categoryFilter = null
                }, label = { Text(stringResource(R.string.recipes_filter_all)) })
                state.categories.filter { c -> state.recipes.any { it.categoryId == c.id } }.forEach { c ->
                    FilterChip(selected = categoryFilter == c.id, onClick = { categoryFilter = c.id }, label = { Text(c.name) })
                }
            }
        }
        if (state.recipes.isEmpty() && !state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    emoji = "📖",
                    title = stringResource(R.string.recipes_empty_title),
                    message = stringResource(R.string.recipes_empty_message),
                )
            }
        }
        items(visible, key = {
            it.id
        }) { recipe -> RecipeCard(recipe, recipe.categoryId?.let(categoryNames::get), onClick = { onOpen(recipe.id) }) }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, category: String?, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(recipe.photoPath, null, size = 84.dp, shape = RectangleShape, placeholder = "🧁")
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(recipe.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (category != null) {
                    Text(category, style = MaterialTheme.typography.bodySmall, color = LocalMonNoelColors.current.secondaryText)
                }
            }
        }
    }
}

@Composable
private fun MealsTab(state: CookingUiState, onOpen: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(340.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.meals.isEmpty() && !state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    emoji = "🍽️",
                    title = stringResource(R.string.meals_empty_title),
                    message = stringResource(R.string.meals_empty_message),
                )
            }
        }
        items(state.meals, key = { it.id }) { meal ->
            MealCard(meal, state.dishes.filter { it.mealId == meal.id }, onClick = { onOpen(meal.id) })
        }
    }
}

@Composable
private fun MealCard(meal: Meal, dishes: List<MealDish>, onClick: () -> Unit) {
    val secondary = LocalMonNoelColors.current.secondaryText
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${meal.type.emoji}  ${meal.name}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val whenWhere = listOfNotNull(meal.date?.format(mealDateFormat), meal.place.ifBlank { null }).joinToString(" · ")
            if (whenWhere.isNotBlank()) Text(whenWhere, style = MaterialTheme.typography.bodyMedium)
            val facts = listOfNotNull(
                pluralStringResource(R.plurals.meal_guests, meal.guestCount, meal.guestCount),
                pluralStringResource(R.plurals.meal_dishes, dishes.size, dishes.size),
                meal.estimatedCostCents?.let { formatEuro(it) },
            ).joinToString(" · ")
            Text(facts, style = MaterialTheme.typography.bodySmall, color = secondary)
            val toOrder = dishes.count { it.toOrder && !it.ordered }
            if (toOrder > 0) {
                Text(
                    pluralStringResource(R.plurals.meal_to_order, toOrder, toOrder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
