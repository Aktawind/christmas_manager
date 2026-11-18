package com.aktawind.monnoel.core.domain.gifts

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.edition.FakeEditionRepository
import com.aktawind.monnoel.core.domain.edition.FixedTime
import com.aktawind.monnoel.core.domain.edition.SequentialIds
import com.aktawind.monnoel.core.model.ChristmasEdition
import com.aktawind.monnoel.core.model.EditionStatus
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.WhisperedIdea
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class FakeWhispers : WhisperRepository {
    val items = MutableStateFlow<List<WhisperedIdea>>(emptyList())

    override fun observeWhispers(editionId: String) = items.map { l -> l.filter { it.editionId == editionId } }

    override fun observeAllWhispers() = items

    override suspend fun getWhisper(id: String) = items.value.firstOrNull { it.id == id }

    override suspend fun saveWhisper(whisper: WhisperedIdea) {
        items.value = items.value.filterNot { it.id == whisper.id } + whisper
    }

    override suspend fun deleteWhisper(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
}

class WhispersTest {
    private val t = Instant.EPOCH
    private val editions = FakeEditionRepository()
    private val persons = FakePersons()
    private val whispers = FakeWhispers()
    private val guard = EditionGuard(editions)
    private val photos = FakePhotos()
    private val save = SaveWhisperUseCase(whispers, persons, photos, guard, FixedTime(), SequentialIds())

    private suspend fun setUp() {
        editions.insert(ChristmasEdition("e", 2026, EditionStatus.ACTIVE, null, t, t))
        editions.insert(ChristmasEdition("old", 2025, EditionStatus.ARCHIVED, null, t, t))
        listOf("yannick" to "Yannick", "christine" to "Christine", "papa" to "Papa").forEach { (id, name) ->
            persons.upsert(Person(id, name, "", null, "", "", t, t))
        }
    }

    @Test
    fun `une idee soufflee garde pour qui, a qui et le Noel`() = runTest {
        setUp()
        val w = (
            save(
                null,
                "e",
                WhisperDraft("yannick", "Montre", givenToPersonId = "christine", priceCents = 8_000),
            ) as AppResult.Success
            ).value
        assertEquals("Christine", w.givenToName)
        assertEquals("e", w.editionId)
        val free = (save(null, "e", WhisperDraft("yannick", "Livre", givenToName = " Collègues ")) as AppResult.Success).value
        assertEquals("Collègues", free.givenToName)
        // Il faut une idée et quelqu'un à qui on l'a soufflée ; pas dans une année archivée.
        assertEquals(AppResult.Failure(AppError.InvalidInput), save(null, "e", WhisperDraft("yannick", "Montre")))
        assertEquals(AppResult.Failure(AppError.InvalidInput), save(null, "e", WhisperDraft("yannick", " ", givenToName = "Papa")))
        assertEquals(AppResult.Failure(AppError.ReadOnly), save(null, "old", WhisperDraft("yannick", "Montre", givenToName = "Papa")))
    }

    @Test
    fun `l'app repere une idee deja soufflee a quelqu'un d'autre`() = runTest {
        setUp()
        save(null, "e", WhisperDraft("yannick", "Montre connectée", givenToPersonId = "christine"))
        val all = whispers.items.value
        // Même idée (accents et majuscules ignorés), même personne, même Noël, autre personne qui l'offrirait : doublon.
        val dup = whisperDuplicates(all, "e", "yannick", "montre connectee", "papa", "", null)
        assertEquals("Christine", dup.single().givenToName)
        // Même personne à qui on la souffle : pas un doublon. Autre destinataire ou autre Noël : non plus.
        assertTrue(whisperDuplicates(all, "e", "yannick", "Montre connectée", "christine", "", null).isEmpty())
        assertTrue(whisperDuplicates(all, "e", "papa", "Montre connectée", "christine", "", null).isEmpty())
        assertTrue(whisperDuplicates(all, "old", "yannick", "Montre connectée", "papa", "", null).isEmpty())
    }

    @Test
    fun `la photo d'une idee est copiee, et supprimee avec l'idee soufflee`() = runTest {
        setUp()
        val draft = WhisperDraft("yannick", "Montre", givenToName = "Papa", photoPath = "photos/idee.jpg", photoFromIdea = true)
        val w = (save(null, "e", draft) as AppResult.Success).value
        assertEquals("photos/copy-0.jpg", w.photoPath)
        DeleteWhisperUseCase(whispers, photos, guard)(w.id)
        assertEquals(listOf("photos/copy-0.jpg"), photos.deleted)
    }
}
