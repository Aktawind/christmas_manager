package com.aktawind.monnoel.core.backup.format

import com.aktawind.monnoel.core.model.BackupFailureReason
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class BackupException(val reason: BackupFailureReason, cause: Throwable? = null) : Exception(reason.name, cause)

data class DecodedBackup(val manifest: BackupManifest, val data: BackupData)

/** Lecture / écriture de l'archive `.zip` de sauvegarde. Code pur, testable sans Android. */
object BackupArchive {
    private const val MAX_JSON_BYTES = 64L * 1024 * 1024

    fun write(
        out: OutputStream,
        data: BackupData,
        mediaDir: File?,
        appVersion: String,
        appVersionCode: Long,
        exportedAt: Instant,
    ): BackupManifest {
        val dataBytes = backupJson.encodeToString(BackupData.serializer(), data).toByteArray()
        val mediaFiles = mediaDir?.takeIf { it.isDirectory }?.walkTopDown()?.filter { it.isFile }?.toList().orEmpty()
        val manifest = BackupManifest(
            formatVersion = BACKUP_FORMAT_VERSION,
            appVersion = appVersion,
            appVersionCode = appVersionCode,
            exportedAt = exportedAt.toString(),
            counts = data.counts(),
            mediaFiles = mediaFiles.size,
            dataSha256 = sha256(dataBytes),
        )
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(backupJson.encodeToString(BackupManifest.serializer(), manifest).toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(dataBytes)
            zip.closeEntry()
            mediaFiles.forEach { file ->
                val relative = file.relativeTo(mediaDir!!).invariantSeparatorsPath
                zip.putNextEntry(ZipEntry(MEDIA_PREFIX + relative))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return manifest
    }

    /**
     * Lit et vérifie une sauvegarde. Les photos sont extraites dans [mediaTargetDir] si fourni
     * (sinon ignorées). Lève [BackupException] si le fichier est invalide.
     */
    fun read(input: InputStream, mediaTargetDir: File?): DecodedBackup {
        var manifestBytes: ByteArray? = null
        var dataBytes: ByteArray? = null
        try {
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when {
                        entry.isDirectory -> Unit
                        entry.name == MANIFEST_ENTRY -> manifestBytes = zip.readLimited()
                        entry.name == DATA_ENTRY -> dataBytes = zip.readLimited()
                        entry.name.startsWith(MEDIA_PREFIX) && mediaTargetDir != null ->
                            extractMedia(zip, entry.name.removePrefix(MEDIA_PREFIX), mediaTargetDir)
                    }
                    zip.closeEntry()
                }
            }
        } catch (e: IOException) {
            throw BackupException(BackupFailureReason.READ_FAILED, e)
        }
        val manifest = manifestBytes?.let { decodeManifest(it) }
            ?: throw BackupException(BackupFailureReason.NOT_A_BACKUP)
        if (manifest.app != BackupManifest.APP_ID) throw BackupException(BackupFailureReason.NOT_A_BACKUP)
        if (manifest.formatVersion > BACKUP_FORMAT_VERSION) throw BackupException(BackupFailureReason.TOO_RECENT)
        val bytes = dataBytes ?: throw BackupException(BackupFailureReason.CORRUPTED)
        if (sha256(bytes) != manifest.dataSha256) throw BackupException(BackupFailureReason.CORRUPTED)
        val data = try {
            val raw: JsonObject = backupJson.parseToJsonElement(bytes.decodeToString()).jsonObject
            val upgraded = BackupMigrations.upgrade(raw, manifest.formatVersion)
            backupJson.decodeFromJsonElement(BackupData.serializer(), upgraded)
        } catch (e: SerializationException) {
            throw BackupException(BackupFailureReason.CORRUPTED, e)
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupFailureReason.CORRUPTED, e)
        } catch (e: IllegalStateException) {
            throw BackupException(BackupFailureReason.CORRUPTED, e)
        }
        return DecodedBackup(manifest, data)
    }

    private fun decodeManifest(bytes: ByteArray): BackupManifest? = try {
        backupJson.decodeFromString(BackupManifest.serializer(), bytes.decodeToString())
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun extractMedia(zip: ZipInputStream, relativePath: String, targetDir: File) {
        val target = File(targetDir, relativePath).canonicalFile
        // Protection contre les chemins malveillants (« zip slip »).
        if (!target.path.startsWith(targetDir.canonicalPath + File.separator)) {
            throw BackupException(BackupFailureReason.CORRUPTED)
        }
        target.parentFile?.mkdirs()
        target.outputStream().use { zip.copyTo(it) }
    }

    private fun ZipInputStream.readLimited(): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(chunk)
            if (read < 0) break
            total += read
            if (total > MAX_JSON_BYTES) throw BackupException(BackupFailureReason.CORRUPTED)
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
