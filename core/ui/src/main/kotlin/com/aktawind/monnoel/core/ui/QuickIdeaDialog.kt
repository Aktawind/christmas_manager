package com.aktawind.monnoel.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Noter une idée en quelques secondes : un titre suffit, le reste se complète plus tard. */
@Composable
fun QuickIdeaDialog(
    initialTitle: String = "",
    initialUrl: String = "",
    onSave: (title: String, url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_idea_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput(title, { title = it }, stringResource(R.string.quick_idea_name), Modifier.fillMaxWidth())
                TextInput(url, {
                    url = it
                }, stringResource(R.string.quick_idea_link), Modifier.fillMaxWidth(), keyboardType = KeyboardType.Uri)
                Text(stringResource(R.string.quick_idea_hint))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, url) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.quick_idea_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
