package com.aktawind.monnoel.core.domain.advent

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.model.AdventBox
import com.aktawind.monnoel.core.model.AdventCalendar
import com.aktawind.monnoel.core.model.AdventSupply
import com.aktawind.monnoel.core.model.AdventTemplate
import com.aktawind.monnoel.core.model.AdventTemplateBox
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.SurpriseType
import javax.inject.Inject

/** Données saisies pour un calendrier. */
data class CalendarDraft(
    val name: String,
    val personIds: Set<String> = emptySet(),
    val description: String = "",
    val coverPhotoPath: String? = null,
    val theme: String = "",
    val budgetCents: Long? = null,
    val notes: String = "",
    val boxCount: Int = DEFAULT_BOX_COUNT,
)

/** Données saisies pour une case. */
data class BoxDraft(
    val type: SurpriseType? = null,
    val title: String = "",
    val photoPath: String? = null,
    val priceCents: Long? = null,
    val supplier: String = "",
    val url: String = "",
    val status: BoxStatus = BoxStatus.TO_FIND,
    val notes: String = "",
    val supplyId: String? = null,
)

private fun emptyBox(id: String, calendarId: String, number: Int) = AdventBox(
    id = id,
    calendarId = calendarId,
    number = number,
    type = null,
    title = "",
    photoPath = null,
    priceCents = null,
    supplier = "",
    url = "",
    status = BoxStatus.TO_FIND,
    notes = "",
    supplyId = null,
)

class SaveCalendarUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    /**
     * Crée ou modifie un calendrier. Les cases manquantes sont créées ; si le nombre de cases
     * diminue, les dernières cases sont supprimées (l'écran prévient avant).
     */
    suspend operator fun invoke(id: String?, editionId: String, draft: CalendarDraft): AppResult<AdventCalendar> {
        if (draft.name.isBlank() || draft.boxCount !in 1..MAX_BOX_COUNT) return AppResult.Failure(AppError.InvalidInput)
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        val existing = id?.let { advent.getCalendar(it) }
        existing?.coverPhotoPath?.takeIf { it != draft.coverPhotoPath }?.let { photos.deletePhoto(it) }
        val now = time.now()
        val calendar = AdventCalendar(
            id = existing?.id ?: ids.newId(),
            editionId = existing?.editionId ?: editionId,
            name = draft.name.trim(),
            personIds = draft.personIds,
            description = draft.description.trim(),
            coverPhotoPath = draft.coverPhotoPath,
            theme = draft.theme.trim(),
            budgetCents = draft.budgetCents,
            notes = draft.notes.trim(),
            boxCount = draft.boxCount,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        advent.saveCalendar(calendar)
        val boxes = advent.getBoxes(calendar.id)
        val removed = boxes.filter { it.number > draft.boxCount }
        if (removed.isNotEmpty()) {
            removed.mapNotNull { it.photoPath }.forEach { photos.deletePhoto(it) }
            advent.deleteBoxes(removed.map { it.id })
        }
        val existingNumbers = boxes.map { it.number }.toSet()
        val missing = (1..draft.boxCount).filter { it !in existingNumbers }
        if (missing.isNotEmpty()) advent.saveBoxes(missing.map { emptyBox(ids.newId(), calendar.id, it) })
        return AppResult.Success(calendar)
    }
}

class DeleteCalendarUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val calendar = advent.getCalendar(id) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(calendar.editionId)?.let { return AppResult.Failure(it) }
        val boxPhotos = advent.getBoxes(id).mapNotNull { it.photoPath }
        advent.deleteCalendar(id)
        (boxPhotos + listOfNotNull(calendar.coverPhotoPath)).forEach { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

class SaveBoxUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
) {
    suspend operator fun invoke(boxId: String, draft: BoxDraft): AppResult<AdventBox> {
        val box = advent.getBox(boxId) ?: return AppResult.Failure(AppError.NotFound)
        val calendar = advent.getCalendar(box.calendarId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(calendar.editionId)?.let { return AppResult.Failure(it) }
        box.photoPath?.takeIf { it != draft.photoPath }?.let { photos.deletePhoto(it) }
        val updated = box.copy(
            type = draft.type,
            title = draft.title.trim(),
            photoPath = draft.photoPath,
            priceCents = draft.priceCents,
            supplier = draft.supplier.trim(),
            url = draft.url.trim(),
            status = draft.status,
            notes = draft.notes.trim(),
            supplyId = draft.supplyId,
        )
        advent.saveBoxes(listOf(updated))
        return AppResult.Success(updated)
    }
}

class SetBoxStatusUseCase @Inject constructor(private val advent: AdventRepository, private val guard: EditionGuard) {
    suspend operator fun invoke(boxId: String, status: BoxStatus): AppResult<Unit> {
        val box = advent.getBox(boxId) ?: return AppResult.Failure(AppError.NotFound)
        val calendar = advent.getCalendar(box.calendarId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(calendar.editionId)?.let { return AppResult.Failure(it) }
        advent.saveBoxes(listOf(box.copy(status = status)))
        return AppResult.Success(Unit)
    }
}

class SwapBoxesUseCase @Inject constructor(private val advent: AdventRepository, private val guard: EditionGuard) {
    suspend operator fun invoke(firstId: String, secondId: String): AppResult<Unit> {
        if (firstId == secondId) return AppResult.Success(Unit)
        val first = advent.getBox(firstId) ?: return AppResult.Failure(AppError.NotFound)
        val second = advent.getBox(secondId) ?: return AppResult.Failure(AppError.NotFound)
        if (first.calendarId != second.calendarId) return AppResult.Failure(AppError.InvalidInput)
        val calendar = advent.getCalendar(first.calendarId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(calendar.editionId)?.let { return AppResult.Failure(it) }
        advent.swapBoxes(firstId, secondId)
        return AppResult.Success(Unit)
    }
}

class SaveSupplyUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val guard: EditionGuard,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(
        calendarId: String,
        supplyId: String?,
        name: String,
        quantity: Int,
        priceCents: Long?,
        bought: Boolean,
        linkedBoxIds: Set<String>,
    ): AppResult<AdventSupply> {
        if (name.isBlank() || quantity < 1) return AppResult.Failure(AppError.InvalidInput)
        val calendar = advent.getCalendar(calendarId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(calendar.editionId)?.let { return AppResult.Failure(it) }
        val existing = advent.getSupplies(calendarId)
        val current = existing.firstOrNull { it.id == supplyId }
        val supply = AdventSupply(
            id = current?.id ?: ids.newId(),
            calendarId = calendarId,
            name = name.trim(),
            quantity = quantity,
            priceCents = priceCents,
            bought = bought,
            sortOrder = current?.sortOrder ?: ((existing.maxOfOrNull { it.sortOrder } ?: -1) + 1),
        )
        advent.saveSupply(supply, linkedBoxIds)
        return AppResult.Success(supply)
    }
}

class DeleteSupplyUseCase @Inject constructor(private val advent: AdventRepository, private val guard: EditionGuard) {
    suspend operator fun invoke(calendarId: String, supplyId: String): AppResult<Unit> {
        val calendar = advent.getCalendar(calendarId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(calendar.editionId)?.let { return AppResult.Failure(it) }
        advent.deleteSupply(supplyId)
        return AppResult.Success(Unit)
    }
}

/**
 * Copie un calendrier (d'une année passée par exemple) dans un Noël : nouvelles cases avec le
 * même contenu, toutes remises « À trouver » ; fournitures à racheter ; photos copiées.
 */
class DuplicateCalendarUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(sourceId: String, targetEditionId: String): AppResult<AdventCalendar> {
        val source = advent.getCalendar(sourceId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(targetEditionId)?.let { return AppResult.Failure(it) }
        val now = time.now()
        val copy = source.copy(
            id = ids.newId(),
            editionId = targetEditionId,
            coverPhotoPath = source.coverPhotoPath?.let { photos.copyPhoto(it) },
            createdAt = now,
            updatedAt = now,
        )
        advent.saveCalendar(copy)
        val supplyIds = advent.getSupplies(sourceId).associate { it.id to ids.newId() }
        val boxes = advent.getBoxes(sourceId).map { box ->
            box.copy(
                id = ids.newId(),
                calendarId = copy.id,
                status = BoxStatus.TO_FIND,
                photoPath = box.photoPath?.let { photos.copyPhoto(it) },
                supplyId = null,
            ) to box.supplyId
        }
        advent.saveBoxes(boxes.map { it.first })
        advent.getSupplies(sourceId).forEach { supply ->
            val newId = supplyIds.getValue(supply.id)
            val linked = boxes.filter { it.second == supply.id }.map { it.first.id }.toSet()
            advent.saveSupply(supply.copy(id = newId, calendarId = copy.id, bought = false), linked)
        }
        return AppResult.Success(copy)
    }
}

/** Enregistre le contenu d'un calendrier comme modèle personnel réutilisable. */
class SaveAsTemplateUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(calendarId: String, name: String): AppResult<AdventTemplate> {
        if (name.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        val calendar = advent.getCalendar(calendarId) ?: return AppResult.Failure(AppError.NotFound)
        val template =
            AdventTemplate(
                ids.newId(),
                name.trim(),
                calendar.theme,
                calendar.description,
                calendar.boxCount,
                time.now(),
            )
        val boxes = advent.getBoxes(calendarId).filterNot { it.isEmpty }.map {
            AdventTemplateBox(ids.newId(), template.id, it.number, it.type, it.title, it.priceCents, it.supplier, it.url, it.notes)
        }
        advent.saveTemplate(template, boxes)
        return AppResult.Success(template)
    }
}

/** Crée un calendrier dans un Noël à partir d'un modèle personnel. */
class CreateFromTemplateUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val saveCalendar: SaveCalendarUseCase,
) {
    suspend operator fun invoke(
        templateId: String,
        editionId: String,
        name: String,
        personIds: Set<String>,
    ): AppResult<AdventCalendar> {
        val template = advent.getTemplate(templateId) ?: return AppResult.Failure(AppError.NotFound)
        val result = saveCalendar(
            id = null,
            editionId = editionId,
            draft = CalendarDraft(
                name = name.ifBlank { template.name },
                personIds = personIds,
                description = template.description,
                theme = template.theme,
                boxCount = template.boxCount,
            ),
        )
        val calendar = (result as? AppResult.Success)?.value ?: return result
        val byNumber = advent.getTemplateBoxes(templateId).associateBy { it.number }
        val filled = advent.getBoxes(calendar.id).mapNotNull { box ->
            byNumber[box.number]?.let { t ->
                box.copy(
                    type = t.type,
                    title = t.title,
                    priceCents = t.priceCents,
                    supplier = t.supplier,
                    url = t.url,
                    notes = t.notes,
                )
            }
        }
        if (filled.isNotEmpty()) advent.saveBoxes(filled)
        return result
    }
}
