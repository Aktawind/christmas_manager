package com.aktawind.monnoel.core.domain.traditions

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.search.normalizeForSearch
import com.aktawind.monnoel.core.model.Craft
import com.aktawind.monnoel.core.model.CraftCategory
import com.aktawind.monnoel.core.model.CraftStatus
import com.aktawind.monnoel.core.model.CraftSupply
import com.aktawind.monnoel.core.model.CraftYear
import com.aktawind.monnoel.core.model.DecorIdea
import com.aktawind.monnoel.core.model.DecorPlace
import javax.inject.Inject

data class CraftDraft(
    val title: String,
    val category: CraftCategory = CraftCategory.DECORATION,
    val photoPath: String? = null,
    val tutorialUrl: String = "",
    val notes: String = "",
)

/** Une idée de bricolage à piocher, avec ses fournitures de base. */
data class CraftIdea(val title: String, val category: CraftCategory, val supplies: List<String>)

val craftIdeas: List<CraftIdea> = listOf(
    CraftIdea(
        "Couronne de porte en branches de sapin",
        CraftCategory.DECORATION,
        listOf("Couronne en paille", "Branches de sapin", "Ruban", "Fil de fer"),
    ),
    CraftIdea(
        "Boules en pommes de pin pailletées",
        CraftCategory.DECORATION,
        listOf("Pommes de pin", "Peinture dorée", "Paillettes", "Ficelle"),
    ),
    CraftIdea("Guirlande d'oranges séchées", CraftCategory.DECORATION, listOf("Oranges", "Ficelle", "Bâtons de cannelle")),
    CraftIdea("Bougeoirs en bocaux givrés", CraftCategory.DECORATION, listOf("Bocaux", "Sel", "Colle", "Bougies chauffe-plat")),
    CraftIdea("Centre de table nature", CraftCategory.DECORATION, listOf("Rondin de bois", "Branches", "Bougies", "Pommes de pin")),
    CraftIdea("Flocons en papier découpé", CraftCategory.KIDS, listOf("Papier blanc", "Ciseaux")),
    CraftIdea("Bonhommes de neige en chaussettes", CraftCategory.KIDS, listOf("Chaussettes blanches", "Riz", "Boutons", "Ruban")),
    CraftIdea("Rennes en bouchons de liège", CraftCategory.KIDS, listOf("Bouchons", "Cure-pipes", "Yeux mobiles", "Pompons rouges")),
    CraftIdea("Étiquettes cadeaux faites main", CraftCategory.WRAPPING, listOf("Papier kraft", "Tampons", "Ficelle")),
    CraftIdea("Papier cadeau tamponné", CraftCategory.WRAPPING, listOf("Papier kraft", "Tampons", "Encre")),
    CraftIdea("Cartes de vœux brodées", CraftCategory.CARDS, listOf("Cartes blanches", "Fil à broder", "Aiguille")),
    CraftIdea("Cartes pop-up sapin", CraftCategory.CARDS, listOf("Papier cartonné", "Ciseaux", "Colle")),
    CraftIdea("Bougies parfumées à offrir", CraftCategory.HANDMADE_GIFT, listOf("Cire de soja", "Mèches", "Huile essentielle", "Pots")),
    CraftIdea(
        "Pots de mélange à cookies",
        CraftCategory.HANDMADE_GIFT,
        listOf("Bocaux", "Farine", "Sucre", "Pépites de chocolat", "Ruban"),
    ),
    CraftIdea("Sels de bain à la cannelle", CraftCategory.HANDMADE_GIFT, listOf("Gros sel", "Huile essentielle", "Petits pots")),
)

/** Idées pas encore dans les bricolages (même titre, accents et majuscules ignorés). */
fun availableCraftIdeas(existing: List<Craft>): List<CraftIdea> {
    val titles = existing.map { normalizeForSearch(it.title) }.toSet()
    return craftIdeas.filter { normalizeForSearch(it.title) !in titles }
}

class SaveCraftUseCase @Inject constructor(
    private val crafts: CraftRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, draft: CraftDraft): AppResult<Craft> {
        if (draft.title.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        val existing = id?.let { crafts.getCraft(it) }
        existing?.photoPath?.takeIf { it != draft.photoPath }?.let { photos.deletePhoto(it) }
        val now = time.now()
        val craft = Craft(
            existing?.id ?: ids.newId(),
            draft.title.trim(),
            draft.photoPath,
            draft.category,
            draft.tutorialUrl.trim(),
            draft.notes.trim(),
            existing?.createdAt ?: now,
            now,
        )
        crafts.saveCraft(craft)
        return AppResult.Success(craft)
    }
}

class AddCraftIdeasUseCase @Inject constructor(
    private val save: SaveCraftUseCase,
    private val crafts: CraftRepository,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(ideas: List<CraftIdea>): Int = ideas.count { idea ->
        val craft = (save(null, CraftDraft(idea.title, idea.category)) as? AppResult.Success)?.value ?: return@count false
        idea.supplies.forEachIndexed { i, name -> crafts.saveSupply(CraftSupply(ids.newId(), craft.id, name, null, i)) }
        true
    }
}

