package com.aktawind.monnoel.feature.budget

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.aktawind.monnoel.core.domain.budget.BudgetPost
import com.aktawind.monnoel.core.model.ExpenseCategory

@get:StringRes
internal val BudgetPost.label: Int
    get() = when (this) {
        BudgetPost.GIFTS -> R.string.post_gifts
        BudgetPost.ADVENT -> R.string.post_advent
        BudgetPost.MEALS -> R.string.post_meals
        BudgetPost.CRAFTS -> R.string.post_crafts
        BudgetPost.OTHER -> R.string.post_other
    }

internal val BudgetPost.emoji: String
    get() = when (this) {
        BudgetPost.GIFTS -> "🎁"
        BudgetPost.ADVENT -> "🗓️"
        BudgetPost.MEALS -> "🍽️"
        BudgetPost.CRAFTS -> "✂️"
        BudgetPost.OTHER -> "✨"
    }

/**
 * Couleurs des postes dans les graphiques : palette vérifiée (daltonisme, contraste) pour le
 * fond ivoire et, séparément, pour le fond vert nuit. L'ordre suit les postes (jamais recyclé).
 */
@Composable
@ReadOnlyComposable
internal fun BudgetPost.chartColor(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (this) {
        BudgetPost.GIFTS -> if (dark) Color(0xFF3A9E6F) else Color(0xFF1E8A5C)
        BudgetPost.ADVENT -> if (dark) Color(0xFF5B93D3) else Color(0xFF3F7FC0)
        BudgetPost.MEALS -> if (dark) Color(0xFFB58C2A) else Color(0xFFC99A2E)
        BudgetPost.CRAFTS -> if (dark) Color(0xFF9C7BD0) else Color(0xFF8A5AB5)
        BudgetPost.OTHER -> if (dark) Color(0xFFD0566C) else Color(0xFFB8384E)
    }
}

@get:StringRes
internal val ExpenseCategory.label: Int
    get() = when (this) {
        ExpenseCategory.TREE -> R.string.expense_tree
        ExpenseCategory.DECORATION -> R.string.expense_decoration
        ExpenseCategory.WRAPPING -> R.string.expense_wrapping
        ExpenseCategory.CLOTHES -> R.string.expense_clothes
        ExpenseCategory.OUTINGS -> R.string.expense_outings
        ExpenseCategory.OTHER -> R.string.expense_other
    }

internal val ExpenseCategory.emoji: String
    get() = when (this) {
        ExpenseCategory.TREE -> "🎄"
        ExpenseCategory.DECORATION -> "✨"
        ExpenseCategory.WRAPPING -> "🎀"
        ExpenseCategory.CLOTHES -> "🧣"
        ExpenseCategory.OUTINGS -> "🎠"
        ExpenseCategory.OTHER -> "🧾"
    }
