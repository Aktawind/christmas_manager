package com.aktawind.monnoel.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aktawind.monnoel.core.database.dao.EditionDao
import com.aktawind.monnoel.core.database.entity.EditionEntity

/**
 * Base unique de l'application (ADR 0002). Toute évolution du schéma doit être
 * accompagnée d'une migration testée — jamais de migration destructive.
 */
@Database(
    entities = [EditionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MonNoelDatabase : RoomDatabase() {
    abstract fun editionDao(): EditionDao

    companion object {
        const val NAME = "mon-noel.db"
    }
}
