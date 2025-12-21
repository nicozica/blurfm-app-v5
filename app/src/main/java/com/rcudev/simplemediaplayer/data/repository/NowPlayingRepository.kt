package com.rcudev.simplemediaplayer.data.repository

import android.util.Log
import com.rcudev.simplemediaplayer.data.api.ITunesApi
import com.rcudev.simplemediaplayer.data.model.NowPlaying
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching and caching Now Playing information
 * from ICY metadata + iTunes API
 */
@Singleton
class NowPlayingRepository @Inject constructor(
    private val iTunesApi: ITunesApi
) {
    private val TAG = "NowPlayingRepository"

    // Cache to avoid spamming iTunes API
    private var lastQuery: String? = null
    private var cachedNowPlaying: NowPlaying? = null

    /**
     * Process ICY metadata and fetch track info from iTunes
     *
     * @param rawMetadata Raw string from ICY (e.g., "Artist - Track")
     * @return NowPlaying with iTunes data or fallback
     */
    suspend fun processMetadata(rawMetadata: String?): NowPlaying {
        if (rawMetadata.isNullOrBlank()) {
            return NowPlaying.DEFAULT
        }

        // Check cache
        if (rawMetadata == lastQuery && cachedNowPlaying != null) {
            Log.d(TAG, "Returning cached result for: $rawMetadata")
            return cachedNowPlaying!!
        }

        // Parse artist and track from "ARTIST - TRACK" format
        val (artist, track) = parseMetadata(rawMetadata)

        if (artist.isEmpty() || track.isEmpty()) {
            Log.w(TAG, "Could not parse metadata: $rawMetadata")
            return NowPlaying(
                title = rawMetadata,
                artist = "Blur FM",
                rawMetadata = rawMetadata
            )
        }

        // Fetch from iTunes
        val nowPlaying = fetchFromITunes(artist, track, rawMetadata)

        // Update cache
        lastQuery = rawMetadata
        cachedNowPlaying = nowPlaying

        return nowPlaying
    }

    /**
     * Parse "ARTIST - TRACK" format
     * Handles various separators: " - ", " – ", " — "
     */
    private fun parseMetadata(metadata: String): Pair<String, String> {
        val separators = listOf(" - ", " – ", " — ", " -")

        for (separator in separators) {
            if (metadata.contains(separator)) {
                val parts = metadata.split(separator, limit = 2)
                if (parts.size == 2) {
                    return Pair(parts[0].trim(), parts[1].trim())
                }
            }
        }

        return Pair("", "")
    }

    /**
     * Fetch track info from iTunes Search API
     */
    private suspend fun fetchFromITunes(
        artist: String,
        track: String,
        rawMetadata: String
    ): NowPlaying = withContext(Dispatchers.IO) {
        try {
            val searchTerm = "$artist $track"
            Log.d(TAG, "Fetching from iTunes: $searchTerm")

            val response = iTunesApi.searchTrack(term = searchTerm)

            if (response.resultCount > 0) {
                val result = response.results[0]
                Log.d(TAG, "Found track: ${result.trackName} by ${result.artistName}")

                NowPlaying(
                    title = result.trackName ?: track,
                    artist = result.artistName ?: artist,
                    artworkUrl = result.artworkUrl600 ?: result.artworkUrl100,
                    rawMetadata = rawMetadata
                )
            } else {
                Log.w(TAG, "No results from iTunes for: $searchTerm")
                NowPlaying(
                    title = track,
                    artist = artist,
                    artworkUrl = null,
                    rawMetadata = rawMetadata
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from iTunes: ${e.message}", e)
            NowPlaying(
                title = track,
                artist = artist,
                artworkUrl = null,
                rawMetadata = rawMetadata
            )
        }
    }

    /**
     * Clear cache (useful when user changes quality/stream)
     */
    fun clearCache() {
        lastQuery = null
        cachedNowPlaying = null
    }
}

