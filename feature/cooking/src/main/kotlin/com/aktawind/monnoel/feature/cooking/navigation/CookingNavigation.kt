package com.aktawind.monnoel.feature.cooking.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.cooking.CookingRoute
import com.aktawind.monnoel.feature.cooking.production.ProductionEditRoute
import com.aktawind.monnoel.feature.cooking.recipe.RecipeEditRoute
import kotlinx.serialization.Serializable

@Serializable
data object CookingDestination

@Serializable
data class RecipeEditDestination(val recipeId: String? = null)

@Serializable
data class ProductionEditDestination(val productionId: String? = null, val recipeId: String? = null)

fun NavGraphBuilder.cookingGraph(navController: NavController) {
    composable<CookingDestination> {
        CookingRoute(
            onOpenRecipe = { navController.navigate(RecipeEditDestination(it)) },
            onNewRecipe = { navController.navigate(RecipeEditDestination()) },
            onOpenProduction = { navController.navigate(ProductionEditDestination(productionId = it)) },
            onNewProduction = { navController.navigate(ProductionEditDestination()) },
        )
    }
    composable<RecipeEditDestination> {
        RecipeEditRoute(
            onDone = { navController.popBackStack() },
            onMake = { recipeId ->
                navController.popBackStack()
                navController.navigate(ProductionEditDestination(recipeId = recipeId))
            },
        )
    }
    composable<ProductionEditDestination> { ProductionEditRoute(onDone = { navController.popBackStack() }) }
}
