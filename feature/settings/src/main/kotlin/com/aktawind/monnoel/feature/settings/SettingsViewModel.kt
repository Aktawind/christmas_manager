package com.aktawind.monnoel.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.AppInfo
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.repository.BackupRepository
import com.aktawind.monnoel.core.domain.repository.UserPreferencesRepository
import com.aktawind.monnoel.core.model.AutoBackupSettings
import com.aktawind.monnoel.core.model.BackupSummary
import com.aktawind.monnoel.core.model.ThemePreference
import com.aktawind.monnoel.core.ui.UiMessage
import com.aktawind.monnoel.core.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val autoBackup: AutoBackupSettings? = null,
    val busy: Boolean = false,
    val pendingImport: Pair<String, BackupSummary>? = null,
    val message: UiMessage? = null,
    val versionName: String = "",
    val versionCode: Long = 0,
)

private data class Transient(
    val busy: Boolean = false,
    val pendingImport: Pair<String, BackupSummary>? = null,
    val message: UiMessage? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val backups: BackupRepository,
    private val appInfo: AppInfo,
) : ViewModel() {
    private val transient = MutableStateFlow(Transient())

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.theme,
        backups.autoBackupSettings,
        transient,
    ) { theme, auto, t ->
        SettingsUiState(
            theme = theme,
            autoBackup = auto,
            busy = t.busy,
            pendingImport = t.pendingImport,
            message = t.message,
            versionName = appInfo.versionName,
            versionCode = appInfo.versionCode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun suggestedExportName(): String = backups.suggestedFileName()

    fun setTheme(theme: ThemePreference) = viewModelScope.launch { preferences.setTheme(theme) }

    fun export(uri: String) = run(success = UiMessage(R.string.settings_export_done)) { backups.exportTo(uri) }

    fun inspect(uri: String) = viewModelScope.launch {
        transient.update { it.copy(busy = true) }
        when (val result = backups.inspect(uri)) {
            is AppResult.Success -> transient.update { it.copy(busy = false, pendingImport = uri to result.value) }
            is AppResult.Failure -> transient.update { it.copy(busy = false, message = result.error.toUiMessage()) }
        }
    }

    fun confirmImport() {
        val uri = transient.value.pendingImport?.first ?: return
        transient.update { it.copy(pendingImport = null) }
        run(success = UiMessage(R.string.settings_import_done)) { backups.importFrom(uri) }
    }

    fun cancelImport() = transient.update { it.copy(pendingImport = null) }

    fun setAutoBackupFolder(uri: String, label: String?) = viewModelScope.launch {
        backups.setAutoBackupFolder(uri, label)
        run(success = UiMessage(R.string.settings_auto_folder_done)) { backups.runAutoBackupNow() }
    }

    fun setAutoBackupEnabled(enabled: Boolean) = viewModelScope.launch { backups.setAutoBackupEnabled(enabled) }

    fun backupNow() = run(success = UiMessage(R.string.settings_auto_now_done)) { backups.runAutoBackupNow() }

    fun messageShown() = transient.update { it.copy(message = null) }

    private fun run(success: UiMessage, action: suspend () -> AppResult<*>) {
        viewModelScope.launch {
            transient.update { it.copy(busy = true) }
            val result = action()
            transient.update {
                it.copy(
                    busy = false,
                    message = when (result) {
                        is AppResult.Success -> success
                        is AppResult.Failure -> result.error.toUiMessage()
                    },
                )
            }
        }
    }
}
