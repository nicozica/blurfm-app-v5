package com.rcudev.player_service.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.rcudev.player_service.service.LiveStreamPlayer
import com.rcudev.player_service.service.SimpleMediaServiceHandler
import com.rcudev.player_service.service.notification.SimpleMediaNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SimpleMediaModule {

    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

    @Provides
    @Singleton
    @UnstableApi
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes
    ): ExoPlayer =
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setTrackSelector(DefaultTrackSelector(context))
            .build()

    @Provides
    @Singleton
    @UnstableApi
    fun provideLiveStreamPlayer(
        exoPlayer: ExoPlayer
    ): LiveStreamPlayer =
        LiveStreamPlayer(
            exoPlayer = exoPlayer,
            onNeedMediaItem = { null } // Will be set later by the handler
        )

    @Provides
    @Singleton
    @UnstableApi
    fun provideServiceHandler(
        liveStreamPlayer: LiveStreamPlayer
    ): SimpleMediaServiceHandler =
        SimpleMediaServiceHandler(
            player = liveStreamPlayer
        )

    @Provides
    @Singleton
    @UnstableApi
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        liveStreamPlayer: LiveStreamPlayer
    ): SimpleMediaNotificationManager =
        SimpleMediaNotificationManager(
            context = context,
            player = liveStreamPlayer.getWrappedPlayer()
        )

    @Provides
    @Singleton
    @UnstableApi
    fun provideMediaSession(
        @ApplicationContext context: Context,
        liveStreamPlayer: LiveStreamPlayer
    ): MediaSession =
        MediaSession.Builder(context, liveStreamPlayer).build()
}