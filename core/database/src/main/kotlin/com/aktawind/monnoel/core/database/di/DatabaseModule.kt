package com.aktawind.monnoel.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aktawind.monnoel.core.database.DefaultData
import com.aktawind.monnoel.core.database.MonNoelDatabase
import com.aktawind.monnoel.core.database.dao.EditionDao
import com.aktawind.monnoel.core.database.dao.GiftCategoryDao
import com.aktawind.monnoel.core.database.dao.GiftIdeaDao
import com.aktawind.monnoel.core.database.dao.PersonDao
import com.aktawind.monnoel.core.database.dao.PlannedGiftDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Données initiales d'une base toute neuve. */
val SeedCallback = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultData.insertGiftCategories(db)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MonNoelDatabase =
        Room.databaseBuilder(context, MonNoelDatabase::class.java, MonNoelDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .addCallback(SeedCallback)
            .build()

    @Provides
    fun provideEditionDao(db: MonNoelDatabase): EditionDao = db.editionDao()

    @Provides
    fun providePersonDao(db: MonNoelDatabase): PersonDao = db.personDao()

    @Provides
    fun provideGiftCategoryDao(db: MonNoelDatabase): GiftCategoryDao = db.giftCategoryDao()

    @Provides
    fun provideGiftIdeaDao(db: MonNoelDatabase): GiftIdeaDao = db.giftIdeaDao()

    @Provides
    fun providePlannedGiftDao(db: MonNoelDatabase): PlannedGiftDao = db.plannedGiftDao()
}
