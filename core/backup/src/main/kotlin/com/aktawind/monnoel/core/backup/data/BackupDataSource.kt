package com.aktawind.monnoel.core.backup.data

import androidx.room.withTransaction
import com.aktawind.monnoel.core.backup.format.BackupData
import com.aktawind.monnoel.core.backup.format.EditionDto
import com.aktawind.monnoel.core.database.MonNoelDatabase
import com.aktawind.monnoel.core.database.entity.EditionEntity
import javax.inject.Inject

/** Lit toute la base pour l'export, et la remplace intégralement lors d'un import. */
class BackupDataSource @Inject constructor(private val db: MonNoelDatabase) {
    suspend fun snapshot(): BackupData = db.withTransaction {
        BackupData(
            editions = db.editionDao().getAll().map { it.toDto() },
        )
    }

    /** Remplacement complet dans une seule transaction : tout ou rien. */
    suspend fun replaceAll(data: BackupData) {
        db.withTransaction {
            db.editionDao().deleteAll()
            db.editionDao().insertAll(data.editions.map { it.toEntity() })
        }
    }
}

private fun EditionEntity.toDto() = EditionDto(
    id = id,
    year = year,
    status = status,
    plannedBudgetCents = plannedBudgetCents,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun EditionDto.toEntity() = EditionEntity(
    id = id,
    year = year,
    status = status,
    plannedBudgetCents = plannedBudgetCents,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
