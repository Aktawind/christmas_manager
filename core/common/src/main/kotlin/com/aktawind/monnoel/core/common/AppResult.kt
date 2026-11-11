package com.aktawind.monnoel.core.common

import com.aktawind.monnoel.core.model.BackupFailureReason

/**
 * Résultat d'une opération métier. Les erreurs sont typées pour être traduites en
 * messages clairs par la couche interface (jamais de trace technique à l'écran).
 */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    /** Une année existe déjà pour ce millésime. */
    data class YearAlreadyExists(val year: Int) : AppError

    /** L'élément n'existe plus. */
    data object NotFound : AppError

    /** L'année est archivée : modification impossible. */
    data object ReadOnly : AppError

    /** Valeur saisie invalide. */
    data object InvalidInput : AppError

    /** Échec d'une opération de sauvegarde / restauration. */
    data class Backup(val reason: BackupFailureReason) : AppError

    /** Erreur inattendue (journalisée). */
    data class Unexpected(val cause: Throwable? = null) : AppError
}

inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) block(value)
    return this
}

inline fun <T> AppResult<T>.onFailure(block: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) block(error)
    return this
}
