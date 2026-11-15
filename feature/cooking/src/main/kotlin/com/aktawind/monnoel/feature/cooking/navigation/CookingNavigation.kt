package com.aktawind.monnoel.feature.cooking.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.cooking.CookingRoute
import com.aktawind.monnoel.feature.cooking.meal.DishEditRoute
import com.aktawind.monnoel.feature.cooking.meal.MealEditRoute
import com.aktawind.monnoel.feature.cooking.meal.MealRoute
import com.aktawind.monnoel.feature.cooking.production.ProductionEditRoute
import com.aktawind.monnoel.feature.cooking.recipe.RecipeEditRoute
import kotlinx.serialization.Serializable

@Serializable
data object CookingDestination

@Serializable
data class RecipeEditDestination(val recipeId: String? = null)

@Serializable
data class ProductionEditDestination(val productionId: String? = null, val recipeId: String? = null)

@Serializable
data class MealDestination(val mealId: String)

@Serializable
data class MealEditDestination(val mealId: String? = null)

/** [course] : nom d'un `CourseType`, pour un nouveau plat. */
@Serializable
data class DishEditDestination(val mealId: String, val dishId: String? = null, val course: String? = null)

fun NavGraphBuilder.cookingGraph(navController: NavController) {
    composable<CookingDestination> {
        CookingRoute(
            onOpenRecipe = { navController.navigate(RecipeEditDestination(it)) },
            onNewRecipe = { navController.navigate(RecipeEditDestination()) },
            onOpenProduction = { navController.navigate(ProductionEditDestination(productionId = it)) },
            onNewProduction = { navController.navigate(ProductionEditDestination()) },
            onOpenMeal = { navController.navigate(MealDestination(it)) },
            onNewMeal = { navController.navigate(MealEditDestination()) },
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
    composable<MealDestination> {
        MealRoute(
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate(MealEditDestination(it)) },
            onAddDish = { mealId, course -> navController.navigate(DishEditDestination(mealId, course = course.name)) },
            onOpenDish = { mealId, dishId -> navController.navigate(DishEditDestination(mealId, dishId = dishId)) },
        )
    }
    composable<MealEditDestination> {
        MealEditRoute(
            onSaved = { id ->
                navController.popBackStack()
                // Nouveau repas : on ouvre sa fiche pour composer le menu.
                if (navController.currentBackStackEntry?.destination?.hasRoute<MealDestination>() != true) {
                    navController.navigate(MealDestination(id))
                }
            },
            onDeleted = {
                if (!navController.popBackStack<MealDestination>(inclusive = true)) navController.popBackStack()
            },
            onBack = { navController.popBackStack() },
        )
    }
    composable<DishEditDestination> { DishEditRoute(onDone = { navController.popBackStack() }) }
}
