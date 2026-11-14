package com.aktawind.monnoel.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aktawind.monnoel.core.database.dao.AdventDao
import com.aktawind.monnoel.core.database.dao.EditionDao
import com.aktawind.monnoel.core.database.dao.GiftCategoryDao
import com.aktawind.monnoel.core.database.dao.GiftIdeaDao
import com.aktawind.monnoel.core.database.dao.PersonDao
import com.aktawind.monnoel.core.database.dao.PlannedGiftDao
import com.aktawind.monnoel.core.database.entity.AdventBoxEntity
import com.aktawind.monnoel.core.database.entity.AdventCalendarEntity
import com.aktawind.monnoel.core.database.entity.AdventCalendarPersonEntity
import com.aktawind.monnoel.core.database.entity.AdventSupplyEntity
import com.aktawind.monnoel.core.database.entity.AdventTemplateBoxEntity
import com.aktawind.monnoel.core.database.entity.AdventTemplateEntity
import com.aktawind.monnoel.core.database.entity.EditionEntity
import com.aktawind.monnoel.core.database.entity.GiftCategoryEntity
import com.aktawind.monnoel.core.database.entity.GiftIdeaEntity
import com.aktawind.monnoel.core.database.entity.IdeaPersonEntity
import com.aktawind.monnoel.core.database.entity.PersonBudgetEntity
import com.aktawind.monnoel.core.database.entity.PersonEntity
import com.aktawind.monnoel.core.database.entity.PlannedGiftEntity

/**
 * Base unique de l'application (ADR 0002). Toute évolution du schéma doit être
 * accompagnée d'une migration testée — jamais de migration destructive.
 *
 * Historique : v1 (v0.1.0) années ; v2 (v0.2.0 tests) personnes, idées, cadeaux, catégories,
 * enveloppes, souhaits ; v3 (v0.2.0) suppression de la liste de souhaits ;
 * v4 (v0.3.0) calendriers de l'Avent, cases, fournitures, modèles.
 */
@Database(
    entities = [
        EditionEntity::class,
        PersonEntity::class,
        GiftCategoryEntity::class,
        GiftIdeaEntity::class,
        IdeaPersonEntity::class,
        PlannedGiftEntity::class,
        PersonBudgetEntity::class,
        AdventCalendarEntity::class,
        AdventCalendarPersonEntity::class,
        AdventSupplyEntity::class,
        AdventBoxEntity::class,
        AdventTemplateEntity::class,
        AdventTemplateBoxEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class MonNoelDatabase : RoomDatabase() {
    abstract fun editionDao(): EditionDao

    abstract fun personDao(): PersonDao

    abstract fun giftCategoryDao(): GiftCategoryDao

    abstract fun giftIdeaDao(): GiftIdeaDao

    abstract fun plannedGiftDao(): PlannedGiftDao

    abstract fun adventDao(): AdventDao

    companion object {
        const val NAME = "mon-noel.db"
    }
}
