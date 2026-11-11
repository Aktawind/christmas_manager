package com.aktawind.monnoel.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aktawind.monnoel.core.domain.repository.UserPreferencesRepository
import com.aktawind.monnoel.core.model.ThemePreference
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreUserPreferences @Inject constructor(private val store: DataStore<Preferences>) :
    UserPreferencesRepository {
    override val theme: Flow<ThemePreference> = store.data.map { prefs ->
        prefs[THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.SYSTEM
    }

    override val selectedEditionId: Flow<String?> = store.data.map { it[SELECTED_EDITION] }

    override suspend fun setTheme(theme: ThemePreference) {
        store.edit { it[THEME] = theme.name }
    }

    override suspend fun setSelectedEditionId(id: String?) {
        store.edit { prefs ->
            if (id == null) prefs.remove(SELECTED_EDITION) else prefs[SELECTED_EDITION] = id
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val SELECTED_EDITION = stringPreferencesKey("selected_edition_id")
    }
}
