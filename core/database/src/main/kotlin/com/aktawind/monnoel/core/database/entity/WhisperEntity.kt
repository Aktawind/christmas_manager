package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.WhisperedIdea
import java.time.Instant

@Entity(
    tableName = "whispered_idea",
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["recipient_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["given_to_person_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("edition_id"), Index("recipient_id"), Index("given_to_person_id")],
)
data class WhisperEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "edition_id") val editionId: String,
    @ColumnInfo(name = "recipient_id") val recipientId: String,
    val title: String,
    @ColumnInfo(name = "price_cents") val priceCents: Long?,
    val url: String,
    @ColumnInfo(name = "given_to_person_id") val givenToPersonId: String?,
    @ColumnInfo(name = "given_to_name") val givenToName: String,
    val received: Boolean,
    val notes: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "photo_path") val photoPath: String? = null,
)

fun WhisperEntity.asModel() = WhisperedIdea(
    id, editionId, recipientId, title, priceCents, url, givenToPersonId, givenToName, received, notes,
    Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt), photoPath,
)

fun WhisperedIdea.asEntity() = WhisperEntity(
    id, editionId, recipientId, title, priceCents, url, givenToPersonId, givenToName, received, notes,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli(), photoPath,
)
