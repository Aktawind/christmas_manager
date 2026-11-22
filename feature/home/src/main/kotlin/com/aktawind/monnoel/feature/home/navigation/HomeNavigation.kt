package com.aktawind.monnoel.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

fun NavGraphBuilder.homeScreen(
    onOpenGifts: () -> Unit,
    onOpenAdvent: () -> Unit,
    onOpenCooking: () -> Unit,
    onOpenPreparations: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenTraditions: () -> Unit,
    onOpenCrafts: () -> Unit,
    onEditPath: () -> Unit,
) {
    composable<HomeDestination> {
        HomeRoute(
            onOpenGifts = onOpenGifts,
            onOpenAdvent = onOpenAdvent,
            onOpenCooking = onOpenCooking,
            onOpenPreparations = onOpenPreparations,
            onOpenBudget = onOpenBudget,
            onOpenTraditions = onOpenTraditions,
            onOpenCrafts = onOpenCrafts,
            onEditPath = onEditPath,
        )
    }
}
