package com.aktawind.monnoel.feature.traditions.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.traditions.TraditionsRoute
import com.aktawind.monnoel.feature.traditions.edit.TraditionEditRoute
import kotlinx.serialization.Serializable

/** [inspirationId] : inspiration à ouvrir directement (depuis la recherche). */
@Serializable
data class TraditionsDestination(val inspirationId: String? = null)

@Serializable
data class TraditionEditDestination(val traditionId: String? = null)

fun NavGraphBuilder.traditionsGraph(navController: NavController) {
    composable<TraditionsDestination> {
        TraditionsRoute(
            onOpenTradition = { navController.navigate(TraditionEditDestination(it)) },
            onNewTradition = { navController.navigate(TraditionEditDestination()) },
        )
    }
    composable<TraditionEditDestination> { TraditionEditRoute(onDone = { navController.popBackStack() }) }
}
