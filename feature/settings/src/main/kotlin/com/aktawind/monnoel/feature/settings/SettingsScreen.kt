package com.aktawind.monnoel.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.model.AutoBackupSettings
import com.aktawind.monnoel.core.model.ThemePreference
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.ImportBackupDialog
import com.aktawind.monnoel.core.ui.formatDateTime
import com.aktawind.monnoel.core.ui.resolve
import com.aktawind.monnoel.core.ui.text
import com.aktawind.monnoel.core.ui.toUiMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(onOpenCategories: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { viewModel.export(it.toString()) }
        }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.inspect(it.toString()) }
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.setAutoBackupFolder(it.toString(), folderLabel(it)) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it.resolve(context))
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                PersonalizeSection(state.theme, viewModel::setTheme, onOpenCategories)
                BackupSection(
                    busy = state.busy,
                    onExport = { exportLauncher.launch(viewModel.suggestedExportName()) },
                    onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                )
                AutoBackupSection(
                    settings = state.autoBackup,
                    busy = state.busy,
                    onChooseFolder = { folderLauncher.launch(null) },
                    onToggle = viewModel::setAutoBackupEnabled,
                    onBackupNow = viewModel::backupNow,
                )
                AboutSection(state.versionName, state.versionCode)
            }
        }
    }

    state.pendingImport?.let { (_, summary) ->
        ImportBackupDialog(
            summary = summary,
            replacesExistingData = true,
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::cancelImport,
        )
    }
}

/** « primary:Download/MonNoel » -> « Download/MonNoel » */
private fun folderLabel(uri: Uri): String? =
    uri.lastPathSegment?.substringAfter(':')?.ifBlank { null } ?: uri.lastPathSegment

@Composable
internal fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun PersonalizeSection(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
    onOpenCategories: () -> Unit,
) {
    val labels = mapOf(
        ThemePreference.SYSTEM to R.string.settings_theme_system,
        ThemePreference.LIGHT to R.string.settings_theme_light,
        ThemePreference.DARK to R.string.settings_theme_dark,
    )
    SettingsCard(stringResource(R.string.settings_personalize)) {
        DropdownField(
            label = stringResource(R.string.settings_theme),
            selected = selected,
            options = ThemePreference.entries,
            optionLabel = { stringResource(labels.getValue(it)) },
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth(),
        )
        SettingsLink(
            title = stringResource(R.string.settings_categories),
            subtitle = stringResource(R.string.settings_categories_explain),
            onClick = onOpenCategories,
        )
    }
}

/** Ligne qui ouvre un sous-écran de réglages. */
@Composable
private fun SettingsLink(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalMonNoelColors.current.secondaryText)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun BackupSection(busy: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    SettingsCard(stringResource(R.string.settings_backup)) {
        Text(
            stringResource(R.string.settings_backup_explain),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalMonNoelColors.current.secondaryText,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onExport, enabled = !busy) { Text(stringResource(R.string.settings_export)) }
            OutlinedButton(onClick = onImport, enabled = !busy) { Text(stringResource(R.string.settings_import)) }
        }
    }
}

@Composable
private fun AutoBackupSection(
    settings: AutoBackupSettings?,
    busy: Boolean,
    onChooseFolder: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_auto)) {
        Text(
            stringResource(R.string.settings_auto_explain),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalMonNoelColors.current.secondaryText,
        )
        val hasFolder = settings?.folderUri != null
        if (hasFolder) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = settings!!.enabled, onValueChange = onToggle, role = Role.Switch)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings_auto_enabled), modifier = Modifier.weight(1f))
                Switch(checked = settings.enabled, onCheckedChange = null)
            }
            Text(stringResource(R.string.settings_auto_folder, settings!!.folderLabel ?: "—"))
            AutoBackupStatus(settings)
        } else {
            Text(stringResource(R.string.settings_auto_no_folder))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onChooseFolder, enabled = !busy) {
                Text(
                    stringResource(
                        if (hasFolder) R.string.settings_auto_change_folder else R.string.settings_auto_choose_folder,
                    ),
                )
            }
            if (hasFolder) {
                Button(onClick = onBackupNow, enabled = !busy) { Text(stringResource(R.string.settings_auto_now)) }
            }
        }
    }
}

@Composable
private fun AutoBackupStatus(settings: AutoBackupSettings) {
    val error = settings.lastError
    val lastSuccess = settings.lastSuccessAt
    when {
        error != null -> Text(
            stringResource(R.string.settings_auto_last_error, error.toUiMessage().text()),
            color = MaterialTheme.colorScheme.error,
        )
        lastSuccess != null -> Text(stringResource(R.string.settings_auto_last_success, lastSuccess.formatDateTime()))
        else -> Text(stringResource(R.string.settings_auto_never), color = LocalMonNoelColors.current.secondaryText)
    }
}

@Composable
private fun AboutSection(versionName: String, versionCode: Long) {
    SettingsCard(stringResource(R.string.settings_about)) {
        Text(stringResource(R.string.settings_about_text))
        Text(
            stringResource(R.string.settings_version, versionName, versionCode),
            style = MaterialTheme.typography.bodySmall,
            color = LocalMonNoelColors.current.secondaryText,
        )
    }
}
