package com.aktawind.monnoel.core.backup.format

import com.aktawind.monnoel.core.database.DefaultData
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Conversion des anciennes versions de `data.json` vers la version courante.
 * Exemple futur : `2 -> { json -> ajouter le champ … }`.
 */
internal object BackupMigrations {
    private val steps: Map<Int, (JsonObject) -> JsonObject> = mapOf(
        // v1 (v0.1.0) -> v2 (v0.2.0) : apparition des catégories de cadeaux, créées par défaut.
        2 to { json ->
            val categories = DefaultData.giftCategories.mapIndexed { index, (id, name) ->
                JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(id),
                        "name" to JsonPrimitive(name),
                        "sortOrder" to JsonPrimitive(index),
                    ),
                )
            }
            JsonObject(json + ("giftCategories" to JsonArray(categories)))
        },
    )

    fun upgrade(data: JsonObject, fromVersion: Int): JsonObject {
        var current = data
        for (version in fromVersion until BACKUP_FORMAT_VERSION) {
            val step = steps[version + 1] ?: error("Conversion manquante vers la version ${version + 1}")
            current = step(current)
        }
        return current
    }
}
