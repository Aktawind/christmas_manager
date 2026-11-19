package com.aktawind.monnoel.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.countdown.DeleteSpecialDateUseCase
import com.aktawind.monnoel.core.domain.countdown.SaveSpecialDateUseCase
import com.aktawind.monnoel.core.domain.countdown.SpecialDateRepository
import com.aktawind.monnoel.core.domain.countdown.specialDateMonths
import com.aktawind.monnoel.core.model.SpecialDate
import com.aktawind.monnoel.core.ui.DropdownField
import com.aktawind.monnoel.core.ui.TextInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ordre de la saison : novembre, décembre, puis janvier. */
private fun SpecialDate.seasonOrder() = (if (month == 1) 13 else month) * 100 + day

@HiltViewModel
class PathDatesViewModel @Inject constructor(
    dates: SpecialDateRepository,
    private val saveDate: SaveSpecialDateUseCase,
    private val deleteDate: DeleteSpecialDateUseCase,
) : ViewModel() {
    val uiState: StateFlow<List<SpecialDate>> = dates.observeDates().map { l -> l.sortedBy { it.seasonOrder() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(month: Int, day: Int, label: String, emoji: String) {
        viewModelScope.launch { saveDate(null, month, day, label, emoji) }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteDate(id) }
    }
}

private val pathEmojis = listOf("⭐", "🎂", "🕯️", "🚗", "🎁", "❄️", "🎶", "💌", "👑", "🧑‍🎄", "🎄", "🥂")

/** Sous-écran des réglages : les dates qui comptent, ajoutées au chemin vers Noël chaque année. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathDatesRoute(onBack: () -> Unit, viewModel: PathDatesViewModel = hiltViewModel()) {
    val dates by viewModel.uiState.collectAsStateWithLifecycle()
    val monthNames = stringArrayResource(R.array.path_months)
    var month by rememberSaveable { mutableStateOf(12) }
    var day by rememberSaveable { mutableStateOf(1) }
    var label by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf(pathEmojis.first()) }
    // Année quelconque non bissextile pour la longueur du mois (novembre 30, décembre et janvier 31).
    val days = (1..YearMonth.of(2025, month).lengthOfMonth()).toList()
    if (day > days.last()) day = days.last()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_path)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(
                padding,
            ).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.settings_path_explain),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalMonNoelColors.current.secondaryText,
                )
                SettingsCard(stringResource(R.string.settings_path_mine)) {
                    if (dates.isEmpty()) Text(stringResource(R.string.settings_path_none), color = LocalMonNoelColors.current.secondaryText)
                    dates.forEach { d ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${d.emoji}  ${d.day} ${monthNames[specialDateMonths.indexOf(d.month)]} · ${d.label}",
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { viewModel.delete(d.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.settings_path_delete, d.label))
                            }
                        }
                    }
                }
                SettingsCard(stringResource(R.string.settings_path_add)) {
                    TextInput(label, { label = it }, stringResource(R.string.settings_path_label), Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DropdownField(
                            label = stringResource(R.string.settings_path_day),
                            selected = day,
                            options = days,
                            optionLabel = { it.toString() },
                            onSelect = { day = it },
                            modifier = Modifier.weight(1f),
                        )
                        DropdownField(
                            label = stringResource(R.string.settings_path_month),
                            selected = month,
                            options = specialDateMonths,
                            optionLabel = { monthNames[specialDateMonths.indexOf(it)] },
                            onSelect = { month = it },
                            modifier = Modifier.weight(1f),
                        )
                        DropdownField(
                            label = stringResource(R.string.settings_path_emoji),
                            selected = emoji,
                            options = pathEmojis,
                            optionLabel = { it },
                            onSelect = { emoji = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Button(onClick = {
                        viewModel.add(month, day, label, emoji)
                        label = ""
                    }, enabled = label.isNotBlank()) { Text(stringResource(R.string.settings_category_add)) }
                }
            }
        }
    }
}
