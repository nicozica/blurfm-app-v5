package com.rcudev.simplemediaplayer.data.api

import com.rcudev.simplemediaplayer.data.model.IcecastStatus
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * API for fetching Icecast status JSON (station metadata) via direct or proxied URL.
 */
interface BlurFmApi {
    @GET
    suspend fun fetchStatus(@Url url: String): IcecastStatus
}
