package com.aktawind.monnoel.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.countdown.CountdownState
import com.aktawind.monnoel.core.domain.countdown.christmasCountdown
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object NoEdition : HomeUiState

    data class Ready(
        val year: Int,
        val isArchived: Boolean,
        val countdown: CountdownState,
        /** Jour de l'année, pour choisir le message du jour. */
        val dayOfYear: Int,
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(observeEditions: ObserveEditionsUseCase, private val time: TimeProvider) :
    ViewModel() {
    val uiState: StateFlow<HomeUiState> = observeEditions()
        .map { state ->
            val edition = state.current ?: return@map HomeUiState.NoEdition
            val today = time.today()
            HomeUiState.Ready(
                year = edition.year,
                isArchived = edition.isArchived,
                countdown = christmasCountdown(today, edition.year),
                dayOfYear = today.dayOfYear,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}
