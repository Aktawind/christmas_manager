package com.aktawind.monnoel.core.model

import java.time.Instant

/** Réglages et état de la sauvegarde automatique. */
data class AutoBackupSettings(
    val enabled: Boolean,
    /** URI (Storage Access Framework) du dossier choisi, ou null si aucun dossier. */
    val folderUri: String?,
    val folderLabel: String?,
    val lastSuccessAt: Instant?,
    val lastAttemptAt: Instant?,
    val lastError: BackupFailureReason?,
)

/** Informations lues dans une sauvegarde avant de l'importer. */
data class BackupSummary(
    val formatVersion: Int,
    val appVersion: String,
    val exportedAt: Instant,
    val counts: Map<String, Int>,
)

enum class BackupFailureReason {
    FOLDER_UNAVAILABLE,
    WRITE_FAILED,
    READ_FAILED,
    NOT_A_BACKUP,
    CORRUPTED,
    TOO_RECENT,
    UNKNOWN,
}
