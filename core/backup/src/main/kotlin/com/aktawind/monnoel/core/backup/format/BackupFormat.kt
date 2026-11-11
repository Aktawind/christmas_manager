package com.aktawind.monnoel.core.backup.format

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Version du format de sauvegarde (ADR 0004). À incrémenter à chaque changement de
 * structure de `data.json`, en ajoutant la conversion dans [BackupMigrations].
 */
const val BACKUP_FORMAT_VERSION = 1

const val MANIFEST_ENTRY = "manifest.json"
const val DATA_ENTRY = "data.json"
const val MEDIA_PREFIX = "media/"

@Serializable
data class BackupManifest(
    val app: String = APP_ID,
    val formatVersion: Int,
    val appVersion: String,
    val appVersionCode: Long,
    val exportedAt: String,
    val counts: Map<String, Int>,
    val mediaFiles: Int,
    val dataSha256: String,
) {
    companion object {
        const val APP_ID = "mon-noel"
    }
}

/** Contenu de `data.json` : une liste par table. Les DTO sont indépendants des entités Room. */
@Serializable
data class BackupData(val editions: List<EditionDto> = emptyList()) {
    fun counts(): Map<String, Int> = mapOf(
        "editions" to editions.size,
    )
}

@Serializable
data class EditionDto(
    val id: String,
    val year: Int,
    val status: String,
    val plannedBudgetCents: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

internal val backupJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}
