package com.aktawind.monnoel.core.domain.search

import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.Expense
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.LutinTask
import com.aktawind.monnoel.core.model.Meal
import com.aktawind.monnoel.core.model.MealDish
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.model.Production
import com.aktawind.monnoel.core.model.Recipe
import java.text.Normalizer

/** Un résultat de recherche : de quoi l'afficher et l'ouvrir. */
data class SearchResult(
    val type: LinkType,
    val id: String,
    val title: String,
    val subtitle: String,
    val photoPath: String?,
)

/** Tout ce qui peut être cherché. Les années servent à situer cadeaux et calendriers. */
data class SearchData(
    val persons: List<Person>,
    val ideas: List<GiftIdea>,
    val gifts: List<PlannedGift>,
    val calendars: List<AdventCalendar>,
    val boxes: List<AdventBox>,
    val tasks: List<LutinTask>,
    val years: Map<String, Int>,
    val recipes: List<Recipe> = emptyList(),
    val productions: List<Production> = emptyList(),
    val meals: List<Meal> = emptyList(),
    val dishes: List<MealDish> = emptyList(),
    val expenses: List<Expense> = emptyList(),
)

/** Minuscules, sans accents : « Noël » et « noel » se valent. */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()

/**
 * Cherche [query] dans tout le carnet. Chaque mot doit apparaître (dans n'importe quel champ
 * de l'élément). Résultats regroupés par type, dans l'ordre d'affichage.
 */
fun searchAll(query: String, data: SearchData, limitPerType: Int = 30): Map<LinkType, List<SearchResult>> {
    val words = normalizeForSearch(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyMap()
    fun matches(vararg fields: String?): Boolean {
        val haystack = normalizeForSearch(fields.filterNotNull().joinToString(" "))
        return words.all { it in haystack }
    }
    val personNames = data.persons.associate { it.id to it.displayName }
    val calendarNames = data.calendars.associate { it.id to it.name }
    val calendarEditions = data.calendars.associate { it.id to it.editionId }
    fun year(editionId: String?) = editionId?.let { data.years[it] }?.let { "Noël $it" }

    val results = linkedMapOf<LinkType, List<SearchResult>>()
    results[LinkType.PERSON] = data.persons.filter { matches(it.displayName, it.interests, it.notes) }
        .map { SearchResult(LinkType.PERSON, it.id, it.displayName, it.interests, it.photoPath) }
    results[LinkType.IDEA] = data.ideas.filter { matches(it.title, it.description, it.shop, it.notes) }
        .map { idea ->
            val forNames = idea.personIds.mapNotNull(personNames::get).joinToString(", ")
            SearchResult(
                LinkType.IDEA,
                idea.id,
                idea.title,
                listOf(forNames, idea.shop).filter {
                    it.isNotBlank()
                }.joinToString(" · "),
                idea.photoPath,
            )
        }
    results[LinkType.GIFT] = data.gifts.filter { matches(it.title, it.description, it.recipientName, it.shop, it.notes) }
        .sortedByDescending { data.years[it.editionId] ?: 0 }
        .map { gift ->
            SearchResult(
                LinkType.GIFT,
                gift.id,
                gift.title,
                listOfNotNull(gift.recipientName.ifBlank { null }, year(gift.editionId)).joinToString(" · "),
                gift.photoPath,
            )
        }
    results[LinkType.CALENDAR] = data.calendars.filter { matches(it.name, it.theme, it.description, it.notes) }
        .map {
            SearchResult(
                LinkType.CALENDAR,
                it.id,
                it.name,
                listOfNotNull(
                    it.theme.ifBlank {
                        null
                    },
                    year(it.editionId),
                ).joinToString(" · "),
                it.coverPhotoPath,
            )
        }
    // Les cases identiques d'un même calendrier (ex. 24 « Chocolat ») ne font qu'un résultat.
    results[LinkType.BOX] = data.boxes.filter { !it.isEmpty && matches(it.title, it.supplier, it.notes) }
        .groupBy { it.calendarId to normalizeForSearch(it.title) }
        .values
        .map { group ->
            val first = group.minBy { it.number }
            val numbers = group.map { it.number }.sorted()
            val where = if (numbers.size == 1) "case ${numbers.single()}" else "cases ${numbers.joinToString(", ")}"
            SearchResult(
                LinkType.BOX,
                first.id,
                first.title,
                listOfNotNull(calendarNames[first.calendarId], where, year(calendarEditions[first.calendarId])).joinToString(" · "),
                group.firstNotNullOfOrNull { it.photoPath },
            )
        }
    results[LinkType.RECIPE] = data.recipes.filter { matches(it.name) }
        .map { SearchResult(LinkType.RECIPE, it.id, it.name, "", it.photoPath) }
    results[LinkType.PRODUCTION] = data.productions.filter { matches(it.recipeName, it.destination, it.notes) }
        .sortedByDescending { data.years[it.editionId] ?: 0 }
        .map {
            SearchResult(
                LinkType.PRODUCTION,
                it.id,
                if (it.quantity > 1) "${it.recipeName} × ${it.quantity}" else it.recipeName,
                listOfNotNull(it.destination.ifBlank { null }, year(it.editionId)).joinToString(" · "),
                it.photoPath ?: it.recipePhotoPath,
            )
        }
    // Un repas est trouvé par son nom, son lieu, ses invités… ou par l'un de ses plats.
    val dishesByMeal = data.dishes.groupBy { it.mealId }
    results[LinkType.MEAL] = data.meals.mapNotNull { meal ->
        val ownMatch = matches(meal.name, meal.place, meal.guests.joinToString(" "), meal.notes)
        val dishes = dishesByMeal[meal.id].orEmpty().filter { matches(it.name, it.broughtBy) }
        if (!ownMatch && dishes.isEmpty()) return@mapNotNull null
        val detail = dishes.joinToString(", ") { it.name }.ifBlank { null }
        meal to SearchResult(
            LinkType.MEAL,
            meal.id,
            meal.name,
            listOfNotNull(detail, year(meal.editionId)).joinToString(" · "),
            dishes.firstNotNullOfOrNull { it.photoPath },
        )
    }.sortedByDescending { data.years[it.first.editionId] ?: 0 }.map { it.second }
    results[LinkType.EXPENSE] = data.expenses.filter { matches(it.title, it.notes) }
        .sortedByDescending { data.years[it.editionId] ?: 0 }
        .map { SearchResult(LinkType.EXPENSE, it.id, it.title, year(it.editionId).orEmpty(), null) }
    results[LinkType.TASK] = data.tasks.filter { matches(it.title, it.description, it.notes) }
        .map { SearchResult(LinkType.TASK, it.id, it.title, year(it.editionId).orEmpty(), null) }
    return results.mapValues { it.value.take(limitPerType) }.filterValues { it.isNotEmpty() }
}
