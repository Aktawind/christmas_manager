package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.IdeaRequest
import com.aktawind.monnoel.core.model.IdeaRequestKind
import java.time.Instant

@Entity(
    tableName = "idea_request",
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["person_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["other_person_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("edition_id"), Index("person_id"), Index("other_person_id")],
)
data class IdeaRequestEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "edition_id") val editionId: String,
    val kind: String,
    @ColumnInfo(name = "person_id") val personId: String,
    @ColumnInfo(name = "other_person_id") val otherPersonId: String?,
    @ColumnInfo(name = "other_name") val otherName: String,
    @ColumnInfo(name = "task_id") val taskId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

fun IdeaRequestEntity.asModel() = IdeaRequest(
    id,
    editionId,
    runCatching { IdeaRequestKind.valueOf(kind) }.getOrDefault(IdeaRequestKind.TO_SEND),
    personId,
    otherPersonId,
    otherName,
    taskId,
    Instant.ofEpochMilli(createdAt),
)

fun IdeaRequest.asEntity() = IdeaRequestEntity(
    id,
    editionId,
    kind.name,
    personId,
    otherPersonId,
    otherName,
    taskId,
    createdAt.toEpochMilli(),
)
