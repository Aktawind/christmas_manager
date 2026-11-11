package com.aktawind.monnoel.core.database

import androidx.sqlite.db.SupportSQLiteDatabase

/** Catégories de cadeaux proposées au départ (modifiables dans les réglages). */
object DefaultData {
    val giftCategories: List<Pair<String, String>> = listOf(
        "cat-toys" to "Jouets",
        "cat-books" to "Livres",
        "cat-clothes" to "Vêtements & accessoires",
        "cat-beauty" to "Beauté & bien-être",
        "cat-home" to "Maison & déco",
        "cat-treats" to "Gourmandises",
        "cat-leisure" to "Loisirs & sport",
        "cat-tech" to "High-tech",
        "cat-experiences" to "Expériences & sorties",
        "cat-handmade" to "Fait main",
        "cat-other" to "Autre",
    )

    fun insertGiftCategories(db: SupportSQLiteDatabase) {
        giftCategories.forEachIndexed { index, (id, name) ->
            db.execSQL(
                "INSERT OR IGNORE INTO gift_category (id, name, sort_order) VALUES (?, ?, ?)",
                arrayOf<Any>(id, name, index),
            )
        }
    }
}
