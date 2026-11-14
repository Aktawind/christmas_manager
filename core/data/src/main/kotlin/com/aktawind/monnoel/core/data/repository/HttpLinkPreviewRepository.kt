package com.aktawind.monnoel.core.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aktawind.monnoel.core.common.IoDispatcher
import com.aktawind.monnoel.core.domain.gifts.PageFetch
import com.aktawind.monnoel.core.domain.gifts.parseLinkPreview
import com.aktawind.monnoel.core.domain.repository.LinkPreviewRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Lit une page web comme le ferait un navigateur, uniquement quand l'utilisateur partage un
 * lien vers Mon Noël (ADR 0008). Aucune autre donnée n'est envoyée.
 */
class HttpLinkPreviewRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LinkPreviewRepository {
    override suspend fun fetch(url: String): PageFetch = withContext(io) {
        // Certains sites refusent les navigateurs mobiles inconnus : on réessaie en « ordinateur ».
        var last: PageFetch = PageFetch.Unreachable
        for (agent in listOf(MOBILE_AGENT, DESKTOP_AGENT)) {
            last = fetchOnce(url, agent)
            if (last is PageFetch.Success) return@withContext last
            if (last is PageFetch.Refused && last.httpCode !in RETRY_CODES) return@withContext last
        }
        last
    }

    private fun fetchOnce(url: String, agent: String): PageFetch = try {
        val connection = open(url, agent, accept = ACCEPT_HTML, referer = null)
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                Log.i(TAG, "Page refusée ($code) : $url")
                PageFetch.Refused(code)
            } else {
                val type = connection.contentType.orEmpty()
                val charsetName = Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE).find(type)?.groupValues?.get(1)
                val bytes = connection.inputStream.use { it.readLimited(MAX_HTML_BYTES) }
                val charset = runCatching { charset(charsetName ?: "UTF-8") }.getOrDefault(Charsets.UTF_8)
                PageFetch.Success(parseLinkPreview(String(bytes, charset), connection.url.toString()))
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: IOException) {
        Log.i(TAG, "Page injoignable : $url", e)
        PageFetch.Unreachable
    } catch (e: IllegalArgumentException) {
        PageFetch.Unreachable
    } catch (e: SecurityException) {
        PageFetch.Unreachable
    }

    override suspend fun downloadImage(url: String, referer: String): String? = withContext(io) {
        for (agent in listOf(MOBILE_AGENT, DESKTOP_AGENT)) {
            try {
                val connection = open(url, agent, accept = ACCEPT_IMAGE, referer = referer)
                try {
                    val code = connection.responseCode
                    if (code !in 200..299) {
                        Log.i(TAG, "Image refusée ($code) : $url")
                        continue
                    }
                    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                    dir.listFiles()?.forEach { it.delete() }
                    val file = File(dir, "image-${System.currentTimeMillis()}")
                    file.writeBytes(connection.inputStream.use { it.readLimited(MAX_IMAGE_BYTES) })
                    return@withContext Uri.fromFile(file).toString()
                } finally {
                    connection.disconnect()
                }
            } catch (e: IOException) {
                Log.i(TAG, "Image injoignable : $url", e)
            } catch (e: IllegalArgumentException) {
                return@withContext null
            } catch (e: SecurityException) {
                return@withContext null
            }
        }
        null
    }

    private fun open(url: String, agent: String, accept: String, referer: String?): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", agent)
        connection.setRequestProperty("Accept", accept)
        connection.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.5")
        if (referer != null) connection.setRequestProperty("Referer", referer)
        return connection
    }

    private fun InputStream.readLimited(max: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (out.size() < max) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, minOf(read, max - out.size()))
        }
        return out.toByteArray()
    }

    private companion object {
        const val TAG = "MonNoelPreview"
        const val TIMEOUT_MS = 8_000
        const val MAX_HTML_BYTES = 3 * 1024 * 1024
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
        val RETRY_CODES = setOf(401, 403, 406, 429, 503)
        const val ACCEPT_HTML = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
        const val MOBILE_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-X710) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.36"
        const val DESKTOP_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.36"
    }
}
