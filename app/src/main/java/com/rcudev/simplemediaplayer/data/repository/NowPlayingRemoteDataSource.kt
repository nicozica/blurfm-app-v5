package com.rcudev.simplemediaplayer.data.repository

import com.rcudev.simplemediaplayer.data.api.BlurFmApi
import com.rcudev.simplemediaplayer.data.model.IceSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches now-playing metadata directly from Icecast status JSON or the provided proxy.
 */
@Singleton
class NowPlayingRemoteDataSource @Inject constructor(
    private val blurFmApi: BlurFmApi
) {
    suspend fun fetchCurrentSource(statusUrl: String): IceSource? {
        return try {
            val status = blurFmApi.fetchStatus(statusUrl)
            status.icestats?.source?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
