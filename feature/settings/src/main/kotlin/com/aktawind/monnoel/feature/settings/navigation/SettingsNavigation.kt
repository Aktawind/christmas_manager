package com.aktawind.monnoel.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.aktawind.monnoel.feature.settings.CategoriesRoute
import com.aktawind.monnoel.feature.settings.PathDatesRoute
import com.aktawind.monnoel.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

@Serializable
data class CategoriesDestination(val recipes: Boolean = false)

@Serializable
data object PathDatesDestination

fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable<SettingsDestination> {
        SettingsRoute(
            onOpenCategories = { navController.navigate(CategoriesDestination()) },
            onOpenRecipeCategories = { navController.navigate(CategoriesDestination(recipes = true)) },
            onOpenPath = { navController.navigate(PathDatesDestination) },
        )
    }
    composable<PathDatesDestination> { PathDatesRoute(onBack = { navController.popBackStack() }) }
    composable<CategoriesDestination> { entry ->
        CategoriesRoute(recipes = entry.toRoute<CategoriesDestination>().recipes, onBack = { navController.popBackStack() })
    }
}
