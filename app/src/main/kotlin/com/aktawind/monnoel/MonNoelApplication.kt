package com.aktawind.monnoel

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aktawind.monnoel.core.backup.DefaultBackupRepository
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.domain.tasks.LutinAutoSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class MonNoelApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var backups: DefaultBackupRepository

    @Inject lateinit var lutinAutoSync: LutinAutoSync

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        lutinAutoSync.start()
        appScope.launch { runCatching { backups.ensureScheduled() } }
    }
}
