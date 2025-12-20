package com.rcudev.simplemediaplayer.di

import android.content.Context
import com.rcudev.simplemediaplayer.common.StreamPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideStreamPreferences(@ApplicationContext context: Context): StreamPreferences =
        StreamPreferences(context)
}

