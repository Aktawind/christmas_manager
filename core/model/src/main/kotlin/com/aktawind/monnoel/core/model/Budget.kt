package com.aktawind.monnoel.core.model

import java.time.Instant

enum class ExpenseCategory { TREE, DECORATION, WRAPPING, CLOTHES, OUTINGS, OTHER }

/** Une dépense hors des autres modules (sapin, décoration, papier cadeau…). */
data class Expense(
    val id: String,
    val editionId: String,
    val title: String,
    val amountCents: Long,
    val category: ExpenseCategory,
    val paid: Boolean,
    val notes: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
