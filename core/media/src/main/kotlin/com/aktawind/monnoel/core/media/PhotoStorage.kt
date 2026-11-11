package com.aktawind.monnoel.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.IoDispatcher
import com.aktawind.monnoel.core.common.MediaPaths
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Photos de l'application : importées, tournées selon l'EXIF, réduites à [MAX_SIDE] px
 * et enregistrées en JPEG dans `files/media/photos/` (inclus dans les sauvegardes).
 */
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ids: IdGenerator,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PhotoRepository {
    private val photosDir: File
        get() = File(MediaPaths.mediaRoot(context.filesDir), MediaPaths.PHOTOS_DIR).apply { mkdirs() }

    override suspend fun importPhoto(sourceUri: String): AppResult<String> = withContext(io) {
        try {
            val uri = sourceUri.toUri()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 ||
                bounds.outHeight <= 0
            ) {
                return@withContext AppResult.Failure(AppError.InvalidInput)
            }

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@withContext AppResult.Failure(AppError.InvalidInput)

            val rotation = context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).rotationDegrees
            } ?: 0
            val scaled = scaleAndRotate(decoded, rotation)

            val name = "${ids.newId()}.jpg"
            File(photosDir, name).outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()
            AppResult.Success("${MediaPaths.PHOTOS_DIR}/$name")
        } catch (e: IOException) {
            AppResult.Failure(AppError.Unexpected(e))
        } catch (e: SecurityException) {
            AppResult.Failure(AppError.Unexpected(e))
        } catch (e: OutOfMemoryError) {
            AppResult.Failure(AppError.Unexpected(e))
        }
    }

    override suspend fun copyPhoto(relativePath: String): String? = withContext(io) {
        val source = MediaPaths.resolve(context.filesDir, relativePath)
        if (!source.exists()) return@withContext null
        val name = "${ids.newId()}.jpg"
        runCatching { source.copyTo(File(photosDir, name)) }.getOrNull()?.let { "${MediaPaths.PHOTOS_DIR}/$name" }
    }

    override suspend fun deletePhoto(relativePath: String) {
        withContext(io) {
            val file = MediaPaths.resolve(context.filesDir, relativePath).canonicalFile
            // Ne supprime jamais rien en dehors du dossier des médias.
            if (file.path.startsWith(
                    MediaPaths.mediaRoot(context.filesDir).canonicalPath + File.separator,
                )
            ) {
                file.delete()
            }
        }
    }

    private fun scaleAndRotate(bitmap: Bitmap, rotation: Int): Bitmap {
        val ratio = MAX_SIDE.toFloat() / max(bitmap.width, bitmap.height)
        val matrix = Matrix()
        if (ratio < 1f) matrix.postScale(ratio, ratio)
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val MAX_SIDE = 1600
        const val QUALITY = 85
    }
}
