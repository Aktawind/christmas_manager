package com.aktawind.monnoel.core.domain.gifts

import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.repository.LinkPreviewRepository
import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Prépare une idée reçue par le partage Android : lit la page (titre, photo, prix, boutique)
 * quand c'est possible. Sans réseau ou si la page refuse, on garde simplement le texte partagé.
 */
class PrepareSharedIdeaUseCase @Inject constructor(
    private val previews: LinkPreviewRepository,
    private val photos: PhotoRepository,
) {
    suspend operator fun invoke(shared: SharedIdea): SharedIdea {
        if (shared.url.isBlank()) return shared
        val preview = withTimeoutOrNull(TIMEOUT_MS) { previews.fetch(shared.url) } ?: return shared
        val photo = preview.imageUrl?.let { imageUrl ->
            withTimeoutOrNull(TIMEOUT_MS) {
                previews.downloadImage(imageUrl)?.let { local ->
                    (photos.importPhoto(local) as? AppResult.Success)?.value
                }
            }
        }
        return shared.copy(
            title = preview.title ?: shared.title,
            priceCents = preview.priceCents ?: shared.priceCents,
            shop = preview.siteName ?: shared.shop,
            photoPath = photo ?: shared.photoPath,
        )
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
