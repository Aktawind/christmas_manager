package com.aktawind.monnoel.core.backup

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object BackupFileNames {
    const val AUTO_PREFIX = "mon-noel-auto-"
    const val SAFETY_PREFIX = "mon-noel-securite-"
    private const val MANUAL_PREFIX = "mon-noel-"
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")

    private fun stamp(at: Instant): String = formatter.format(at.atZone(ZoneId.systemDefault()))

    fun manual(at: Instant) = "$MANUAL_PREFIX${stamp(at)}.zip"

    fun auto(at: Instant) = "$AUTO_PREFIX${stamp(at)}.zip"

    fun safety(at: Instant) = "$SAFETY_PREFIX${stamp(at)}.zip"
}
