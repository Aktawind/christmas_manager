package com.aktawind.monnoel.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.settings.CategoriesRoute
import com.aktawind.monnoel.feature.settings.SettingsRoute
import com.aktawind.monnoel.feature.settings.SuggestionsSettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

@Serializable
data object CategoriesDestination

@Serializable
data object SuggestionsSettingsDestination

fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable<SettingsDestination> {
        SettingsRoute(
            onOpenCategories = { navController.navigate(CategoriesDestination) },
            onOpenSuggestions = { navController.navigate(SuggestionsSettingsDestination) },
        )
    }
    composable<SuggestionsSettingsDestination> { SuggestionsSettingsRoute(onBack = { navController.popBackStack() }) }
    composable<CategoriesDestination> { CategoriesRoute(onBack = { navController.popBackStack() }) }
}
