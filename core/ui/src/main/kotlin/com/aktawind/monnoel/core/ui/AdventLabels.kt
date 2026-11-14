package com.aktawind.monnoel.core.ui

import androidx.annotation.StringRes
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.SurpriseType

val SurpriseType.emoji: String
    get() = when (this) {
        SurpriseType.BOUGHT -> "🛍️"
        SurpriseType.HANDMADE -> "✂️"
        SurpriseType.TREAT -> "🍬"
        SurpriseType.ACTIVITY -> "🎨"
        SurpriseType.MESSAGE -> "💌"
        SurpriseType.EXPERIENCE -> "🎟️"
        SurpriseType.PHOTO -> "📷"
        SurpriseType.GAME -> "🧩"
    }

@get:StringRes
val SurpriseType.label: Int
    get() = when (this) {
        SurpriseType.BOUGHT -> R.string.surprise_bought
        SurpriseType.HANDMADE -> R.string.surprise_handmade
        SurpriseType.TREAT -> R.string.surprise_treat
        SurpriseType.ACTIVITY -> R.string.surprise_activity
        SurpriseType.MESSAGE -> R.string.surprise_message
        SurpriseType.EXPERIENCE -> R.string.surprise_experience
        SurpriseType.PHOTO -> R.string.surprise_photo
        SurpriseType.GAME -> R.string.surprise_game
    }

@get:StringRes
val BoxStatus.label: Int
    get() = when (this) {
        BoxStatus.TO_FIND -> R.string.box_status_to_find
        BoxStatus.TO_PREPARE -> R.string.box_status_to_prepare
        BoxStatus.READY -> R.string.box_status_ready
    }
