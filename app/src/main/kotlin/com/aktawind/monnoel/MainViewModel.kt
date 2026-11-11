package com.aktawind.monnoel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.repository.UserPreferencesRepository
import com.aktawind.monnoel.core.model.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val theme: ThemePreference, val hasEditions: Boolean) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    observeEditions: ObserveEditionsUseCase,
) : ViewModel() {
    val uiState: StateFlow<MainUiState> = combine(preferences.theme, observeEditions()) { theme, editions ->
        MainUiState.Ready(theme = theme, hasEditions = editions.editions.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)
}
