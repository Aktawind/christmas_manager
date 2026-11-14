package com.aktawind.monnoel.core.domain.repository

import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.model.GiftCategory
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PersonBudget
import com.aktawind.monnoel.core.model.PlannedGift
import kotlinx.coroutines.flow.Flow

interface PersonRepository {
    fun observePersons(): Flow<List<Person>>

    fun observePerson(id: String): Flow<Person?>

    suspend fun getPerson(id: String): Person?

    suspend fun upsert(person: Person)

    suspend fun delete(id: String)
}

interface GiftCategoryRepository {
    fun observeCategories(): Flow<List<GiftCategory>>

    suspend fun upsert(category: GiftCategory)

    /** Les idées et cadeaux de cette catégorie passent « sans catégorie ». */
    suspend fun delete(id: String)
}

interface GiftIdeaRepository {
    fun observeIdeas(): Flow<List<GiftIdea>>

    fun observeIdea(id: String): Flow<GiftIdea?>

    suspend fun getIdea(id: String): GiftIdea?

    suspend fun upsert(idea: GiftIdea)

    suspend fun delete(id: String)
}

interface PlannedGiftRepository {
    fun observeGifts(editionId: String): Flow<List<PlannedGift>>

    /** Tous les cadeaux d'une personne, toutes années confondues. */
    fun observeGiftsForPerson(personId: String): Flow<List<PlannedGift>>

    fun observeGift(id: String): Flow<PlannedGift?>

    suspend fun getGift(id: String): PlannedGift?

    suspend fun upsert(gift: PlannedGift)

    suspend fun delete(id: String)

    fun observeBudgets(editionId: String): Flow<List<PersonBudget>>

    suspend fun setBudget(editionId: String, personId: String, amountCents: Long?)
}

/** Stockage des photos (redimensionnées) dans le dossier des médias. */
interface PhotoRepository {
    /** Importe une image (URI de la galerie ou de l'appareil photo) ; renvoie son chemin relatif. */
    suspend fun importPhoto(sourceUri: String): AppResult<String>

    /** Copie une photo existante (pour figer l'historique d'un cadeau). */
    suspend fun copyPhoto(relativePath: String): String?

    suspend fun deletePhoto(relativePath: String)
}

/** Lecture d'une page web pour pré-remplir une idée partagée (facultatif, nécessite Internet). */
interface LinkPreviewRepository {
    suspend fun fetch(url: String): com.aktawind.monnoel.core.domain.gifts.PageFetch

    /** Télécharge une image dans le cache ; renvoie une URI locale lisible, ou null. */
    suspend fun downloadImage(url: String, referer: String): String?
}
