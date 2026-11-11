package com.aktawind.monnoel.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aktawind.monnoel.core.model.BackupSummary

/** Confirmation avant de remplacer les données par une sauvegarde. */
@Composable
fun ImportBackupDialog(
    summary: BackupSummary,
    replacesExistingData: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val editions = summary.counts["editions"] ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.import_dialog_date, summary.exportedAt.formatDateTime()))
                Text(pluralStringResource(R.plurals.import_dialog_editions, editions, editions))
                Text(
                    stringResource(R.string.import_dialog_version, summary.appVersion),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (replacesExistingData) {
                    Text(stringResource(R.string.import_dialog_warning), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.import_dialog_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
