package com.aktawind.monnoel.core.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aktawind.monnoel.core.model.AutoBackupSettings
import com.aktawind.monnoel.core.model.BackupFailureReason
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class BackupSettingsStore @Inject constructor(@ApplicationContext context: Context) {
    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("backup_settings") }

    val settings: Flow<AutoBackupSettings> = store.data.map { p ->
        AutoBackupSettings(
            enabled = p[ENABLED] ?: false,
            folderUri = p[FOLDER_URI],
            folderLabel = p[FOLDER_LABEL],
            lastSuccessAt = p[LAST_SUCCESS]?.let(Instant::ofEpochMilli),
            lastAttemptAt = p[LAST_ATTEMPT]?.let(Instant::ofEpochMilli),
            lastError = p[LAST_ERROR]?.let { runCatching { BackupFailureReason.valueOf(it) }.getOrNull() },
        )
    }

    suspend fun current(): AutoBackupSettings = settings.first()

    suspend fun setFolder(uri: String, label: String?) {
        store.edit {
            it[FOLDER_URI] = uri
            if (label != null) it[FOLDER_LABEL] = label else it.remove(FOLDER_LABEL)
            it[ENABLED] = true
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[ENABLED] = enabled }
    }

    suspend fun recordSuccess(at: Instant) {
        store.edit {
            it[LAST_SUCCESS] = at.toEpochMilli()
            it[LAST_ATTEMPT] = at.toEpochMilli()
            it.remove(LAST_ERROR)
        }
    }

    suspend fun recordFailure(at: Instant, reason: BackupFailureReason) {
        store.edit {
            it[LAST_ATTEMPT] = at.toEpochMilli()
            it[LAST_ERROR] = reason.name
        }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val FOLDER_URI = stringPreferencesKey("folder_uri")
        val FOLDER_LABEL = stringPreferencesKey("folder_label")
        val LAST_SUCCESS = longPreferencesKey("last_success")
        val LAST_ATTEMPT = longPreferencesKey("last_attempt")
        val LAST_ERROR = stringPreferencesKey("last_error")
    }
}
