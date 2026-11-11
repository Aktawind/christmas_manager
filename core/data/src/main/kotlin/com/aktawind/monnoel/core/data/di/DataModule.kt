package com.aktawind.monnoel.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aktawind.monnoel.core.common.ApplicationScope
import com.aktawind.monnoel.core.common.IdGenerator
import com.aktawind.monnoel.core.common.IoDispatcher
import com.aktawind.monnoel.core.common.SystemTimeProvider
import com.aktawind.monnoel.core.common.TimeProvider
import com.aktawind.monnoel.core.common.UuidGenerator
import com.aktawind.monnoel.core.data.repository.DataStoreUserPreferences
import com.aktawind.monnoel.core.data.repository.RoomEditionRepository
import com.aktawind.monnoel.core.domain.repository.EditionRepository
import com.aktawind.monnoel.core.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds
    abstract fun bindEditionRepository(impl: RoomEditionRepository): EditionRepository

    @Binds
    @Singleton
    abstract fun bindPreferences(impl: DataStoreUserPreferences): UserPreferencesRepository

    @Binds
    abstract fun bindTime(impl: SystemTimeProvider): TimeProvider

    @Binds
    abstract fun bindIds(impl: UuidGenerator): IdGenerator
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {
    @Provides
    @Singleton
    fun providePreferencesStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("user_preferences") }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
