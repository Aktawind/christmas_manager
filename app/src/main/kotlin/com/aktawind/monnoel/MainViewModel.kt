package com.aktawind.monnoel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.gifts.IdeaDraft
import com.aktawind.monnoel.core.domain.gifts.SaveIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.SharedIdea
import com.aktawind.monnoel.core.domain.gifts.parseSharedText
import com.aktawind.monnoel.core.domain.repository.UserPreferencesRepository
import com.aktawind.monnoel.core.model.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val theme: ThemePreference, val hasEditions: Boolean) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    observeEditions: ObserveEditionsUseCase,
    private val saveIdea: SaveIdeaUseCase,
) : ViewModel() {
    val uiState: StateFlow<MainUiState> = combine(preferences.theme, observeEditions()) { theme, editions ->
        MainUiState.Ready(theme = theme, hasEditions = editions.editions.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    /** Idée reçue par le partage Android, en attente de confirmation. */
    private val _sharedIdea = MutableStateFlow<SharedIdea?>(null)
    val sharedIdea: StateFlow<SharedIdea?> = _sharedIdea.asStateFlow()

    private val _ideaSaved = MutableStateFlow(false)
    val ideaSaved: StateFlow<Boolean> = _ideaSaved.asStateFlow()

    fun onShared(text: String?, subject: String?) {
        parseSharedText(text, subject)?.let { _sharedIdea.value = it }
    }

    fun saveSharedIdea(title: String, url: String) {
        _sharedIdea.value = null
        viewModelScope.launch {
            if (saveIdea(null, IdeaDraft(title = title, url = url)) is AppResult.Success) _ideaSaved.value = true
        }
    }

    fun dismissSharedIdea() {
        _sharedIdea.value = null
    }

    fun ideaSavedShown() {
        _ideaSaved.value = false
    }
}
