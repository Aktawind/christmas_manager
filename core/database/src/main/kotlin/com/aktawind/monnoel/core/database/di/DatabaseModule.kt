package com.aktawind.monnoel.core.database.di

import android.content.Context
import androidx.room.Room
import com.aktawind.monnoel.core.database.MonNoelDatabase
import com.aktawind.monnoel.core.database.dao.EditionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MonNoelDatabase =
        Room.databaseBuilder(context, MonNoelDatabase::class.java, MonNoelDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides
    fun provideEditionDao(db: MonNoelDatabase): EditionDao = db.editionDao()
}
