package com.aktawind.monnoel.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** « samedi 6 décembre ». */
val longDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

/** « sam. 6 déc. », pour les cartes. */
val shortDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

/**
 * Choix d'une date facultative : un bouton qui ouvre le calendrier, et « Retirer la date ».
 * [defaultDate] est proposée à l'ouverture quand aucune date n'est choisie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    defaultDate: LocalDate,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { picking = true }, enabled = enabled) {
            Text(date?.let { "📅 " + it.format(longDateFormat) } ?: stringResource(R.string.date_choose))
        }
        if (date != null && enabled) {
            TextButton(onClick = { onDateChange(null) }) { Text(stringResource(R.string.date_clear)) }
        }
    }
    if (picking) {
        val start = date ?: defaultDate
        val state = rememberDatePickerState(initialSelectedDateMillis = start.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    picking = false
                }) { Text(stringResource(R.string.date_ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state) }
    }
}
