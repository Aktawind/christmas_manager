package com.aktawind.monnoel.feature.budget.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.budget.BudgetRoute
import kotlinx.serialization.Serializable

/** [expenseId] : dépense à ouvrir directement (depuis la recherche). */
@Serializable
data class BudgetDestination(val expenseId: String? = null)

fun NavGraphBuilder.budgetScreen() {
    composable<BudgetDestination> { BudgetRoute() }
}
