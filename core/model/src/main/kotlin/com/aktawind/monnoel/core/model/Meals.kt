package com.aktawind.monnoel.core.model

import java.time.Instant
import java.time.LocalDate

enum class MealType { CHRISTMAS_EVE, CHRISTMAS_DAY, BRUNCH, SNACK, FRIENDS, OTHER }

/** Les moments du menu, dans l'ordre du repas. */
enum class CourseType { APERITIF, STARTER, MAIN, CHEESE, DESSERT, DRINKS }

/** Un repas de fête d'une année : quand, où, avec qui, et un coût estimé facultatif. */
data class Meal(
    val id: String,
    val editionId: String,
    val name: String,
    val type: MealType,
    val date: LocalDate?,
    val place: String,
    /** Convives présents dans l'application (personnes des cadeaux). */
    val personIds: Set<String>,
    /** Autres invités, en texte libre. */
    val guests: List<String>,
    val estimatedCostCents: Long?,
    val notes: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val guestCount: Int get() = personIds.size + guests.size
}

/** Un plat du menu : une recette de la bibliothèque (copie figée) ou un nom libre. */
data class MealDish(
    val id: String,
    val mealId: String,
    val course: CourseType,
    val recipeId: String?,
    val name: String,
    val photoPath: String?,
    /** Qui apporte ce plat (facultatif). */
    val broughtBy: String,
    /** À commander (traiteur, poissonnier…) ; [ordered] quand c'est fait. */
    val toOrder: Boolean,
    val ordered: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
)
