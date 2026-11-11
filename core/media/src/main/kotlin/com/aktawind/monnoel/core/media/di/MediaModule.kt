package com.aktawind.monnoel.core.media.di

import com.aktawind.monnoel.core.domain.repository.PhotoRepository
import com.aktawind.monnoel.core.media.PhotoStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {
    @Binds
    abstract fun bindPhotos(impl: PhotoStorage): PhotoRepository
}
