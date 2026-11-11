package com.aktawind.monnoel.di

import com.aktawind.monnoel.BuildConfig
import com.aktawind.monnoel.core.common.AppInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideAppInfo(): AppInfo = object : AppInfo {
        override val versionName: String = BuildConfig.VERSION_NAME
        override val versionCode: Long = BuildConfig.VERSION_CODE.toLong()
    }
}
