package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.Expense
import com.aktawind.monnoel.core.model.ExpenseCategory
import java.time.Instant

@Entity(
    tableName = "expense",
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("edition_id")],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "edition_id") val editionId: String,
    val title: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val category: String,
    val paid: Boolean,
    val notes: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun ExpenseEntity.asModel() = Expense(
    id, editionId, title, amountCents, runCatching { ExpenseCategory.valueOf(category) }.getOrDefault(ExpenseCategory.OTHER), paid, notes,
    Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt),
)

fun Expense.asEntity() = ExpenseEntity(
    id, editionId, title, amountCents, category.name, paid, notes, createdAt.toEpochMilli(), updatedAt.toEpochMilli(),
)
