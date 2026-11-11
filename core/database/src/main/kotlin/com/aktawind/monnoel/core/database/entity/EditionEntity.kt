package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.EditionStatus
import java.time.Instant

@Entity(
    tableName = "christmas_edition",
    indices = [Index(value = ["year"], unique = true)],
)
data class EditionEntity(
    @PrimaryKey val id: String,
    val year: Int,
    val status: String,
    @ColumnInfo(name = "planned_budget_cents") val plannedBudgetCents: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun EditionEntity.asModel() = ChristmasEdition(
    id = id,
    year = year,
    status = runCatching { EditionStatus.valueOf(status) }.getOrDefault(EditionStatus.ACTIVE),
    plannedBudgetCents = plannedBudgetCents,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun ChristmasEdition.asEntity() = EditionEntity(
    id = id,
    year = year,
    status = status.name,
    plannedBudgetCents = plannedBudgetCents,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)
