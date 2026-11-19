package com.aktawind.monnoel.core.ui

import androidx.annotation.StringRes
import com.aktawind.monnoel.core.model.InspirationKind

@get:StringRes
val InspirationKind.label: Int
    get() = when (this) {
        InspirationKind.FILM -> R.string.kind_film
        InspirationKind.CARTOON -> R.string.kind_cartoon
        InspirationKind.BOOK -> R.string.kind_book
        InspirationKind.MUSIC -> R.string.kind_music
        InspirationKind.OUTING -> R.string.kind_outing
        InspirationKind.ACTIVITY -> R.string.kind_activity
    }

val InspirationKind.emoji: String
    get() = when (this) {
        InspirationKind.FILM -> "🎬"
        InspirationKind.CARTOON -> "📺"
        InspirationKind.BOOK -> "📚"
        InspirationKind.MUSIC -> "🎵"
        InspirationKind.OUTING -> "🎠"
        InspirationKind.ACTIVITY -> "✂️"
    }
