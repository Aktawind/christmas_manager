package com.aktawind.monnoel.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.settings.CategoriesRoute
import com.aktawind.monnoel.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

@Serializable
data object CategoriesDestination

fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable<SettingsDestination> {
        SettingsRoute(onOpenCategories = { navController.navigate(CategoriesDestination) })
    }
    composable<CategoriesDestination> { CategoriesRoute(onBack = { navController.popBackStack() }) }
}
