package com.aktawind.monnoel.core.domain.repository

import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.model.AutoBackupSettings
import com.aktawind.monnoel.core.model.BackupSummary
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    /** Nom de fichier proposé pour un export manuel. */
    fun suggestedFileName(): String

    /** Exporte toutes les données vers l'emplacement choisi (URI SAF). */
    suspend fun exportTo(destinationUri: String): AppResult<BackupSummary>

    /** Lit et vérifie une sauvegarde sans rien modifier. */
    suspend fun inspect(sourceUri: String): AppResult<BackupSummary>

    /**
     * Remplace toutes les données par celles de la sauvegarde. Une sauvegarde de sécurité
     * des données actuelles est faite avant ; en cas d'échec, rien n'est modifié.
     */
    suspend fun importFrom(sourceUri: String): AppResult<BackupSummary>

    val autoBackupSettings: Flow<AutoBackupSettings>

    suspend fun setAutoBackupFolder(folderUri: String, label: String?)

    suspend fun setAutoBackupEnabled(enabled: Boolean)

    /** Lance immédiatement une sauvegarde automatique dans le dossier choisi. */
    suspend fun runAutoBackupNow(): AppResult<Unit>
}
