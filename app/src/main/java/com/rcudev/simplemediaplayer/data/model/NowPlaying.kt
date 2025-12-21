package com.rcudev.simplemediaplayer.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response from iTunes Search API
 */
data class ITunesSearchResponse(
    @SerializedName("resultCount")
    val resultCount: Int,

    @SerializedName("results")
    val results: List<ITunesTrack>
)

/**
 * Track information from iTunes
 */
data class ITunesTrack(
    @SerializedName("trackName")
    val trackName: String?,

    @SerializedName("artistName")
    val artistName: String?,

    @SerializedName("artworkUrl100")
    val artworkUrl100: String?,

    @SerializedName("artworkUrl600")
    val artworkUrl600: String?
)

/**
 * Now Playing information with parsed metadata
 */
data class NowPlaying(
    val title: String = "Blur FM",
    val artist: String = "En vivo",
    val artworkUrl: String? = null,
    val rawMetadata: String? = null
) {
    companion object {
        val DEFAULT = NowPlaying()
    }
}

