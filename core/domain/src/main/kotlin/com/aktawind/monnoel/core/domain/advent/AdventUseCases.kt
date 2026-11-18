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
import com.aktawind.monnoel.core.model.CalendarKind
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
    val kind: CalendarKind = CalendarKind.HOMEMADE,
    val priceCents: Long? = null,
    val shop: String = "",
    val bought: Boolean = false,
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
        val existing = id?.let { advent.getCalendar(it) }
        // Le type est choisi à la création et ne change plus (un calendrier tout prêt n'a pas de cases).
        val kind = existing?.kind ?: draft.kind
        val readyMade = kind == CalendarKind.READY_MADE
        if (draft.name.isBlank() || (!readyMade && draft.boxCount !in 1..MAX_BOX_COUNT) || (draft.priceCents ?: 0) < 0) {
            return AppResult.Failure(AppError.InvalidInput)
        }
        guard.check(editionId)?.let { return AppResult.Failure(it) }
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
            boxCount = if (readyMade) 0 else draft.boxCount,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            kind = kind,
            priceCents = if (readyMade) draft.priceCents else null,
            shop = if (readyMade) draft.shop.trim() else "",
            bought = readyMade && draft.bought,
        )
        advent.saveCalendar(calendar)
        if (readyMade) return AppResult.Success(calendar)
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

/** Calendrier tout prêt : à acheter ou acheté. */
class SetCalendarBoughtUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(id: String, bought: Boolean): AppResult<Unit> {
        val calendar = advent.getCalendar(id) ?: return AppResult.Failure(AppError.NotFound)
        if (!calendar.isReadyMade) return AppResult.Failure(AppError.InvalidInput)
        guard.check(calendar.editionId)?.let { return AppResult.Failure(it) }
        advent.saveCalendar(calendar.copy(bought = bought, updatedAt = time.now()))
        return AppResult.Success(Unit)
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

/** Données saisies pour une fourniture ou un achat groupé. */
data class SupplyDraft(
    val name: String,
    val quantity: Int = 1,
    val priceCents: Long? = null,
    val bought: Boolean = false,
    val photoPath: String? = null,
    val type: SurpriseType? = null,
    /** Contenu écrit dans chaque case reliée ; vide = le nom de la fourniture. */
    val boxTitle: String = "",
    val supplier: String = "",
)

/**
 * Remplit automatiquement les cases reliées à un achat groupé : type de surprise, contenu,
 * fournisseur ; si l'achat est fait, les cases « À trouver » passent « À préparer ».
 * Le prix n'est pas recopié : chaque case affiche sa part de l'achat.
 */
internal fun AdventBox.filledFrom(supply: AdventSupply): AdventBox = copy(
    type = supply.type ?: type,
    title = supply.contentForBoxes,
    supplier = supply.supplier.ifBlank { supplier },
    status = if (supply.bought && status == BoxStatus.TO_FIND) BoxStatus.TO_PREPARE else status,
    supplyId = supply.id,
)

class SaveSupplyUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(
        calendarId: String,
        supplyId: String?,
        draft: SupplyDraft,
        linkedBoxIds: Set<String>,
    ): AppResult<AdventSupply> {
        if (draft.name.isBlank() || draft.quantity < 1) return AppResult.Failure(AppError.InvalidInput)
        val calendar = advent.getCalendar(calendarId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(calendar.editionId)?.let { return AppResult.Failure(it) }
        val existing = advent.getSupplies(calendarId)
        val current = existing.firstOrNull { it.id == supplyId }
        current?.photoPath?.takeIf { it != draft.photoPath }?.let { photos.deletePhoto(it) }
        val supply = AdventSupply(
            id = current?.id ?: ids.newId(),
            calendarId = calendarId,
            name = draft.name.trim(),
            quantity = draft.quantity,
            priceCents = draft.priceCents,
            bought = draft.bought,
            sortOrder = current?.sortOrder ?: ((existing.maxOfOrNull { it.sortOrder } ?: -1) + 1),
            photoPath = draft.photoPath,
            type = draft.type,
            boxTitle = draft.boxTitle.trim(),
            supplier = draft.supplier.trim(),
        )
        advent.saveSupply(supply, linkedBoxIds)
        val filled = advent.getBoxes(calendarId).filter { it.id in linkedBoxIds }.map { it.filledFrom(supply) }
        if (filled.isNotEmpty()) advent.saveBoxes(filled)
        return AppResult.Success(supply)
    }
}

class DeleteSupplyUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
) {
    suspend operator fun invoke(calendarId: String, supplyId: String): AppResult<Unit> {
        val calendar = advent.getCalendar(calendarId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(calendar.editionId)?.let { return AppResult.Failure(it) }
        val supply = advent.getSupplies(calendarId).firstOrNull { it.id == supplyId }
        advent.deleteSupply(supplyId)
        supply?.photoPath?.let { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

/** Ce qu'on change d'un coup sur plusieurs cases (null = ne pas changer). */
data class BulkBoxChange(
    val type: SurpriseType? = null,
    val status: BoxStatus? = null,
    val title: String? = null,
    val priceCents: Long? = null,
    /** Relie les cases à cet achat groupé (et les remplit automatiquement). */
    val supplyId: String? = null,
)

/** Modifie plusieurs cases en une fois (ex. 24 cases « Chocolat », prêtes). */
class BulkUpdateBoxesUseCase @Inject constructor(
    private val advent: AdventRepository,
    private val guard: EditionGuard,
) {
    suspend operator fun invoke(calendarId: String, boxIds: Set<String>, change: BulkBoxChange): AppResult<Int> {
        if (boxIds.isEmpty()) return AppResult.Success(0)
        val calendar = advent.getCalendar(calendarId) ?: return AppResult.Failure(AppError.NotFound)
        guard.check(calendar.editionId)?.let { return AppResult.Failure(it) }
        val boxes = advent.getBoxes(calendarId)
        val supply = change.supplyId?.let { id -> advent.getSupplies(calendarId).firstOrNull { it.id == id } }
        if (supply != null) {
            val linked = boxes.filter { it.supplyId == supply.id }.map { it.id }.toSet() + boxIds
            advent.saveSupply(supply, linked)
        }
        val updated = boxes.filter { it.id in boxIds }.map { box ->
            var b = supply?.let { box.filledFrom(it) } ?: box
            change.type?.let { b = b.copy(type = it) }
            change.title?.takeIf { it.isNotBlank() }?.let { b = b.copy(title = it.trim()) }
            change.priceCents?.let { b = b.copy(priceCents = it) }
            change.status?.let { b = b.copy(status = it) }
            b
        }
        advent.saveBoxes(updated)
        return AppResult.Success(updated.size)
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
            // Un calendrier tout prêt recopié est à racheter pour la nouvelle année.
            bought = false,
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
            val photo = supply.photoPath?.let { photos.copyPhoto(it) }
            advent.saveSupply(supply.copy(id = newId, calendarId = copy.id, bought = false, photoPath = photo), linked)
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
