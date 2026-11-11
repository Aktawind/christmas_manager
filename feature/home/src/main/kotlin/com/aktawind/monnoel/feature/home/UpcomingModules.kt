package com.aktawind.monnoel.feature.home

import androidx.annotation.StringRes

/** Modules annoncés sur l'accueil, avec la version où ils arrivent (docs/ROADMAP.md). */
internal data class UpcomingModule(val emoji: String, @StringRes val title: Int, val version: String)

internal val upcomingModules = listOf(
    UpcomingModule("🗓️", R.string.module_advent, "0.3"),
    UpcomingModule("📝", R.string.module_tasks, "0.4"),
    UpcomingModule("🧁", R.string.module_recipes, "0.5"),
    UpcomingModule("🍽️", R.string.module_meals, "0.6"),
    UpcomingModule("💰", R.string.module_budget, "0.7"),
    UpcomingModule("❤️", R.string.module_traditions, "0.8"),
    UpcomingModule("✂️", R.string.module_activities, "0.9"),
    UpcomingModule("📖", R.string.module_memories, "0.10"),
)
