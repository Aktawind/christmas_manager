package com.aktawind.monnoel.feature.gifts.edit

import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Photos ajoutées pendant une saisie : si l'on quitte sans enregistrer, elles sont
 * supprimées pour ne pas encombrer la tablette.
 */
internal class PhotoDrafts(private val photos: PhotoRepository, private val appScope: CoroutineScope) {
    private val pending = mutableSetOf<String>()

    fun added(path: String) {
        pending += path
    }

    /** La saisie a été enregistrée avec [kept] : les autres photos temporaires sont supprimées. */
    fun committed(kept: String?) {
        pending -= setOfNotNull(kept)
        discard()
    }

    fun discard() {
        val toDelete = pending.toList()
        pending.clear()
        if (toDelete.isNotEmpty()) appScope.launch { toDelete.forEach { photos.deletePhoto(it) } }
    }
}
