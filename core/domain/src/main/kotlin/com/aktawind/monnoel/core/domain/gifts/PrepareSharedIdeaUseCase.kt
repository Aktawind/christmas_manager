package com.aktawind.monnoel.core.domain.gifts

import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.repository.LinkPreviewRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull

/** Résultat de la lecture d'une page web. */
sealed interface PageFetch {
    data class Success(val preview: LinkPreview) : PageFetch

    /** Le site a répondu, mais en refusant (code HTTP hors 2xx). */
    data class Refused(val httpCode: Int) : PageFetch

    data object Unreachable : PageFetch
}

/**
 * Prépare une idée reçue par le partage Android : lit la page (titre, photo, prix, boutique)
 * quand c'est possible. Sans réseau ou si la page refuse, on garde le texte partagé et on
 * indique simplement ce qui n'a pas pu être récupéré.
 */
class PrepareSharedIdeaUseCase @Inject constructor(
    private val previews: LinkPreviewRepository,
    private val photos: PhotoRepository,
) {
    suspend operator fun invoke(shared: SharedIdea): SharedIdea {
        if (shared.url.isBlank()) return shared
        val preview = when (val fetch = withTimeoutOrNull(TIMEOUT_MS) { previews.fetch(shared.url) }) {
            is PageFetch.Success -> fetch.preview
            is PageFetch.Refused -> return shared.copy(
                previewIssue = PreviewIssue.PAGE_REFUSED,
                previewCode = fetch.httpCode,
            )
            PageFetch.Unreachable, null -> return shared.copy(previewIssue = PreviewIssue.PAGE_UNREACHABLE)
        }
        val photo = preview.imageUrl?.let { imageUrl ->
            withTimeoutOrNull(TIMEOUT_MS) {
                previews.downloadImage(imageUrl, referer = shared.url)?.let { local ->
                    (photos.importPhoto(local) as? AppResult.Success)?.value
                }
            }
        }
        return shared.copy(
            title = preview.title ?: shared.title,
            priceCents = preview.priceCents ?: shared.priceCents,
            shop = preview.siteName ?: shared.shop,
            photoPath = photo ?: shared.photoPath,
            previewIssue = when {
                photo != null -> null
                preview.imageUrl == null -> PreviewIssue.IMAGE_NOT_FOUND
                else -> PreviewIssue.IMAGE_NOT_LOADED
            },
        )
    }

    private companion object {
        const val TIMEOUT_MS = 12_000L
    }
}
