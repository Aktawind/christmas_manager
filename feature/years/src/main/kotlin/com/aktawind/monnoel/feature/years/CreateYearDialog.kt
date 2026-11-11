package com.aktawind.monnoel.feature.years

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.aktawind.monnoel.core.domain.edition.MAX_YEAR
import com.aktawind.monnoel.core.domain.edition.MIN_YEAR

@Composable
internal fun CreateYearDialog(suggestedYear: Int, onCreate: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(suggestedYear.toString()) }
    val year = text.toIntOrNull()
    val valid = year != null && year in MIN_YEAR..MAX_YEAR
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.years_new_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.years_new_label)) },
                singleLine = true,
                isError = !valid,
                supportingText = if (!valid) {
                    { Text(stringResource(R.string.years_new_invalid, MIN_YEAR, MAX_YEAR)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                year?.let(onCreate)
            }, enabled = valid) { Text(stringResource(R.string.years_new_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.years_cancel)) } },
    )
}
