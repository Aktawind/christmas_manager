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
    onOpenTasks: () -> Unit,
    onOpenCooking: () -> Unit,
    onOpenBudget: () -> Unit,
) {
    composable<HomeDestination> {
        HomeRoute(
            onOpenGifts = onOpenGifts,
            onOpenAdvent = onOpenAdvent,
            onOpenTasks = onOpenTasks,
            onOpenCooking = onOpenCooking,
            onOpenBudget = onOpenBudget,
        )
    }
}
