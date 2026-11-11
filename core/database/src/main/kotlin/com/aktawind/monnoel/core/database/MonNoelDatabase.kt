package com.aktawind.monnoel.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aktawind.monnoel.core.database.dao.EditionDao
import com.aktawind.monnoel.core.database.dao.GiftCategoryDao
import com.aktawind.monnoel.core.database.dao.GiftIdeaDao
import com.aktawind.monnoel.core.database.dao.PersonDao
import com.aktawind.monnoel.core.database.dao.PlannedGiftDao
import com.aktawind.monnoel.core.database.entity.EditionEntity
import com.aktawind.monnoel.core.database.entity.GiftCategoryEntity
import com.aktawind.monnoel.core.database.entity.GiftIdeaEntity
import com.aktawind.monnoel.core.database.entity.IdeaPersonEntity
import com.aktawind.monnoel.core.database.entity.PersonBudgetEntity
import com.aktawind.monnoel.core.database.entity.PersonEntity
import com.aktawind.monnoel.core.database.entity.PlannedGiftEntity
import com.aktawind.monnoel.core.database.entity.WishItemEntity

/**
 * Base unique de l'application (ADR 0002). Toute évolution du schéma doit être
 * accompagnée d'une migration testée — jamais de migration destructive.
 *
 * Historique : v1 (v0.1.0) années ; v2 (v0.2.0) personnes, idées, cadeaux, catégories, enveloppes.
 */
@Database(
    entities = [
        EditionEntity::class,
        PersonEntity::class,
        WishItemEntity::class,
        GiftCategoryEntity::class,
        GiftIdeaEntity::class,
        IdeaPersonEntity::class,
        PlannedGiftEntity::class,
        PersonBudgetEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class MonNoelDatabase : RoomDatabase() {
    abstract fun editionDao(): EditionDao

    abstract fun personDao(): PersonDao

    abstract fun giftCategoryDao(): GiftCategoryDao

    abstract fun giftIdeaDao(): GiftIdeaDao

    abstract fun plannedGiftDao(): PlannedGiftDao

    companion object {
        const val NAME = "mon-noel.db"
    }
}
