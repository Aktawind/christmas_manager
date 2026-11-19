package com.aktawind.monnoel.feature.traditions

import androidx.annotation.StringRes
import com.aktawind.monnoel.core.model.TraditionCategory
import com.aktawind.monnoel.core.model.TraditionPeriod

@get:StringRes
internal val TraditionCategory.label: Int
    get() = when (this) {
        TraditionCategory.HOME -> R.string.category_home
        TraditionCategory.OUTINGS -> R.string.category_outings
        TraditionCategory.COOKING -> R.string.category_cooking
        TraditionCategory.FILMS_BOOKS -> R.string.category_films_books
        TraditionCategory.FAMILY -> R.string.category_family
        TraditionCategory.OTHER -> R.string.category_other
    }

internal val TraditionCategory.emoji: String
    get() = when (this) {
        TraditionCategory.HOME -> "🏡"
        TraditionCategory.OUTINGS -> "🎠"
        TraditionCategory.COOKING -> "🍪"
        TraditionCategory.FILMS_BOOKS -> "🎬"
        TraditionCategory.FAMILY -> "👨‍👩‍👧"
        TraditionCategory.OTHER -> "✨"
    }

@get:StringRes
internal val TraditionPeriod.label: Int
    get() = when (this) {
        TraditionPeriod.ADVENT -> R.string.period_advent
        TraditionPeriod.EVE -> R.string.period_eve
        TraditionPeriod.DAY -> R.string.period_day
        TraditionPeriod.AFTER -> R.string.period_after
    }
