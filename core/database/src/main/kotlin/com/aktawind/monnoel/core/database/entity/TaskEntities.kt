package com.aktawind.monnoel.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.LinkType
import com.aktawind.monnoel.core.model.LutinTask
import com.aktawind.monnoel.core.model.TaskModule
import com.aktawind.monnoel.core.model.TaskStatus
import java.time.Instant

@Entity(
    tableName = "lutin_task",
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("edition_id")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "edition_id") val editionId: String,
    val title: String,
    val description: String,
    val status: String,
    val module: String,
    val notes: String,
    @ColumnInfo(name = "link_type") val linkType: String?,
    @ColumnInfo(name = "link_id") val linkId: String?,
    @ColumnInfo(name = "suggestion_key") val suggestionKey: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "dismissed_suggestion",
    primaryKeys = ["edition_id", "suggestion_key"],
    foreignKeys = [
        ForeignKey(entity = EditionEntity::class, parentColumns = ["id"], childColumns = ["edition_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class DismissedSuggestionEntity(
    @ColumnInfo(name = "edition_id") val editionId: String,
    @ColumnInfo(name = "suggestion_key") val suggestionKey: String,
)

fun TaskEntity.asModel() = LutinTask(
    id = id,
    editionId = editionId,
    title = title,
    description = description,
    status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.TODO),
    module = runCatching { TaskModule.valueOf(module) }.getOrDefault(TaskModule.OTHER),
    notes = notes,
    linkType = linkType?.let { runCatching { LinkType.valueOf(it) }.getOrNull() },
    linkId = linkId,
    suggestionKey = suggestionKey,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun LutinTask.asEntity() = TaskEntity(
    id, editionId, title, description, status.name, module.name, notes, linkType?.name, linkId, suggestionKey,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli(),
)