class DeleteCraftUseCase @Inject constructor(private val crafts: CraftRepository, private val photos: PhotoRepository) {
    /** Supprime le bricolage, ses fournitures, son avancement de chaque année et leurs photos. */
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val craft = crafts.getCraft(id) ?: return AppResult.Failure(AppError.NotFound)
        val results = crafts.getYearsOf(id).mapNotNull { it.resultPhotoPath }
        crafts.deleteCraft(id)
        (listOfNotNull(craft.photoPath) + results).forEach { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

class SaveCraftSupplyUseCase @Inject constructor(private val crafts: CraftRepository, private val ids: IdGenerator) {
    suspend operator fun invoke(id: String?, craftId: String, name: String, priceCents: Long?): AppResult<CraftSupply> {
        if (name.isBlank() || (priceCents ?: 0) < 0) return AppResult.Failure(AppError.InvalidInput)
        val existing = crafts.getSupplies(craftId)
        val order = existing.firstOrNull { it.id == id }?.sortOrder ?: ((existing.maxOfOrNull { it.sortOrder } ?: -1) + 1)
        val supply = CraftSupply(id ?: ids.newId(), craftId, name.trim(), priceCents, order)
        crafts.saveSupply(supply)
        return AppResult.Success(supply)
    }
}

class DeleteCraftSupplyUseCase @Inject constructor(private val crafts: CraftRepository) {
    suspend operator fun invoke(id: String) = crafts.deleteSupply(id)
}

/**
 * Avancement d'un bricolage pour un Noël. null = simple idée (retiré du programme, photo du
 * résultat supprimée).
 */
class SetCraftStatusUseCase @Inject constructor(
    private val crafts: CraftRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(editionId: String, craftId: String, status: CraftStatus?): AppResult<Unit> {
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        val existing = crafts.getYear(editionId, craftId)
        when {
            status == null && existing != null -> {
                crafts.deleteYear(existing.id)
                existing.resultPhotoPath?.let { photos.deletePhoto(it) }
            }
            status != null -> crafts.saveYear(
                existing?.copy(status = status) ?: CraftYear(ids.newId(), editionId, craftId, status, null, time.now()),
            )
        }
        return AppResult.Success(Unit)
    }
}

/** Photo du résultat (le bricolage passe au programme s'il n'y était pas). */
class SetCraftResultPhotoUseCase @Inject constructor(
    private val crafts: CraftRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(editionId: String, craftId: String, photoPath: String?): AppResult<Unit> {
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        val existing = crafts.getYear(editionId, craftId)
        existing?.resultPhotoPath?.takeIf { it != photoPath }?.let { photos.deletePhoto(it) }
        val year = existing ?: CraftYear(ids.newId(), editionId, craftId, CraftStatus.DONE, null, time.now())
        crafts.saveYear(year.copy(resultPhotoPath = photoPath))
        return AppResult.Success(Unit)
    }
}

class SetCraftSupplyBoughtUseCase @Inject constructor(private val crafts: CraftRepository, private val guard: EditionGuard) {
    suspend operator fun invoke(editionId: String, supplyId: String, bought: Boolean): AppResult<Unit> {
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        crafts.setBought(editionId, supplyId, bought)
        return AppResult.Success(Unit)
    }
}

data class DecorDraft(
    val title: String,
    val place: DecorPlace = DecorPlace.TREE,
    val photoPath: String? = null,
    val url: String = "",
    val comment: String = "",
    val favorite: Boolean = false,
)

class SaveDecorUseCase @Inject constructor(
    private val crafts: CraftRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, draft: DecorDraft): AppResult<DecorIdea> {
        if (draft.title.isBlank() && draft.photoPath == null) return AppResult.Failure(AppError.InvalidInput)
        val existing = id?.let { crafts.getDecor(it) }
        existing?.photoPath?.takeIf { it != draft.photoPath }?.let { photos.deletePhoto(it) }
        val now = time.now()
        val decor = DecorIdea(
            existing?.id ?: ids.newId(), draft.title.trim(), draft.photoPath, draft.url.trim(), draft.comment.trim(), draft.place,
            draft.favorite, existing?.createdAt ?: now, now,
        )
        crafts.saveDecor(decor)
        return AppResult.Success(decor)
    }
}

class DeleteDecorUseCase @Inject constructor(private val crafts: CraftRepository, private val photos: PhotoRepository) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val decor = crafts.getDecor(id) ?: return AppResult.Failure(AppError.NotFound)
        crafts.deleteDecor(id)
        decor.photoPath?.let { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

/** Copie d'un bricolage avec ses fournitures (photo comprise), sans son avancement. */
class DuplicateCraftUseCase @Inject constructor(
    private val crafts: CraftRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String, newTitle: String): AppResult<Craft> {
        val source = crafts.getCraft(id) ?: return AppResult.Failure(AppError.NotFound)
        val now = time.now()
        val copy = source.copy(
            id = ids.newId(),
            title = newTitle.trim().ifBlank { source.title },
            photoPath = source.photoPath?.let { photos.copyPhoto(it) },
            createdAt = now,
            updatedAt = now,
        )
        crafts.saveCraft(copy)
        crafts.getSupplies(id).forEach { crafts.saveSupply(it.copy(id = ids.newId(), craftId = copy.id)) }
        return AppResult.Success(copy)
    }
}

/** Copie d'une idée déco (photo comprise). */
class DuplicateDecorUseCase @Inject constructor(
    private val crafts: CraftRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String, newTitle: String): AppResult<DecorIdea> {
        val source = crafts.getDecor(id) ?: return AppResult.Failure(AppError.NotFound)
        val now = time.now()
        val copy = source.copy(
            id = ids.newId(),
            title = newTitle.trim(),
            photoPath = source.photoPath?.let { photos.copyPhoto(it) },
            createdAt = now,
            updatedAt = now,
        )
        crafts.saveDecor(copy)
        return AppResult.Success(copy)
    }
}
