package com.aktawind.monnoel.core.domain.tasks

import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.LutinTask
import com.aktawind.monnoel.core.model.PlannedGift
import com.aktawind.monnoel.core.model.SuggestionKind
import com.aktawind.monnoel.core.model.TaskModule

/**
 * Une chose à faire repérée par l'application. [subject] sert au libellé (nom du cadeau,
 * du calendrier…), [count] au nombre de cases concernées.
 */
data class Suggestion(
    val key: String,
    val kind: SuggestionKind,
    val module: TaskModule,
    val subject: String,
    val detail: String,
    val count: Int,
    val linkType: LinkType,
    val linkId: String,
)

/** Données d'un Noël utiles aux suggestions. */
data class SuggestionSource(
    val gifts: List<PlannedGift>,
    val calendars: List<AdventCalendar>,
    val boxes: List<AdventBox>,
    val supplies: List<AdventSupply>,
)

/** État de chaque suggestion possible : clé -> faite (true) ou encore à faire (false). */
fun suggestionStates(source: SuggestionSource): Map<String, Boolean> {
    val states = mutableMapOf<String, Boolean>()
    source.gifts.forEach { gift ->
        val step = gift.status.ordinal
        states["${SuggestionKind.GIFT_DELIVERY.name}:${gift.id}"] = step >= GiftStatus.RECEIVED.ordinal
        states["${SuggestionKind.GIFT_WRAP.name}:${gift.id}"] = step >= GiftStatus.WRAPPED.ordinal
    }
    source.supplies.forEach { states["${SuggestionKind.SUPPLY_BUY.name}:${it.id}"] = it.bought }
    source.calendars.forEach { calendar ->
        val own = source.boxes.filter { it.calendarId == calendar.id }
        val toFind = own.count { it.status == BoxStatus.TO_FIND }
        val toPrepare = own.count { it.status == BoxStatus.TO_PREPARE }
        states["${SuggestionKind.ADVENT_FIND.name}:${calendar.id}"] = toFind == 0
        states["${SuggestionKind.ADVENT_PREPARE.name}:${calendar.id}"] = toFind == 0 && toPrepare == 0
    }
    return states
}

/**
 * Suggestions à proposer : ce qui est à faire maintenant, hors types désactivés, suggestions
 * supprimées et suggestions déjà ajoutées à la liste.
 */
fun currentSuggestions(
    source: SuggestionSource,
    disabled: Set<SuggestionKind>,
    dismissed: Set<String>,
    tasks: List<LutinTask>,
): List<Suggestion> {
    val taken = tasks.mapNotNull { it.suggestionKey }.toSet() + dismissed
    val calendarNames = source.calendars.associate { it.id to it.name }
    val result = mutableListOf<Suggestion>()
    fun add(s: Suggestion) {
        if (s.kind !in disabled && s.key !in taken) result += s
    }
    source.gifts.forEach { gift ->
        if (gift.status == GiftStatus.ORDERED) {
            add(
                Suggestion(
                    "${SuggestionKind.GIFT_DELIVERY.name}:${gift.id}",
                    SuggestionKind.GIFT_DELIVERY,
                    TaskModule.GIFTS,
                    gift.title,
                    gift.recipientName,
                    1,
                    LinkType.GIFT,
                    gift.id,
                ),
            )
        }
        if (gift.status == GiftStatus.TO_WRAP) {
            add(
                Suggestion(
                    "${SuggestionKind.GIFT_WRAP.name}:${gift.id}",
                    SuggestionKind.GIFT_WRAP,
                    TaskModule.GIFTS,
                    gift.title,
                    gift.recipientName,
                    1,
                    LinkType.GIFT,
                    gift.id,
                ),
            )
        }
    }
    source.supplies.filter { !it.bought }.forEach { supply ->
        add(
            Suggestion(
                "${SuggestionKind.SUPPLY_BUY.name}:${supply.id}",
                SuggestionKind.SUPPLY_BUY,
                TaskModule.ADVENT,
                supply.name,
                calendarNames[supply.calendarId].orEmpty(),
                supply.quantity,
                LinkType.CALENDAR,
                supply.calendarId,
            ),
        )
    }
    source.calendars.forEach { calendar ->
        val own = source.boxes.filter { it.calendarId == calendar.id }
        val toFind = own.count { it.status == BoxStatus.TO_FIND }
        val toPrepare = own.count { it.status == BoxStatus.TO_PREPARE }
        if (toFind > 0) {
            add(
                Suggestion(
                    "${SuggestionKind.ADVENT_FIND.name}:${calendar.id}",
                    SuggestionKind.ADVENT_FIND,
                    TaskModule.ADVENT,
                    calendar.name,
                    "",
                    toFind,
                    LinkType.CALENDAR,
                    calendar.id,
                ),
            )
        }
        if (toPrepare > 0) {
            add(
                Suggestion(
                    "${SuggestionKind.ADVENT_PREPARE.name}:${calendar.id}",
                    SuggestionKind.ADVENT_PREPARE,
                    TaskModule.ADVENT,
                    calendar.name,
                    "",
                    toPrepare,
                    LinkType.CALENDAR,
                    calendar.id,
                ),
            )
        }
    }
    return result
}
