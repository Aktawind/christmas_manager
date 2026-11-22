package com.aktawind.monnoel.feature.traditions

import androidx.annotation.StringRes
import com.aktawind.monnoel.core.model.CraftCategory
import com.aktawind.monnoel.core.model.CraftStatus

@get:StringRes
internal val CraftCategory.label: Int
    get() = when (this) {
        CraftCategory.DECORATION -> R.string.craft_cat_decoration
        CraftCategory.HANDMADE_GIFT -> R.string.craft_cat_handmade_gift
        CraftCategory.WRAPPING -> R.string.craft_cat_wrapping
        CraftCategory.CARDS -> R.string.craft_cat_cards
        CraftCategory.KIDS -> R.string.craft_cat_kids
        CraftCategory.OTHER -> R.string.craft_cat_other
    }

internal val CraftCategory.emoji: String
    get() = when (this) {
        CraftCategory.DECORATION -> "🎄"
        CraftCategory.HANDMADE_GIFT -> "🎁"
        CraftCategory.WRAPPING -> "🎀"
        CraftCategory.CARDS -> "💌"
        CraftCategory.KIDS -> "🧒"
        CraftCategory.OTHER -> "✂️"
    }

/** Avancement d'un bricolage ; null = simple idée. */
@StringRes
internal fun craftStatusLabel(status: CraftStatus?): Int = when (status) {
    null -> R.string.craft_status_idea
    CraftStatus.PLANNED -> R.string.craft_status_planned
    CraftStatus.IN_PROGRESS -> R.string.craft_status_in_progress
    CraftStatus.DONE -> R.string.craft_status_done
}

internal fun craftStatusEmoji(status: CraftStatus?): String = when (status) {
    null -> "💡"
    CraftStatus.PLANNED -> "📌"
    CraftStatus.IN_PROGRESS -> "✂️"
    CraftStatus.DONE -> "✅"
}

/** Les choix d'avancement, dans l'ordre. */
internal val craftStatusChoices: List<CraftStatus?> = listOf(null) + CraftStatus.entries
