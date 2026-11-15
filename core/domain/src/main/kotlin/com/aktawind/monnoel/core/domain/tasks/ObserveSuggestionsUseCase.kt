package com.aktawind.monnoel.core.domain.tasks

import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.domain.cooking.CookingRepository
import com.aktawind.monnoel.core.domain.edition.ObserveEditionsUseCase
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.domain.repository.UserPreferencesRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.LutinTask
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/** Liste de lutin du Noël affiché : tâches, suggestions à proposer et état de chaque suggestion. */
data class LutinState(
    val edition: ChristmasEdition?,
    val tasks: List<LutinTask>,
    val suggestions: List<Suggestion>,
    val states: Map<String, Boolean>,
)

class ObserveLutinUseCase @Inject constructor(
    private val observeEditions: ObserveEditionsUseCase,
    private val gifts: PlannedGiftRepository,
    private val advent: AdventRepository,
    private val cooking: CookingRepository,
    private val tasks: TaskRepository,
    private val preferences: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<LutinState> = observeEditions().map { it.current }.distinctUntilChanged().flatMapLatest { edition ->
        if (edition == null) return@flatMapLatest flowOf(LutinState(null, emptyList(), emptyList(), emptyMap()))
        val source = combine(
            gifts.observeGifts(edition.id),
            advent.observeCalendars(edition.id),
            advent.observeBoxesForEdition(edition.id),
            advent.observeSuppliesForEdition(edition.id),
            cooking.observeProductions(edition.id),
        ) { g, c, b, s, p -> SuggestionSource(g, c, b, s, p) }
        combine(
            source,
            tasks.observeTasks(edition.id),
            tasks.observeDismissed(edition.id),
            preferences.disabledSuggestions,
        ) { src, taskList, dismissed, disabled ->
            LutinState(
                edition = edition,
                tasks = taskList,
                // Une année archivée ne reçoit plus de suggestions.
                suggestions = if (edition.isReadOnly) emptyList() else currentSuggestions(src, disabled, dismissed, taskList),
                states = suggestionStates(src),
            )
        }
    }
}

/**
 * Tient à jour, en arrière-plan pendant que l'application est ouverte, les tâches venues de
 * suggestions : elles se cochent toutes seules quand c'est fait.
 */
@Singleton
class LutinAutoSync @Inject constructor(
    private val observeLutin: ObserveLutinUseCase,
    private val sync: SyncSuggestedTasksUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        observeLutin()
            .map { it.edition to it.states }
            .distinctUntilChanged()
            .onEach { (edition, states) -> if (edition != null && !edition.isReadOnly) sync(edition.id, states) }
            .launchIn(scope)
    }
}
