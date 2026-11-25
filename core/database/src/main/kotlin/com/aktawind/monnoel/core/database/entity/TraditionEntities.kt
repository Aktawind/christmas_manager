package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.Inspiration
import com.aktawind.monnoel.core.model.InspirationKind
import com.aktawind.monnoel.core.model.Tradition
import com.aktawind.monnoel.core.model.TraditionCategory
import com.aktawind.monnoel.core.model.TraditionPeriod
import com.aktawind.monnoel.core.model.TraditionYear
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "tradition")
data class TraditionEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    val category: String,
    val period: String,
    val notes: String,
    val favorite: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "tradition_person",
    primaryKeys = ["tradition_id", "person_id"],
    foreignKeys = [
        ForeignKey(entity = TraditionEntity::class, parentColumns = ["id"], childColumns = ["tradition_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["person_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("person_id")],
)
data class TraditionPersonEntity(
    @ColumnInfo(name = "tradition_id") val traditionId: String,
    @ColumnInfo(name = "person_id") val personId: String,
)

@Entity(
    tableName = "tradition_year",
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TraditionEntity::class, parentColumns = ["id"], childColumns = ["tradition_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["edition_id", "tradition_id"], unique = true), Index("tradition_id")],
)
data class TraditionYearEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "edition_id") val editionId: String,
    @ColumnInfo(name = "tradition_id") val traditionId: String,
    val done: Boolean,
    @ColumnInfo(name = "memory_photo_path") val memoryPhotoPath: String?,
    @ColumnInfo(name = "memory_note") val memoryNote: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** Date prévue cette année (ISO-8601), facultative. */
    val date: String? = null,
)

@Entity(tableName = "inspiration")
data class InspirationEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    val url: String,
    val notes: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Inspiration vue / faite pendant un Noël. */
@Entity(
    tableName = "inspiration_done",
    primaryKeys = ["edition_id", "inspiration_id"],
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = InspirationEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspiration_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("inspiration_id")],
)
data class InspirationDoneEntity(
    @ColumnInfo(name = "edition_id") val editionId: String,
    @ColumnInfo(name = "inspiration_id") val inspirationId: String,
)

/** Date prévue d'une inspiration pour un Noël. */
@Entity(
    tableName = "inspiration_date",
    primaryKeys = ["edition_id", "inspiration_id"],
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = InspirationEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspiration_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("inspiration_id")],
)
data class InspirationDateEntity(
    @ColumnInfo(name = "edition_id") val editionId: String,
    @ColumnInfo(name = "inspiration_id") val inspirationId: String,
    val date: String,
)

fun TraditionEntity.asModel(personIds: Set<String>) = Tradition(
    id, name, photoPath,
    runCatching { TraditionCategory.valueOf(category) }.getOrDefault(TraditionCategory.OTHER),
    runCatching { TraditionPeriod.valueOf(period) }.getOrDefault(TraditionPeriod.ADVENT),
    personIds, notes, favorite, Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt),
)

fun Tradition.asEntity() =
    TraditionEntity(id, name, photoPath, category.name, period.name, notes, favorite, createdAt.toEpochMilli(), updatedAt.toEpochMilli())

fun TraditionYearEntity.asModel() = TraditionYear(
    id,
    editionId,
    traditionId,
    done,
    memoryPhotoPath,
    memoryNote,
    Instant.ofEpochMilli(createdAt),
    date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
)

fun TraditionYear.asEntity() =
    TraditionYearEntity(id, editionId, traditionId, done, memoryPhotoPath, memoryNote, createdAt.toEpochMilli(), date?.toString())

fun InspirationEntity.asModel() = Inspiration(
    id,
    runCatching { InspirationKind.valueOf(kind) }.getOrDefault(InspirationKind.ACTIVITY),
    title,
    photoPath,
    url,
    notes,
    Instant.ofEpochMilli(createdAt),
    Instant.ofEpochMilli(updatedAt),
)

fun Inspiration.asEntity() = InspirationEntity(
    id,
    kind.name,
    title,
    photoPath,
    url,
    notes,
    createdAt.toEpochMilli(),
    updatedAt.toEpochMilli(),
)
