package com.aktawind.monnoel.core.domain.gifts

/** Idée reçue par le partage Android (titre et lien proposés). */
data class SharedIdea(
    val title: String,
    val url: String,
    val priceCents: Long? = null,
    val shop: String = "",
    /** Photo déjà importée dans l'application (chemin relatif), si la page en proposait une. */
    val photoPath: String? = null,
    /** Ce qui n'a pas pu être récupéré depuis la page, pour l'expliquer simplement. */
    val previewIssue: PreviewIssue? = null,
    /** Code de réponse du site quand il a refusé (ex. 403). */
    val previewCode: Int? = null,
)

enum class PreviewIssue {
    /** Le site a refusé de donner la page (protection anti-robots…). */
    PAGE_REFUSED,

    /** Pas de connexion, ou le site ne répond pas. */
    PAGE_UNREACHABLE,

    /** La page ne propose pas d'image. */
    IMAGE_NOT_FOUND,

    /** Une image existe mais n'a pas pu être téléchargée ou lue. */
    IMAGE_NOT_LOADED,
}

private val urlRegex = Regex("""https?://\S+""")

/**
 * Extrait un titre et un lien d'un texte partagé par un navigateur ou une application de
 * boutique (ex. « Super jouet https://boutique.fr/jouet »).
 */
fun parseSharedText(text: String?, subject: String?): SharedIdea? {
    val content = text.orEmpty().trim()
    val url = urlRegex.find(content)?.value?.trimEnd('.', ',', ')', ';').orEmpty()
    val rest = content.replace(urlRegex, " ").replace(Regex("\\s+"), " ").trim().trim('-', '–', ':', '|').trim()
    val title = subject?.trim()?.takeIf { it.isNotBlank() } ?: rest
    if (title.isBlank() && url.isBlank()) return null
    return SharedIdea(title = title.take(120), url = url)
}
