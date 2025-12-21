package com.rcudev.simplemediaplayer.data.api

import com.rcudev.simplemediaplayer.data.model.ITunesSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * iTunes Search API
 * Documentation: https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/
 */
interface ITunesApi {

    @GET("search")
    suspend fun searchTrack(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 1
    ): ITunesSearchResponse
}

