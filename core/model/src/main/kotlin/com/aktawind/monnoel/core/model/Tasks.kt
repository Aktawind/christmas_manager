package com.aktawind.monnoel.core.model

import java.time.Instant

/** Ma liste de lutin : des choses à faire, sans date ni priorité (ADR 0006). */
data class LutinTask(
    val id: String,
    val editionId: String,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val module: TaskModule,
    val notes: String,
    /** Élément lié (cadeau, calendrier…), facultatif. */
    val linkType: LinkType?,
    val linkId: String?,
    /** Clé de la suggestion d'origine : la tâche se coche toute seule quand c'est fait. */
    val suggestionKey: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isOpen: Boolean get() = status == TaskStatus.TODO || status == TaskStatus.IN_PROGRESS
}

enum class TaskStatus { TODO, IN_PROGRESS, DONE, CANCELLED }

enum class TaskModule { GIFTS, ADVENT, COOKING, MEALS, HOME, OTHER }

enum class LinkType { PERSON, IDEA, GIFT, CALENDAR, BOX, TASK, RECIPE, PRODUCTION, MEAL, EXPENSE, WHISPER }

/** Types de suggestions automatiques, activables un par un dans les réglages. */
enum class SuggestionKind { GIFT_DELIVERY, GIFT_WRAP, SUPPLY_BUY, ADVENT_FIND, ADVENT_PREPARE, PRODUCTION_MAKE, MEAL_MENU, DISH_ORDER }
