package com.aktawind.monnoel.core.backup.di

import com.aktawind.monnoel.core.backup.DefaultBackupRepository
import com.aktawind.monnoel.core.domain.repository.BackupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    abstract fun bindBackupRepository(impl: DefaultBackupRepository): BackupRepository
}
