package com.aktawind.monnoel.core.backup

import android.content.Context
import java.io.File

/** Emplacements de fichiers de l'application. */
object AppDirectories {
    /** Photos de l'application (redimensionnées), incluses dans les sauvegardes. */
    fun media(context: Context): File = File(context.filesDir, "media")

    /** Sauvegardes de sécurité faites automatiquement avant chaque import. */
    fun safetyBackups(context: Context): File = File(context.filesDir, "safety-backups")
}
