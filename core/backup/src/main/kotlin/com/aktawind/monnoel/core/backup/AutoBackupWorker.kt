package com.aktawind.monnoel.core.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backups: BackupRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (backups.runAutoBackupNow()) {
        is AppResult.Success -> Result.success()
        is AppResult.Failure -> if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_RETRIES = 2
    }
}
