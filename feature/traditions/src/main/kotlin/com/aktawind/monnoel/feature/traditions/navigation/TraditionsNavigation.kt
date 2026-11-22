package com.aktawind.monnoel.feature.traditions.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.traditions.TraditionsRoute
import com.aktawind.monnoel.feature.traditions.edit.CraftEditRoute
import com.aktawind.monnoel.feature.traditions.edit.TraditionEditRoute
import kotlinx.serialization.Serializable

/**
 * [inspirationId] / [decorId] : élément à ouvrir directement (depuis la recherche) ;
 * [tab] : onglet à afficher (0 programme, 1 traditions, 2 inspirations, 3 bricolages, 4 décoration).
 */
@Serializable
data class TraditionsDestination(val inspirationId: String? = null, val tab: Int? = null, val decorId: String? = null)

@Serializable
data class CraftEditDestination(val craftId: String? = null)

@Serializable
data class TraditionEditDestination(val traditionId: String? = null)

fun NavGraphBuilder.traditionsGraph(navController: NavController) {
    composable<TraditionsDestination> {
        TraditionsRoute(
            onOpenTradition = { navController.navigate(TraditionEditDestination(it)) },
            onNewTradition = { navController.navigate(TraditionEditDestination()) },
            onOpenCraft = { navController.navigate(CraftEditDestination(it)) },
            onNewCraft = { navController.navigate(CraftEditDestination()) },
        )
    }
    composable<CraftEditDestination> { CraftEditRoute(onDone = { navController.popBackStack() }) }
    composable<TraditionEditDestination> { TraditionEditRoute(onDone = { navController.popBackStack() }) }
}
