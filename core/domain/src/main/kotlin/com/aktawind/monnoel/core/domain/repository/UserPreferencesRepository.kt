package com.aktawind.monnoel.core.domain.repository

import com.aktawind.monnoel.core.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val theme: Flow<ThemePreference>

    /** Identifiant de l'année affichée dans l'application. */
    val selectedEditionId: Flow<String?>

    suspend fun setTheme(theme: ThemePreference)

    suspend fun setSelectedEditionId(id: String?)
}
