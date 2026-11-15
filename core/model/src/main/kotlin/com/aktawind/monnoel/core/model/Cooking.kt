package com.aktawind.monnoel.core.model

import java.time.Instant

/** Une recette de la bibliothèque : juste un nom et une photo (les détails vivent ailleurs). */
data class Recipe(
    val id: String,
    val name: String,
    val photoPath: String?,
    val categoryId: String?,
    /** Lien facultatif vers la recette (site, autre application). */
    val url: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class RecipeCategory(val id: String, val name: String, val sortOrder: Int)

enum class ProductionStatus { TO_DO, IN_PROGRESS, DONE }

/** La réalisation d'une recette pour un Noël (copie figée du nom et de la photo). */
data class Production(
    val id: String,
    val editionId: String,
    val recipeId: String?,
    val recipeName: String,
    val recipePhotoPath: String?,
    val quantity: Int,
    /** Pour quoi : réveillon, famille, à offrir… (texte libre). */
    val destination: String,
    val status: ProductionStatus,
    /** Photo du résultat. */
    val photoPath: String?,
    val notes: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
