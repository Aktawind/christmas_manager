package com.aktawind.monnoel.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.aktawind.monnoel.core.domain.tasks.Suggestion
import com.aktawind.monnoel.core.model.ProductionStatus
import com.aktawind.monnoel.core.model.SuggestionKind
import com.aktawind.monnoel.core.model.TaskModule
import com.aktawind.monnoel.core.model.TaskStatus

/** Libellé d'une suggestion, utilisé aussi comme titre de la tâche quand on l'ajoute. */
@Composable
fun Suggestion.label(): String = when (kind) {
    SuggestionKind.GIFT_DELIVERY ->
        if (detail.isBlank()) {
            stringResource(
                R.string.suggestion_delivery,
                subject,
            )
        } else {
            stringResource(R.string.suggestion_delivery_for, subject, detail)
        }
    SuggestionKind.GIFT_WRAP -> stringResource(R.string.suggestion_wrap, subject)
    SuggestionKind.SUPPLY_BUY ->
        if (detail.isBlank()) {
            stringResource(
                R.string.suggestion_buy,
                subject,
            )
        } else {
            stringResource(R.string.suggestion_buy_for, subject, detail)
        }
    SuggestionKind.ADVENT_FIND -> pluralStringResource(R.plurals.suggestion_find, count, count, subject)
    SuggestionKind.ADVENT_PREPARE -> pluralStringResource(R.plurals.suggestion_prepare, count, count, subject)
    SuggestionKind.PRODUCTION_MAKE ->
        if (count > 1) stringResource(R.string.suggestion_make_many, subject, count) else stringResource(R.string.suggestion_make, subject)
}

@get:StringRes
val SuggestionKind.settingLabel: Int
    get() = when (this) {
        SuggestionKind.GIFT_DELIVERY -> R.string.suggestion_kind_delivery
        SuggestionKind.GIFT_WRAP -> R.string.suggestion_kind_wrap
        SuggestionKind.SUPPLY_BUY -> R.string.suggestion_kind_buy
        SuggestionKind.ADVENT_FIND -> R.string.suggestion_kind_find
        SuggestionKind.ADVENT_PREPARE -> R.string.suggestion_kind_prepare
        SuggestionKind.PRODUCTION_MAKE -> R.string.suggestion_kind_make
    }

val TaskModule.emoji: String
    get() = when (this) {
        TaskModule.GIFTS -> "🎁"
        TaskModule.ADVENT -> "🗓️"
        TaskModule.COOKING -> "🧁"
        TaskModule.MEALS -> "🍽️"
        TaskModule.HOME -> "🏡"
        TaskModule.OTHER -> "✨"
    }

@get:StringRes
val TaskModule.label: Int
    get() = when (this) {
        TaskModule.GIFTS -> R.string.module_gifts
        TaskModule.ADVENT -> R.string.module_advent
        TaskModule.COOKING -> R.string.module_cooking
        TaskModule.MEALS -> R.string.module_meals
        TaskModule.HOME -> R.string.module_home
        TaskModule.OTHER -> R.string.module_other
    }

@get:StringRes
val ProductionStatus.label: Int
    get() = when (this) {
        ProductionStatus.TO_DO -> R.string.production_status_todo
        ProductionStatus.IN_PROGRESS -> R.string.production_status_in_progress
        ProductionStatus.DONE -> R.string.production_status_done
    }

val ProductionStatus.emoji: String
    get() = when (this) {
        ProductionStatus.TO_DO -> "🥣"
        ProductionStatus.IN_PROGRESS -> "👩‍🍳"
        ProductionStatus.DONE -> "✅"
    }

@get:StringRes
val TaskStatus.label: Int
    get() = when (this) {
        TaskStatus.TODO -> R.string.task_status_todo
        TaskStatus.IN_PROGRESS -> R.string.task_status_in_progress
        TaskStatus.DONE -> R.string.task_status_done
        TaskStatus.CANCELLED -> R.string.task_status_cancelled
    }
