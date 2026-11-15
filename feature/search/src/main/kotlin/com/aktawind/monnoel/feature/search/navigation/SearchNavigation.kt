package com.aktawind.monnoel.feature.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.feature.search.SearchRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchDestination

fun NavGraphBuilder.searchScreen(onOpen: (LinkType, String) -> Unit) {
    composable<SearchDestination> { SearchRoute(onOpen = onOpen) }
}
