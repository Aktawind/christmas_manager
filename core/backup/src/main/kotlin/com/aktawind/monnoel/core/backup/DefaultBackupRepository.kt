package com.aktawind.monnoel.core.backup

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.aktawind.monnoel.core.backup.data.BackupDataSource
import com.aktawind.monnoel.core.backup.format.BackupArchive
import com.aktawind.monnoel.core.backup.format.BackupException
import com.aktawind.monnoel.core.backup.format.BackupManifest
import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppInfo
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IoDispatcher
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.repository.BackupRepository
import com.aktawind.monnoel.core.model.AutoBackupSettings
import com.aktawind.monnoel.core.model.BackupFailureReason
import com.aktawind.monnoel.core.model.BackupSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class DefaultBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: BackupDataSource,
    private val settingsStore: BackupSettingsStore,
    private val scheduler: AutoBackupScheduler,
    private val appInfo: AppInfo,
    private val time: TimeProvider,
    @IoDispatcher private val io: CoroutineDispatcher,
) : BackupRepository {
    /** Une seule opération de sauvegarde ou de restauration à la fois. */
    private val mutex = Mutex()

    override val autoBackupSettings: Flow<AutoBackupSettings> = settingsStore.settings

    override fun suggestedFileName(): String = BackupFileNames.manual(time.now())

    override suspend fun exportTo(destinationUri: String): AppResult<BackupSummary> = guarded {
        val stream = context.contentResolver.openOutputStream(destinationUri.toUri(), "wt")
            ?: throw BackupException(BackupFailureReason.WRITE_FAILED)
        writeBackup(stream).toSummary()
    }

    override suspend fun inspect(sourceUri: String): AppResult<BackupSummary> = guarded {
        val input = context.contentResolver.openInputStream(sourceUri.toUri())
            ?: throw BackupException(BackupFailureReason.READ_FAILED)
        input.use { BackupArchive.read(it, mediaTargetDir = null) }.manifest.toSummary()
    }

    override suspend fun importFrom(sourceUri: String): AppResult<BackupSummary> = guarded {
        val staging = File(context.cacheDir, "import-media").apply { deleteRecursively() }
        try {
            // 1. Lecture et vérification complètes avant toute modification.
            val input = context.contentResolver.openInputStream(sourceUri.toUri())
                ?: throw BackupException(BackupFailureReason.READ_FAILED)
            staging.mkdirs()
            val decoded = input.use { BackupArchive.read(it, mediaTargetDir = staging) }

            // 2. Sauvegarde de sécurité des données actuelles.
            writeSafetyBackup()

            // 3. Remplacement des données (transaction : tout ou rien).
            dataSource.replaceAll(decoded.data)

            // 4. Remplacement des photos.
            val media = AppDirectories.media(context)
            media.deleteRecursively()
            if (!staging.renameTo(media)) {
                staging.copyRecursively(media, overwrite = true)
            }
            decoded.manifest.toSummary()
        } finally {
            staging.deleteRecursively()
        }
    }

    override suspend fun setAutoBackupFolder(folderUri: String, label: String?) {
        val uri = folderUri.toUri()
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        settingsStore.setFolder(folderUri, label)
        scheduler.schedule()
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        settingsStore.setEnabled(enabled)
        if (enabled) scheduler.schedule() else scheduler.cancel()
    }

    override suspend fun runAutoBackupNow(): AppResult<Unit> {
        val result = guarded {
            val settings = settingsStore.current()
            val folderUri = settings.folderUri ?: throw BackupException(BackupFailureReason.FOLDER_UNAVAILABLE)
            val folder = DocumentFile.fromTreeUri(context, folderUri.toUri())
            if (folder == null || !folder.canWrite()) throw BackupException(BackupFailureReason.FOLDER_UNAVAILABLE)
            val file = folder.createFile("application/zip", BackupFileNames.auto(time.now()).removeSuffix(".zip"))
                ?: throw BackupException(BackupFailureReason.WRITE_FAILED)
            val stream = context.contentResolver.openOutputStream(file.uri, "wt")
                ?: throw BackupException(BackupFailureReason.WRITE_FAILED)
            writeBackup(stream)
            rotate(folder)
        }
        when (result) {
            is AppResult.Success -> settingsStore.recordSuccess(time.now())
            is AppResult.Failure -> settingsStore.recordFailure(
                time.now(),
                (result.error as? AppError.Backup)?.reason ?: BackupFailureReason.UNKNOWN,
            )
        }
        return result
    }

    /** Planifie la sauvegarde automatique au démarrage si elle est activée. */
    suspend fun ensureScheduled() {
        val settings = settingsStore.current()
        if (settings.enabled && settings.folderUri != null) scheduler.schedule()
    }

    private suspend fun writeBackup(stream: OutputStream): BackupManifest {
        val data = dataSource.snapshot()
        return stream.use {
            BackupArchive.write(
                out = it,
                data = data,
                mediaDir = AppDirectories.media(context),
                appVersion = appInfo.versionName,
                appVersionCode = appInfo.versionCode,
                exportedAt = time.now(),
            )
        }
    }

    private suspend fun writeSafetyBackup() {
        val dir = AppDirectories.safetyBackups(context).apply { mkdirs() }
        val file = File(dir, BackupFileNames.safety(time.now()))
        try {
            writeBackup(file.outputStream())
        } catch (e: IOException) {
            file.delete()
            throw BackupException(BackupFailureReason.WRITE_FAILED, e)
        }
        dir.listFiles { f -> f.name.startsWith(BackupFileNames.SAFETY_PREFIX) }
            ?.sortedByDescending { it.name }
            ?.drop(KEEP_SAFETY)
            ?.forEach { it.delete() }
    }

    private fun rotate(folder: DocumentFile) {
        folder.listFiles()
            .filter { it.name?.startsWith(BackupFileNames.AUTO_PREFIX) == true }
            .sortedByDescending { it.name }
            .drop(KEEP_AUTO)
            .forEach { it.delete() }
    }

    private suspend fun <T> guarded(block: suspend () -> T): AppResult<T> = withContext(io) {
        mutex.withLock {
            try {
                AppResult.Success(block())
            } catch (e: BackupException) {
                AppResult.Failure(AppError.Backup(e.reason))
            } catch (e: SecurityException) {
                AppResult.Failure(AppError.Backup(BackupFailureReason.FOLDER_UNAVAILABLE))
            } catch (e: IOException) {
                AppResult.Failure(AppError.Backup(BackupFailureReason.READ_FAILED))
            } catch (e: IllegalArgumentException) {
                AppResult.Failure(AppError.Backup(BackupFailureReason.UNKNOWN))
            } catch (e: android.database.SQLException) {
                AppResult.Failure(AppError.Backup(BackupFailureReason.CORRUPTED))
            }
        }
    }

    private fun BackupManifest.toSummary() = BackupSummary(
        formatVersion = formatVersion,
        appVersion = appVersion,
        exportedAt = runCatching { Instant.parse(exportedAt) }.getOrDefault(Instant.EPOCH),
        counts = counts,
    )

    companion object {
        const val KEEP_AUTO = 10
        const val KEEP_SAFETY = 5
    }
}
