package com.aktawind.monnoel.core.domain.edition

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.model.EditionStatus
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class EditionUseCasesTest {
    private val repo = FakeEditionRepository()
    private val prefs = FakePreferences()
    private val create = CreateEditionUseCase(repo, prefs, FixedTime(), SequentialIds())

    @Test
    fun `creer une annee la selectionne`() = runTest {
        val result = create(2026)
        assertIs<AppResult.Success<*>>(result)
        assertEquals("id-0", prefs.selectedEditionId.value)
        val state = ObserveEditionsUseCase(repo, prefs)().first()
        assertEquals(2026, state.current?.year)
    }

    @Test
    fun `refuse une annee deja existante`() = runTest {
        create(2026)
        val result = create(2026)
        assertEquals(AppResult.Failure(AppError.YearAlreadyExists(2026)), result)
    }

    @Test
    fun `refuse une annee hors limites`() = runTest {
        assertEquals(AppResult.Failure(AppError.InvalidInput), create(1999))
    }

    @Test
    fun `archiver puis desarchiver`() = runTest {
        create(2025)
        val archive = SetEditionArchivedUseCase(repo, FixedTime())
        archive("id-0", true)
        assertEquals(EditionStatus.ARCHIVED, repo.getById("id-0")?.status)
        archive("id-0", false)
        assertEquals(EditionStatus.ACTIVE, repo.getById("id-0")?.status)
    }

    @Test
    fun `supprimer l'annee selectionnee bascule sur une autre`() = runTest {
        create(2025)
        create(2026)
        DeleteEditionUseCase(repo, prefs)("id-1")
        assertNull(prefs.selectedEditionId.value)
        val state = ObserveEditionsUseCase(repo, prefs)().first()
        assertEquals(2025, state.current?.year)
    }

    @Test
    fun `annee affichee par defaut ignore les archives`() = runTest {
        create(2024, select = false)
        create(2025, select = false)
        SetEditionArchivedUseCase(repo, FixedTime())("id-1", true)
        val state = ObserveEditionsUseCase(repo, prefs)().first()
        assertEquals(2024, state.current?.year)
    }

    @Test
    fun `annee proposee pour un nouveau Noel`() {
        assertEquals(2026, suggestedNewYear(LocalDate.of(2026, 11, 11), emptyList()))
        assertEquals(2027, suggestedNewYear(LocalDate.of(2026, 11, 11), listOf(2026)))
        assertEquals(2027, suggestedNewYear(LocalDate.of(2026, 12, 27), emptyList()))
    }
}
