package com.aktawind.monnoel.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aktawind.monnoel.R
import com.aktawind.monnoel.core.designsystem.icon.Cupcake
import com.aktawind.monnoel.core.designsystem.icon.MonNoelIcons
import com.aktawind.monnoel.core.designsystem.icon.Wallet
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.feature.advent.navigation.AdventDestination
import com.aktawind.monnoel.feature.advent.navigation.BoxEditDestination
import com.aktawind.monnoel.feature.advent.navigation.CalendarDestination
import com.aktawind.monnoel.feature.advent.navigation.CalendarEditDestination
import com.aktawind.monnoel.feature.advent.navigation.adventGraph
import com.aktawind.monnoel.feature.budget.navigation.BudgetDestination
import com.aktawind.monnoel.feature.budget.navigation.budgetScreen
import com.aktawind.monnoel.feature.cooking.navigation.CookingDestination
import com.aktawind.monnoel.feature.cooking.navigation.DishEditDestination
import com.aktawind.monnoel.feature.cooking.navigation.MealDestination
import com.aktawind.monnoel.feature.cooking.navigation.MealEditDestination
import com.aktawind.monnoel.feature.cooking.navigation.ProductionEditDestination
import com.aktawind.monnoel.feature.cooking.navigation.RecipeEditDestination
import com.aktawind.monnoel.feature.cooking.navigation.cookingGraph
import com.aktawind.monnoel.feature.gifts.navigation.GiftEditDestination
import com.aktawind.monnoel.feature.gifts.navigation.GiftsDestination
import com.aktawind.monnoel.feature.gifts.navigation.IdeaEditDestination
import com.aktawind.monnoel.feature.gifts.navigation.PersonDestination
import com.aktawind.monnoel.feature.gifts.navigation.PersonEditDestination
import com.aktawind.monnoel.feature.gifts.navigation.giftsGraph
import com.aktawind.monnoel.feature.home.navigation.HomeDestination
import com.aktawind.monnoel.feature.home.navigation.homeScreen
import com.aktawind.monnoel.feature.search.navigation.SearchDestination
import com.aktawind.monnoel.feature.search.navigation.searchScreen
import com.aktawind.monnoel.feature.settings.navigation.CategoriesDestination
import com.aktawind.monnoel.feature.settings.navigation.SettingsDestination
import com.aktawind.monnoel.feature.settings.navigation.SuggestionsSettingsDestination
import com.aktawind.monnoel.feature.settings.navigation.settingsScreen
import com.aktawind.monnoel.feature.tasks.navigation.TaskEditDestination
import com.aktawind.monnoel.feature.tasks.navigation.TasksDestination
import com.aktawind.monnoel.feature.tasks.navigation.tasksGraph
import com.aktawind.monnoel.feature.years.WelcomeRoute
import com.aktawind.monnoel.feature.years.navigation.YearsDestination
import com.aktawind.monnoel.feature.years.navigation.yearsScreen
import kotlin.reflect.KClass

private enum class TopLevelDestination(
    val route: Any,
    val routeClasses: List<KClass<*>>,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    HOME(HomeDestination, listOf(HomeDestination::class), Icons.Filled.Home, R.string.nav_home),
    SEARCH(SearchDestination, listOf(SearchDestination::class), Icons.Filled.Search, R.string.nav_search),
    GIFTS(
        GiftsDestination,
        listOf(
            GiftsDestination::class,
            GiftEditDestination::class,
            IdeaEditDestination::class,
            PersonDestination::class,
            PersonEditDestination::class,
        ),
        MonNoelIcons.Gift,
        R.string.nav_gifts,
    ),
    ADVENT(
        AdventDestination,
        listOf(AdventDestination::class, CalendarDestination::class, CalendarEditDestination::class, BoxEditDestination::class),
        Icons.Filled.DateRange,
        R.string.nav_advent,
    ),
    COOKING(
        CookingDestination,
        listOf(
            CookingDestination::class,
            RecipeEditDestination::class,
            ProductionEditDestination::class,
            MealDestination::class,
            MealEditDestination::class,
            DishEditDestination::class,
        ),
        MonNoelIcons.Cupcake,
        R.string.nav_cooking,
    ),
    TASKS(
        TasksDestination,
        listOf(TasksDestination::class, TaskEditDestination::class),
        Icons.AutoMirrored.Filled.List,
        R.string.nav_tasks,
    ),
    BUDGET(BudgetDestination(), listOf(BudgetDestination::class), MonNoelIcons.Wallet, R.string.nav_budget),
    YEARS(YearsDestination, listOf(YearsDestination::class), Icons.Filled.Star, R.string.nav_years),
    SETTINGS(
        SettingsDestination,
        listOf(SettingsDestination::class, CategoriesDestination::class, SuggestionsSettingsDestination::class),
        Icons.Filled.Settings,
        R.string.nav_settings,
    ),
}

/**
 * Racine de l'interface. La barre de navigation s'adapte : rail latéral sur tablette,
 * barre du bas sur téléphone.
 */
@Composable
fun MonNoelApp(hasEditions: Boolean) {
    if (!hasEditions) {
        WelcomeRoute()
        return
    }
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination

    fun navigate(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    // Marges système (barre d'état en haut, barre de navigation Android en bas) : le contenu
    // ne doit jamais passer dessous. Avec la barre du bas (téléphone), c'est elle qui gère le bas.
    val contentInsets = if (layoutType == NavigationSuiteType.NavigationBar) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.End)
    }

    // Ouvre l'élément lié à une tâche ou à un résultat de recherche.
    val openLink: (LinkType, String) -> Unit = { type, id ->
        val destination: Any = when (type) {
            LinkType.PERSON -> PersonDestination(id)
            LinkType.IDEA -> IdeaEditDestination(ideaId = id)
            LinkType.GIFT -> GiftEditDestination(giftId = id)
            LinkType.CALENDAR -> CalendarDestination(id)
            LinkType.BOX -> BoxEditDestination(id)
            LinkType.TASK -> TaskEditDestination(id)
            LinkType.RECIPE -> RecipeEditDestination(recipeId = id)
            LinkType.PRODUCTION -> ProductionEditDestination(productionId = id)
            LinkType.MEAL -> MealDestination(id)
            LinkType.EXPENSE -> BudgetDestination(expenseId = id)
            LinkType.WHISPER -> PersonDestination(id)
        }
        navController.navigate(destination)
    }

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = current?.hierarchy?.any { d -> destination.routeClasses.any { d.hasRoute(it) } } == true
                item(
                    selected = selected,
                    onClick = { navigate(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
    ) {
        Surface(Modifier.windowInsetsPadding(contentInsets), color = MaterialTheme.colorScheme.background) {
            NavHost(navController = navController, startDestination = HomeDestination) {
                homeScreen(
                    onOpenGifts = { navigate(TopLevelDestination.GIFTS) },
                    onOpenAdvent = { navigate(TopLevelDestination.ADVENT) },
                    onOpenTasks = { navigate(TopLevelDestination.TASKS) },
                    onOpenCooking = { navigate(TopLevelDestination.COOKING) },
                    onOpenBudget = { navigate(TopLevelDestination.BUDGET) },
                )
                searchScreen(onOpen = openLink)
                tasksGraph(navController, onOpenLink = openLink)
                giftsGraph(navController)
                adventGraph(navController)
                cookingGraph(navController)
                budgetScreen()
                yearsScreen(onYearSelected = { navigate(TopLevelDestination.HOME) })
                settingsScreen(navController)
            }
        }
    }
}
