package com.aktawind.monnoel.feature.settings

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.designsystem.theme.LocalMonNoelColors
import com.aktawind.monnoel.core.domain.repository.UserPreferencesRepository
import com.aktawind.monnoel.core.model.SuggestionKind
import com.aktawind.monnoel.core.ui.settingLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SuggestionsSettingsViewModel @Inject constructor(private val preferences: UserPreferencesRepository) : ViewModel() {
    val disabled: StateFlow<Set<SuggestionKind>> =
        preferences.disabledSuggestions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setEnabled(kind: SuggestionKind, enabled: Boolean) {
        viewModelScope.launch { preferences.setSuggestionEnabled(kind, enabled) }
    }
}

/** Sous-écran : quelles suggestions le lutin peut proposer dans « Ma liste ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsSettingsRoute(onBack: () -> Unit, viewModel: SuggestionsSettingsViewModel = hiltViewModel()) {
    val disabled by viewModel.disabled.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_suggestions)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                SettingsCard(stringResource(R.string.settings_suggestions_card)) {
                    Text(
                        stringResource(R.string.settings_suggestions_explain),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalMonNoelColors.current.secondaryText,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SuggestionKind.entries.forEach { kind ->
                            val enabled = kind !in disabled
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .toggleable(value = enabled, onValueChange = { viewModel.setEnabled(kind, it) }, role = Role.Switch)
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(kind.settingLabel), modifier = Modifier.weight(1f))
                                Switch(checked = enabled, onCheckedChange = null)
                            }
                        }
                    }
                }
            }
        }
    }
}
