package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.AdventTemplate
import com.aktawind.monnoel.core.model.AdventTemplateBox
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.SurpriseType
import java.time.Instant

@Entity(
    tableName = "advent_calendar",
    foreignKeys = [
        ForeignKey(
            entity = EditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["edition_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("edition_id")],
)
data class AdventCalendarEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "edition_id") val editionId: String,
    val name: String,
    val description: String,
    @ColumnInfo(name = "cover_photo_path") val coverPhotoPath: String?,
    val theme: String,
    @ColumnInfo(name = "budget_cents") val budgetCents: Long?,
    val notes: String,
    @ColumnInfo(name = "box_count") val boxCount: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "advent_calendar_person",
    primaryKeys = ["calendar_id", "person_id"],
    foreignKeys = [
        ForeignKey(
            entity = AdventCalendarEntity::class,
            parentColumns = ["id"],
            childColumns = ["calendar_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("person_id")],
)
data class AdventCalendarPersonEntity(
    @ColumnInfo(name = "calendar_id") val calendarId: String,
    @ColumnInfo(name = "person_id") val personId: String,
)

@Entity(
    tableName = "advent_supply",
    foreignKeys = [
        ForeignKey(
            entity = AdventCalendarEntity::class,
            parentColumns = ["id"],
            childColumns = ["calendar_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("calendar_id")],
)
data class AdventSupplyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "calendar_id") val calendarId: String,
    val name: String,
    val quantity: Int,
    @ColumnInfo(name = "price_cents") val priceCents: Long?,
    val bought: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "advent_box",
    foreignKeys = [
        ForeignKey(
            entity = AdventCalendarEntity::class,
            parentColumns = ["id"],
            childColumns = ["calendar_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AdventSupplyEntity::class,
            parentColumns = ["id"],
            childColumns = ["supply_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("calendar_id"), Index("supply_id")],
)
data class AdventBoxEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "calendar_id") val calendarId: String,
    val number: Int,
    val type: String?,
    val title: String,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    @ColumnInfo(name = "price_cents") val priceCents: Long?,
    val supplier: String,
    val url: String,
    val status: String,
    val notes: String,
    @ColumnInfo(name = "supply_id") val supplyId: String?,
)

@Entity(tableName = "advent_template")
data class AdventTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val theme: String,
    val description: String,
    @ColumnInfo(name = "box_count") val boxCount: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "advent_template_box",
    foreignKeys = [
        ForeignKey(
            entity = AdventTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("template_id")],
)
data class AdventTemplateBoxEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_id") val templateId: String,
    val number: Int,
    val type: String?,
    val title: String,
    @ColumnInfo(name = "price_cents") val priceCents: Long?,
    val supplier: String,
    val url: String,
    val notes: String,
)

private fun surprise(value: String?) = value?.let { runCatching { SurpriseType.valueOf(it) }.getOrNull() }

fun AdventCalendarEntity.asModel(personIds: Set<String>) = AdventCalendar(
    id, editionId, name, personIds, description, coverPhotoPath, theme, budgetCents, notes, boxCount,
    Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt),
)

fun AdventCalendar.asEntity() = AdventCalendarEntity(
    id, editionId, name, description, coverPhotoPath, theme, budgetCents, notes, boxCount,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli(),
)

fun AdventBoxEntity.asModel() = AdventBox(
    id, calendarId, number, surprise(type), title, photoPath, priceCents, supplier, url,
    runCatching { BoxStatus.valueOf(status) }.getOrDefault(BoxStatus.TO_FIND), notes, supplyId,
)

fun AdventBox.asEntity() = AdventBoxEntity(
    id, calendarId, number, type?.name, title, photoPath, priceCents, supplier, url, status.name, notes, supplyId,
)

fun AdventSupplyEntity.asModel() = AdventSupply(id, calendarId, name, quantity, priceCents, bought, sortOrder)

fun AdventSupply.asEntity() = AdventSupplyEntity(id, calendarId, name, quantity, priceCents, bought, sortOrder)

fun AdventTemplateEntity.asModel() =
    AdventTemplate(id, name, theme, description, boxCount, Instant.ofEpochMilli(createdAt))

fun AdventTemplate.asEntity() = AdventTemplateEntity(id, name, theme, description, boxCount, createdAt.toEpochMilli())

fun AdventTemplateBoxEntity.asModel() =
    AdventTemplateBox(id, templateId, number, surprise(type), title, priceCents, supplier, url, notes)

fun AdventTemplateBox.asEntity() =
    AdventTemplateBoxEntity(id, templateId, number, type?.name, title, priceCents, supplier, url, notes)
