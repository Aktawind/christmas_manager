package com.aktawind.monnoel.core.domain.traditions

import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.domain.gifts.EditionGuard
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.domain.search.normalizeForSearch
import com.aktawind.monnoel.core.model.Inspiration
import com.aktawind.monnoel.core.model.InspirationKind
import com.aktawind.monnoel.core.model.Tradition
import com.aktawind.monnoel.core.model.TraditionCategory
import com.aktawind.monnoel.core.model.TraditionPeriod
import com.aktawind.monnoel.core.model.TraditionYear
import javax.inject.Inject

data class TraditionDraft(
    val name: String,
    val category: TraditionCategory = TraditionCategory.HOME,
    val period: TraditionPeriod = TraditionPeriod.ADVENT,
    val photoPath: String? = null,
    val personIds: Set<String> = emptySet(),
    val notes: String = "",
    val favorite: Boolean = false,
)

/** Une idée de tradition à piocher (ajoutée seulement si on la choisit). */
data class TraditionIdea(val name: String, val category: TraditionCategory, val period: TraditionPeriod)

/** Une cinquantaine d'idées pour commencer ; on garde celles qui plaisent, on supprime les autres. */
val traditionIdeas: List<TraditionIdea> = listOf(
    TraditionIdea("Le lutin farceur", TraditionCategory.HOME, TraditionPeriod.ADVENT),
    TraditionIdea("Décorer le sapin ensemble", TraditionCategory.HOME, TraditionPeriod.ADVENT),
    TraditionIdea("Fabriquer la couronne de l'Avent", TraditionCategory.HOME, TraditionPeriod.ADVENT),
    TraditionIdea("Écrire la lettre au Père Noël", TraditionCategory.FAMILY, TraditionPeriod.ADVENT),
    TraditionIdea("Envoyer des cartes de vœux", TraditionCategory.FAMILY, TraditionPeriod.ADVENT),
    TraditionIdea("Chanter des chants de Noël", TraditionCategory.FAMILY, TraditionPeriod.ADVENT),
    TraditionIdea("Visiter un marché de Noël", TraditionCategory.OUTINGS, TraditionPeriod.ADVENT),
    TraditionIdea("Admirer les illuminations", TraditionCategory.OUTINGS, TraditionPeriod.ADVENT),
    TraditionIdea("Chocolat chaud devant un film de Noël", TraditionCategory.FILMS_BOOKS, TraditionPeriod.ADVENT),
    TraditionIdea("Une histoire de Noël chaque soir", TraditionCategory.FILMS_BOOKS, TraditionPeriod.ADVENT),
    TraditionIdea("Faire des sablés de Noël", TraditionCategory.COOKING, TraditionPeriod.ADVENT),
    TraditionIdea("Préparer une boîte solidaire", TraditionCategory.OTHER, TraditionPeriod.ADVENT),
    TraditionIdea("Pyjamas assortis du réveillon", TraditionCategory.FAMILY, TraditionPeriod.EVE),
    TraditionIdea("Lait et biscuits pour le Père Noël", TraditionCategory.FAMILY, TraditionPeriod.EVE),
    TraditionIdea("Ouvrir un cadeau le soir du réveillon", TraditionCategory.FAMILY, TraditionPeriod.EVE),
    TraditionIdea("Suivre le traîneau du Père Noël", TraditionCategory.FAMILY, TraditionPeriod.EVE),
    TraditionIdea("Petit-déjeuner de Noël en pyjama", TraditionCategory.COOKING, TraditionPeriod.DAY),
    TraditionIdea("Photo de famille devant le sapin", TraditionCategory.FAMILY, TraditionPeriod.DAY),
    TraditionIdea("Promenade du jour de Noël", TraditionCategory.OUTINGS, TraditionPeriod.DAY),
    TraditionIdea("Jeux de société en famille", TraditionCategory.FAMILY, TraditionPeriod.AFTER),
    TraditionIdea("Écrire des mots de remerciement", TraditionCategory.FAMILY, TraditionPeriod.AFTER),
    TraditionIdea("La galette des rois", TraditionCategory.COOKING, TraditionPeriod.AFTER),
    TraditionIdea("Choisir le sapin en famille", TraditionCategory.HOME, TraditionPeriod.ADVENT),
    TraditionIdea("Installer la crèche", TraditionCategory.HOME, TraditionPeriod.ADVENT),
    TraditionIdea("Décorer les fenêtres", TraditionCategory.HOME, TraditionPeriod.ADVENT),
    TraditionIdea("Fabriquer des décorations maison", TraditionCategory.HOME, TraditionPeriod.ADVENT),
    TraditionIdea("Une bougie allumée chaque dimanche de l'Avent", TraditionCategory.HOME, TraditionPeriod.ADVENT),
    TraditionIdea("Accrocher les chaussettes à la cheminée", TraditionCategory.HOME, TraditionPeriod.ADVENT),
    TraditionIdea("Construire une maison en pain d'épices", TraditionCategory.COOKING, TraditionPeriod.ADVENT),
    TraditionIdea("Préparer des bredele", TraditionCategory.COOKING, TraditionPeriod.ADVENT),
    TraditionIdea("Faire des truffes au chocolat à offrir", TraditionCategory.COOKING, TraditionPeriod.ADVENT),
    TraditionIdea("Vin chaud ou chocolat chaud au marché", TraditionCategory.COOKING, TraditionPeriod.ADVENT),
    TraditionIdea("Cuisiner la bûche ensemble", TraditionCategory.COOKING, TraditionPeriod.EVE),
    TraditionIdea("Préparer le repas du réveillon ensemble", TraditionCategory.COOKING, TraditionPeriod.EVE),
    TraditionIdea("Voir un spectacle de Noël", TraditionCategory.OUTINGS, TraditionPeriod.ADVENT),
    TraditionIdea("Rencontrer le Père Noël", TraditionCategory.OUTINGS, TraditionPeriod.ADVENT),
    TraditionIdea("Patiner à la patinoire de Noël", TraditionCategory.OUTINGS, TraditionPeriod.ADVENT),
    TraditionIdea("Concert ou chorale de Noël", TraditionCategory.OUTINGS, TraditionPeriod.ADVENT),
    TraditionIdea("Messe de minuit ou veillée", TraditionCategory.OUTINGS, TraditionPeriod.EVE),
    TraditionIdea("Balade aux flambeaux", TraditionCategory.OUTINGS, TraditionPeriod.AFTER),
    TraditionIdea("Soirée films de Noël en pyjama", TraditionCategory.FILMS_BOOKS, TraditionPeriod.ADVENT),
    TraditionIdea("Lire « La nuit avant Noël »", TraditionCategory.FILMS_BOOKS, TraditionPeriod.EVE),
    TraditionIdea("Regarder un dessin animé de Noël chaque week-end", TraditionCategory.FILMS_BOOKS, TraditionPeriod.ADVENT),
    TraditionIdea("Emballer les cadeaux en musique", TraditionCategory.FAMILY, TraditionPeriod.ADVENT),
    TraditionIdea("Écrire un mot doux dans chaque cadeau", TraditionCategory.FAMILY, TraditionPeriod.EVE),
    TraditionIdea("Un tour de table des meilleurs moments de l'année", TraditionCategory.FAMILY, TraditionPeriod.EVE),
    TraditionIdea("Appeler la famille éloignée", TraditionCategory.FAMILY, TraditionPeriod.DAY),
    TraditionIdea("Ouvrir les cadeaux chacun son tour", TraditionCategory.FAMILY, TraditionPeriod.DAY),
    TraditionIdea("Bataille de boules de neige (s'il neige !)", TraditionCategory.OUTINGS, TraditionPeriod.DAY),
    TraditionIdea("Faire un vœu pour la nouvelle année", TraditionCategory.FAMILY, TraditionPeriod.AFTER),
    TraditionIdea("Donner des jouets à une association", TraditionCategory.OTHER, TraditionPeriod.ADVENT),
    TraditionIdea("Rendre visite à un voisin seul", TraditionCategory.OTHER, TraditionPeriod.ADVENT),
)

