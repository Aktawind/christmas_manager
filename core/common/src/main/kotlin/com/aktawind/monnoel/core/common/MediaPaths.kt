package com.aktawind.monnoel.core.common

import java.io.File

/** Emplacement des photos : `<filesDir>/media/<chemin relatif>`. */
object MediaPaths {
    const val MEDIA_DIR = "media"
    const val PHOTOS_DIR = "photos"

    fun mediaRoot(filesDir: File): File = File(filesDir, MEDIA_DIR)

    fun resolve(filesDir: File, relativePath: String): File = File(mediaRoot(filesDir), relativePath)
}
