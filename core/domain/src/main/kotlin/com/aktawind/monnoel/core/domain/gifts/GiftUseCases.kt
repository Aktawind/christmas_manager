package com.aktawind.monnoel.core.domain.gifts

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.repository.EditionRepository
import com.aktawind.monnoel.core.domain.repository.GiftIdeaRepository
import com.aktawind.monnoel.core.domain.repository.PersonRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.repository.PlannedGiftRepository
import com.aktawind.monnoel.core.model.GiftIdea
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.Person
import com.aktawind.monnoel.core.model.PlannedGift
import javax.inject.Inject

/** Vérifie qu'un Noël existe et n'est pas archivé. */
class EditionGuard @Inject constructor(private val editions: EditionRepository) {
    suspend fun check(editionId: String): AppError? {
        val edition = editions.getById(editionId) ?: return AppError.NotFound
        return if (edition.isReadOnly) AppError.ReadOnly else null
    }
}

/** Enregistre une personne (création ou modification). */
class SavePersonUseCase @Inject constructor(
    private val persons: PersonRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(
        id: String?,
        firstName: String,
        lastName: String,
        photoPath: String?,
        interests: String,
        notes: String,
    ): AppResult<Person> {
        if (firstName.isBlank() && lastName.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        val now = time.now()
        val existing = id?.let { persons.getPerson(it) }
        existing?.photoPath?.takeIf { it != photoPath }?.let { photos.deletePhoto(it) }
        val person = Person(
            id = existing?.id ?: ids.newId(),
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            photoPath = photoPath,
            interests = interests.trim(),
            notes = notes.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        persons.upsert(person)
        return AppResult.Success(person)
    }
}

class DeletePersonUseCase @Inject constructor(
    private val persons: PersonRepository,
    private val photos: PhotoRepository,
) {
    /** Les cadeaux déjà prévus gardent le nom du destinataire (historique). */
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val person = persons.getPerson(id) ?: return AppResult.Failure(AppError.NotFound)
        persons.delete(id)
        person.photoPath?.let { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

/** Données saisies pour une idée. */
data class IdeaDraft(
    val title: String,
    val description: String = "",
    val photoPath: String? = null,
    val estimatedPriceCents: Long? = null,
    val url: String = "",
    val shop: String = "",
    val notes: String = "",
    val categoryId: String? = null,
    val personIds: Set<String> = emptySet(),
)

class SaveIdeaUseCase @Inject constructor(
    private val ideas: GiftIdeaRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, draft: IdeaDraft): AppResult<GiftIdea> {
        if (draft.title.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        val now = time.now()
        val existing = id?.let { ideas.getIdea(it) }
        existing?.photoPath?.takeIf { it != draft.photoPath }?.let { photos.deletePhoto(it) }
        val idea = GiftIdea(
            id = existing?.id ?: ids.newId(),
            title = draft.title.trim(),
            description = draft.description.trim(),
            photoPath = draft.photoPath,
            estimatedPriceCents = draft.estimatedPriceCents,
            url = draft.url.trim(),
            shop = draft.shop.trim(),
            notes = draft.notes.trim(),
            categoryId = draft.categoryId,
            personIds = draft.personIds,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        ideas.upsert(idea)
        return AppResult.Success(idea)
    }
}

class DeleteIdeaUseCase @Inject constructor(
    private val ideas: GiftIdeaRepository,
    private val photos: PhotoRepository,
) {
    /** Les cadeaux créés depuis cette idée sont conservés (ils ont leur propre copie). */
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val idea = ideas.getIdea(id) ?: return AppResult.Failure(AppError.NotFound)
        ideas.delete(id)
        idea.photoPath?.let { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

/** Données saisies pour un cadeau. */
data class GiftDraft(
    val personId: String?,
    val title: String,
    val description: String = "",
    val photoPath: String? = null,
    val priceCents: Long? = null,
    val purchaseDate: java.time.LocalDate? = null,
    val shop: String = "",
    val orderUrl: String = "",
    val trackingNumber: String = "",
    val notes: String = "",
    val categoryId: String? = null,
    val status: GiftStatus = GiftStatus.TO_BUY,
    val ideaId: String? = null,
)

class SaveGiftUseCase @Inject constructor(
    private val gifts: PlannedGiftRepository,
    private val persons: PersonRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, editionId: String, draft: GiftDraft): AppResult<PlannedGift> {
        if (draft.title.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        val existing = id?.let { gifts.getGift(it) }
        existing?.photoPath?.takeIf { it != draft.photoPath }?.let { photos.deletePhoto(it) }
        val person = draft.personId?.let { persons.getPerson(it) }
        val now = time.now()
        val gift = PlannedGift(
            id = existing?.id ?: ids.newId(),
            editionId = editionId,
            personId = person?.id,
            recipientName = person?.displayName ?: existing?.recipientName.orEmpty(),
            ideaId = draft.ideaId ?: existing?.ideaId,
            title = draft.title.trim(),
            description = draft.description.trim(),
            photoPath = draft.photoPath,
            priceCents = draft.priceCents,
            purchaseDate =
            draft.purchaseDate ?: if (draft.status.isPaid) existing?.purchaseDate ?: time.today() else null,
            shop = draft.shop.trim(),
            orderUrl = draft.orderUrl.trim(),
            trackingNumber = draft.trackingNumber.trim(),
            notes = draft.notes.trim(),
            categoryId = draft.categoryId,
            status = draft.status,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        gifts.upsert(gift)
        return AppResult.Success(gift)
    }
}

/** Transforme une idée en cadeau pour une personne et un Noël (copie figée de l'idée). */
class OfferIdeaUseCase @Inject constructor(
    private val ideas: GiftIdeaRepository,
    private val photos: PhotoRepository,
    private val saveGift: SaveGiftUseCase,
) {
    suspend operator fun invoke(ideaId: String, personId: String?, editionId: String): AppResult<PlannedGift> {
        val idea = ideas.getIdea(ideaId) ?: return AppResult.Failure(AppError.NotFound)
        val photo = idea.photoPath?.let { photos.copyPhoto(it) }
        val result = saveGift(
            id = null,
            editionId = editionId,
            draft = GiftDraft(
                personId = personId,
                title = idea.title,
                description = idea.description,
                photoPath = photo,
                priceCents = idea.estimatedPriceCents,
                shop = idea.shop,
                orderUrl = idea.url,
                notes = idea.notes,
                categoryId = idea.categoryId,
                ideaId = idea.id,
            ),
        )
        if (result is AppResult.Failure) photo?.let { photos.deletePhoto(it) }
        return result
    }
}

class ChangeGiftStatusUseCase @Inject constructor(
    private val gifts: PlannedGiftRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(giftId: String, status: GiftStatus): AppResult<PlannedGift> {
        val gift = gifts.getGift(giftId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(gift.editionId)?.let { return AppResult.Failure(it) }
        val updated = gift.copy(
            status = status,
            purchaseDate = gift.purchaseDate ?: if (status.isPaid) time.today() else null,
            updatedAt = time.now(),
        )
        gifts.upsert(updated)
        return AppResult.Success(updated)
    }
}

class DeleteGiftUseCase @Inject constructor(
    private val gifts: PlannedGiftRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val gift = gifts.getGift(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(gift.editionId)?.let { return AppResult.Failure(it) }
        gifts.delete(id)
        gift.photoPath?.let { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

class SetPersonBudgetUseCase @Inject constructor(
    private val gifts: PlannedGiftRepository,
    private val guard: EditionGuard,
) {
    suspend operator fun invoke(editionId: String, personId: String, amountCents: Long?): AppResult<Unit> {
        if (amountCents != null && amountCents < 0) return AppResult.Failure(AppError.InvalidInput)
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        gifts.setBudget(editionId, personId, amountCents?.takeIf { it > 0 })
        return AppResult.Success(Unit)
    }
}
