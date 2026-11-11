package com.aktawind.monnoel.core.domain.edition

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.repository.EditionRepository
import com.aktawind.monnoel.core.domain.repository.UserPreferencesRepository
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.EditionStatus
import java.time.LocalDate
import java.time.Month
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

const val MIN_YEAR = 2000
const val MAX_YEAR = 2100

/**
 * Année de Noël « naturelle » pour une date : jusqu'au 26 décembre on prépare le Noël
 * de l'année en cours, ensuite celui de l'année suivante.
 */
fun christmasYearFor(today: LocalDate): Int =
    if (today.month == Month.DECEMBER && today.dayOfMonth > 26) today.year + 1 else today.year

/** Année proposée par défaut lors de la création d'un nouveau Noël. */
fun suggestedNewYear(today: LocalDate, existingYears: Collection<Int>): Int {
    var year = christmasYearFor(today)
    while (year in existingYears) year++
    return year
}

/**
 * Choisit l'année affichée : celle sélectionnée par l'utilisateur si elle existe encore,
 * sinon la plus récente non archivée, sinon la plus récente.
 */
fun resolveCurrentEdition(editions: List<ChristmasEdition>, selectedId: String?): ChristmasEdition? =
    editions.firstOrNull { it.id == selectedId }
        ?: editions.filter { !it.isArchived }.maxByOrNull { it.year }
        ?: editions.maxByOrNull { it.year }

class ObserveEditionsUseCase @Inject constructor(
    private val editions: EditionRepository,
    private val preferences: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<EditionsState> = combine(
        editions.observeEditions(),
        preferences.selectedEditionId,
    ) { list, selectedId ->
        EditionsState(editions = list, current = resolveCurrentEdition(list, selectedId))
    }
}

data class EditionsState(val editions: List<ChristmasEdition>, val current: ChristmasEdition?)

class CreateEditionUseCase @Inject constructor(
    private val editions: EditionRepository,
    private val preferences: UserPreferencesRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(year: Int, select: Boolean = true): AppResult<ChristmasEdition> {
        if (year !in MIN_YEAR..MAX_YEAR) return AppResult.Failure(AppError.InvalidInput)
        if (editions.getByYear(year) != null) return AppResult.Failure(AppError.YearAlreadyExists(year))
        val now = time.now()
        val edition = ChristmasEdition(
            id = ids.newId(),
            year = year,
            status = EditionStatus.ACTIVE,
            plannedBudgetCents = null,
            createdAt = now,
            updatedAt = now,
        )
        editions.insert(edition)
        if (select) preferences.setSelectedEditionId(edition.id)
        return AppResult.Success(edition)
    }
}

class SetEditionArchivedUseCase @Inject constructor(
    private val editions: EditionRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(id: String, archived: Boolean): AppResult<ChristmasEdition> {
        val edition = editions.getById(id) ?: return AppResult.Failure(AppError.NotFound)
        val updated = edition.copy(
            status = if (archived) EditionStatus.ARCHIVED else EditionStatus.ACTIVE,
            updatedAt = time.now(),
        )
        editions.update(updated)
        return AppResult.Success(updated)
    }
}

class DeleteEditionUseCase @Inject constructor(
    private val editions: EditionRepository,
    private val preferences: UserPreferencesRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        editions.getById(id) ?: return AppResult.Failure(AppError.NotFound)
        editions.delete(id)
        if (preferences.selectedEditionId.first() == id) preferences.setSelectedEditionId(null)
        return AppResult.Success(Unit)
    }
}

class SelectEditionUseCase @Inject constructor(
    private val editions: EditionRepository,
    private val preferences: UserPreferencesRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        editions.getById(id) ?: return AppResult.Failure(AppError.NotFound)
        preferences.setSelectedEditionId(id)
        return AppResult.Success(Unit)
    }
}
