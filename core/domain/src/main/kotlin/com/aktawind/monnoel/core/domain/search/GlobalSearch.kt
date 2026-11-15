package com.aktawind.monnoel.core.domain.search

import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.LutinTask
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PlannedGift
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
    results[LinkType.BOX] = data.boxes.filter { !it.isEmpty && matches(it.title, it.supplier, it.notes) }
        .map { box ->
            SearchResult(
                LinkType.BOX,
                box.id,
                box.title,
                listOfNotNull(
                    calendarNames[box.calendarId],
                    "case ${box.number}",
                    year(calendarEditions[box.calendarId]),
                ).joinToString(" · "),
                box.photoPath,
            )
        }
    results[LinkType.TASK] = data.tasks.filter { matches(it.title, it.description, it.notes) }
        .map { SearchResult(LinkType.TASK, it.id, it.title, year(it.editionId).orEmpty(), null) }
    return results.mapValues { it.value.take(limitPerType) }.filterValues { it.isNotEmpty() }
}
