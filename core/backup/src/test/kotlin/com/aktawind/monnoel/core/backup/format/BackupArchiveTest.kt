package com.aktawind.monnoel.core.backup.format

import com.aktawind.monnoel.core.model.BackupFailureReason
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BackupArchiveTest {
    private val sample = BackupData(
        editions = listOf(
            EditionDto("a", 2025, "ARCHIVED", 50_000, 1L, 2L),
            EditionDto("b", 2026, "ACTIVE", null, 3L, 4L),
        ),
    )

    private fun export(mediaDir: java.io.File? = null): ByteArray {
        val out = ByteArrayOutputStream()
        BackupArchive.write(out, sample, mediaDir, "0.1.0", 12, Instant.parse("2026-11-11T10:00:00Z"))
        return out.toByteArray()
    }

    @Test
    fun `aller-retour complet avec photos`() {
        val media = Files.createTempDirectory("media").toFile()
        java.io.File(media, "gifts/photo1.jpg").apply { parentFile!!.mkdirs() }.writeBytes(byteArrayOf(1, 2, 3))
        val bytes = export(media)

        val target = Files.createTempDirectory("restore").toFile()
        val decoded = BackupArchive.read(ByteArrayInputStream(bytes), target)

        assertEquals(sample, decoded.data)
        assertEquals(BACKUP_FORMAT_VERSION, decoded.manifest.formatVersion)
        assertEquals(mapOf("editions" to 2), decoded.manifest.counts)
        assertEquals(1, decoded.manifest.mediaFiles)
        assertEquals(listOf<Byte>(1, 2, 3), java.io.File(target, "gifts/photo1.jpg").readBytes().toList())
    }

    @Test
    fun `refuse un fichier qui n'est pas une sauvegarde`() {
        val e = assertFailsWith<BackupException> {
            BackupArchive.read(ByteArrayInputStream("bonjour".toByteArray()), null)
        }
        assertEquals(BackupFailureReason.NOT_A_BACKUP, e.reason)
    }

    @Test
    fun `detecte une sauvegarde modifiee`() {
        val original = BackupArchive.read(ByteArrayInputStream(export()), null)
        val tampered = zipOf(
            MANIFEST_ENTRY to backupJson.encodeToString(BackupManifest.serializer(), original.manifest),
            DATA_ENTRY to """{"editions":[]}""",
        )
        val e = assertFailsWith<BackupException> { BackupArchive.read(ByteArrayInputStream(tampered), null) }
        assertEquals(BackupFailureReason.CORRUPTED, e.reason)
    }

    @Test
    fun `refuse une sauvegarde d'une version plus recente`() {
        val original = BackupArchive.read(ByteArrayInputStream(export()), null)
        val future = original.manifest.copy(formatVersion = BACKUP_FORMAT_VERSION + 1)
        val bytes = zipOf(MANIFEST_ENTRY to backupJson.encodeToString(BackupManifest.serializer(), future))
        val e = assertFailsWith<BackupException> { BackupArchive.read(ByteArrayInputStream(bytes), null) }
        assertEquals(BackupFailureReason.TOO_RECENT, e.reason)
    }

    @Test
    fun `bloque les chemins de photos malveillants`() {
        val original = BackupArchive.read(ByteArrayInputStream(export()), null)
        val bytes = zipOf(
            "media/../../evil.txt" to "x",
            MANIFEST_ENTRY to backupJson.encodeToString(BackupManifest.serializer(), original.manifest),
        )
        val target = Files.createTempDirectory("restore").toFile()
        val e = assertFailsWith<BackupException> { BackupArchive.read(ByteArrayInputStream(bytes), target) }
        assertEquals(BackupFailureReason.CORRUPTED, e.reason)
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