/** Idées pas encore dans le catalogue (même nom, accents et majuscules ignorés). */
fun availableTraditionIdeas(existing: List<Tradition>): List<TraditionIdea> {
    val names = existing.map { normalizeForSearch(it.name) }.toSet()
    return traditionIdeas.filter { normalizeForSearch(it.name) !in names }
}

class SaveTraditionUseCase @Inject constructor(
    private val traditions: TraditionRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, draft: TraditionDraft): AppResult<Tradition> {
        if (draft.name.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        val existing = id?.let { traditions.getTradition(it) }
        existing?.photoPath?.takeIf { it != draft.photoPath }?.let { photos.deletePhoto(it) }
        val now = time.now()
        val tradition = Tradition(
            id = existing?.id ?: ids.newId(),
            name = draft.name.trim(),
            photoPath = draft.photoPath,
            category = draft.category,
            period = draft.period,
            personIds = draft.personIds,
            notes = draft.notes.trim(),
            favorite = draft.favorite,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        traditions.saveTradition(tradition)
        return AppResult.Success(tradition)
    }
}

/** Ajoute au catalogue les idées choisies. */
class AddTraditionIdeasUseCase @Inject constructor(private val save: SaveTraditionUseCase) {
    suspend operator fun invoke(ideas: List<TraditionIdea>): Int =
        ideas.count { save(null, TraditionDraft(it.name, it.category, it.period)) is AppResult.Success }
}

class SetTraditionFavoriteUseCase @Inject constructor(private val traditions: TraditionRepository, private val time: TimeProvider) {
    suspend operator fun invoke(id: String, favorite: Boolean): AppResult<Unit> {
        val tradition = traditions.getTradition(id) ?: return AppResult.Failure(AppError.NotFound)
        traditions.saveTradition(tradition.copy(favorite = favorite, updatedAt = time.now()))
        return AppResult.Success(Unit)
    }
}

class DeleteTraditionUseCase @Inject constructor(private val traditions: TraditionRepository, private val photos: PhotoRepository) {
    /** Supprime la tradition, son programme de chaque année et leurs photos. */
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val tradition = traditions.getTradition(id) ?: return AppResult.Failure(AppError.NotFound)
        val memories = traditions.getYearsOf(id).mapNotNull { it.memoryPhotoPath }
        traditions.deleteTradition(id)
        (listOfNotNull(tradition.photoPath) + memories).forEach { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

/** Met une tradition au programme d'un Noël (ou l'en retire). */
class SetTraditionPlannedUseCase @Inject constructor(
    private val traditions: TraditionRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(editionId: String, traditionId: String, planned: Boolean): AppResult<Unit> {
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        val existing = traditions.getYear(editionId, traditionId)
        when {
            planned && existing == null -> traditions.saveYear(
                TraditionYear(ids.newId(), editionId, traditionId, false, null, "", time.now()),
            )
            !planned && existing != null -> {
                traditions.deleteYear(existing.id)
                existing.memoryPhotoPath?.let { photos.deletePhoto(it) }
            }
        }
        return AppResult.Success(Unit)
    }
}

/** Faite cette année, avec un souvenir (photo, petit mot). */
class SaveTraditionMemoryUseCase @Inject constructor(
    private val traditions: TraditionRepository,
    private val photos: PhotoRepository,
    private val guard: EditionGuard,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(editionId: String, traditionId: String, done: Boolean, photoPath: String?, note: String): AppResult<Unit> {
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        val existing = traditions.getYear(editionId, traditionId)
        existing?.memoryPhotoPath?.takeIf { it != photoPath }?.let { photos.deletePhoto(it) }
        val year = (existing ?: TraditionYear(ids.newId(), editionId, traditionId, false, null, "", time.now()))
            .copy(done = done, memoryPhotoPath = photoPath, memoryNote = note.trim())
        traditions.saveYear(year)
        return AppResult.Success(Unit)
    }
}

data class InspirationDraft(
    val title: String,
    val kind: InspirationKind = InspirationKind.FILM,
    val photoPath: String? = null,
    val url: String = "",
    val notes: String = "",
)

class SaveInspirationUseCase @Inject constructor(
    private val traditions: TraditionRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(id: String?, draft: InspirationDraft): AppResult<Inspiration> {
        if (draft.title.isBlank()) return AppResult.Failure(AppError.InvalidInput)
        val existing = id?.let { traditions.getInspiration(it) }
        existing?.photoPath?.takeIf { it != draft.photoPath }?.let { photos.deletePhoto(it) }
        val now = time.now()
        val inspiration = Inspiration(
            existing?.id ?: ids.newId(),
            draft.kind,
            draft.title.trim(),
            draft.photoPath,
            draft.url.trim(),
            draft.notes.trim(),
            existing?.createdAt ?: now,
            now,
        )
        traditions.saveInspiration(inspiration)
        return AppResult.Success(inspiration)
    }
}

class DeleteInspirationUseCase @Inject constructor(private val traditions: TraditionRepository, private val photos: PhotoRepository) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val inspiration = traditions.getInspiration(id) ?: return AppResult.Failure(AppError.NotFound)
        traditions.deleteInspiration(id)
        inspiration.photoPath?.let { photos.deletePhoto(it) }
        return AppResult.Success(Unit)
    }
}

/** Vu / fait pendant ce Noël (chaque année repart à zéro). */
class SetInspirationDoneUseCase @Inject constructor(private val traditions: TraditionRepository, private val guard: EditionGuard) {
    suspend operator fun invoke(editionId: String, inspirationId: String, done: Boolean): AppResult<Unit> {
        guard.check(editionId)?.let { return AppResult.Failure(it) }
        traditions.setDone(editionId, inspirationId, done)
        return AppResult.Success(Unit)
    }
}
