package com.aktawind.monnoel.core.backup.format

import kotlinx.serialization.json.JsonObject

/**
 * Conversion des anciennes versions de `data.json` vers la version courante.
 * Exemple futur : `2 -> { json -> ajouter le champ … }`.
 */
internal object BackupMigrations {
    private val steps: Map<Int, (JsonObject) -> JsonObject> = emptyMap()

    fun upgrade(data: JsonObject, fromVersion: Int): JsonObject {
        var current = data
        for (version in fromVersion until BACKUP_FORMAT_VERSION) {
            val step = steps[version + 1] ?: error("Conversion manquante vers la version ${version + 1}")
            current = step(current)
        }
        return current
    }
}
