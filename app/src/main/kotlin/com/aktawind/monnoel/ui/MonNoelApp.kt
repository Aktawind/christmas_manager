package com.aktawind.monnoel.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
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
import com.aktawind.monnoel.feature.home.navigation.HomeDestination
import com.aktawind.monnoel.feature.home.navigation.homeScreen
import com.aktawind.monnoel.feature.settings.navigation.SettingsDestination
import com.aktawind.monnoel.feature.settings.navigation.settingsScreen
import com.aktawind.monnoel.feature.years.WelcomeRoute
import com.aktawind.monnoel.feature.years.navigation.YearsDestination
import com.aktawind.monnoel.feature.years.navigation.yearsScreen
import kotlin.reflect.KClass

private enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    HOME(HomeDestination, HomeDestination::class, Icons.Filled.Home, R.string.nav_home),
    YEARS(YearsDestination, YearsDestination::class, Icons.Filled.Star, R.string.nav_years),
    SETTINGS(SettingsDestination, SettingsDestination::class, Icons.Filled.Settings, R.string.nav_settings),
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

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = current?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true
                item(
                    selected = selected,
                    onClick = { navigate(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
    ) {
        Surface(Modifier, color = MaterialTheme.colorScheme.background) {
            NavHost(navController = navController, startDestination = HomeDestination) {
                homeScreen()
                yearsScreen(onYearSelected = { navigate(TopLevelDestination.HOME) })
                settingsScreen()
            }
        }
    }
}
