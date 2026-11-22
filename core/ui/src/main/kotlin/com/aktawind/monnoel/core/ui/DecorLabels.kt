package com.aktawind.monnoel.core.ui

import androidx.annotation.StringRes
import com.aktawind.monnoel.core.model.DecorPlace

@get:StringRes
val DecorPlace.label: Int
    get() = when (this) {
        DecorPlace.TREE -> R.string.place_tree
        DecorPlace.TABLE -> R.string.place_table
        DecorPlace.ENTRANCE -> R.string.place_entrance
        DecorPlace.WINDOWS -> R.string.place_windows
        DecorPlace.OUTDOOR -> R.string.place_outdoor
        DecorPlace.OTHER -> R.string.place_other
    }

val DecorPlace.emoji: String
    get() = when (this) {
        DecorPlace.TREE -> "🎄"
        DecorPlace.TABLE -> "🍽️"
        DecorPlace.ENTRANCE -> "🚪"
        DecorPlace.WINDOWS -> "❄️"
        DecorPlace.OUTDOOR -> "🏡"
        DecorPlace.OTHER -> "✨"
    }
