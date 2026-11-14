package com.aktawind.monnoel.core.data.repository

import android.content.Context
import android.net.Uri
import com.aktawind.monnoel.core.common.IoDispatcher
import com.aktawind.monnoel.core.domain.gifts.LinkPreview
import com.aktawind.monnoel.core.domain.gifts.parseLinkPreview
import com.aktawind.monnoel.core.domain.repository.LinkPreviewRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Lit une page web comme le ferait un navigateur, uniquement quand l'utilisateur partage un
 * lien vers Mon Noël. Aucune autre donnée n'est envoyée. Les échecs renvoient null.
 */
class HttpLinkPreviewRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LinkPreviewRepository {
    override suspend fun fetch(url: String): LinkPreview? = withContext(io) {
        try {
            val connection = open(url)
            try {
                if (connection.responseCode !in 200..299) return@withContext null
                val type = connection.contentType.orEmpty()
                if (type.isNotEmpty() && !type.contains("html", ignoreCase = true)) return@withContext null
                val charset = Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE).find(type)?.groupValues?.get(1)
                val bytes = connection.inputStream.use { it.readLimited(MAX_HTML_BYTES) }
                val html = String(bytes, runCatching { charset(charset ?: "UTF-8") }.getOrDefault(Charsets.UTF_8))
                parseLinkPreview(html, connection.url.toString())
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    override suspend fun downloadImage(url: String): String? = withContext(io) {
        try {
            val connection = open(url)
            try {
                if (connection.responseCode !in 200..299) return@withContext null
                val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val file = File(dir, "image-${System.currentTimeMillis()}")
                file.outputStream().use { out ->
                    out.write(connection.inputStream.use { it.readLimited(MAX_IMAGE_BYTES) })
                }
                Uri.fromFile(file).toString()
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    private fun open(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9")
        return connection
    }

    private fun java.io.InputStream.readLimited(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (out.size() < max) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, minOf(read, max - out.size()))
        }
        return out.toByteArray()
    }

    private companion object {
        const val TIMEOUT_MS = 8_000
        const val MAX_HTML_BYTES = 2 * 1024 * 1024
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/126.0 Mobile Safari/537.36"
    }
}
