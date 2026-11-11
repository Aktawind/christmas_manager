package com.aktawind.monnoel.core.ui

import androidx.annotation.StringRes
import com.aktawind.monnoel.core.common.AppError
import com.aktawind.monnoel.core.model.BackupFailureReason

/** Message compréhensible pour chaque erreur métier (gestion centralisée des erreurs). */
data class UiMessage(@StringRes val resId: Int, val args: List<Any> = emptyList())

fun AppError.toUiMessage(): UiMessage = when (this) {
    is AppError.YearAlreadyExists -> UiMessage(R.string.error_year_exists, listOf(year))
    AppError.NotFound -> UiMessage(R.string.error_not_found)
    AppError.ReadOnly -> UiMessage(R.string.error_read_only)
    AppError.InvalidInput -> UiMessage(R.string.error_invalid_input)
    is AppError.Backup -> reason.toUiMessage()
    is AppError.Unexpected -> UiMessage(R.string.error_unexpected)
}

fun BackupFailureReason.toUiMessage(): UiMessage = UiMessage(
    when (this) {
        BackupFailureReason.FOLDER_UNAVAILABLE -> R.string.error_backup_folder
        BackupFailureReason.WRITE_FAILED -> R.string.error_backup_write
        BackupFailureReason.READ_FAILED -> R.string.error_backup_read
        BackupFailureReason.NOT_A_BACKUP -> R.string.error_backup_not_backup
        BackupFailureReason.CORRUPTED -> R.string.error_backup_corrupted
        BackupFailureReason.TOO_RECENT -> R.string.error_backup_too_recent
        BackupFailureReason.UNKNOWN -> R.string.error_unexpected
    },
)
