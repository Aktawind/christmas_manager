package com.aktawind.monnoel.feature.years.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.years.YearsRoute
import kotlinx.serialization.Serializable

@Serializable
data object YearsDestination

fun NavGraphBuilder.yearsScreen(onYearSelected: () -> Unit) {
    composable<YearsDestination> { YearsRoute(onYearSelected = onYearSelected) }
}
