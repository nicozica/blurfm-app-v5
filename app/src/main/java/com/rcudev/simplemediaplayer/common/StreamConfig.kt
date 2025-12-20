package com.rcudev.simplemediaplayer.common

import android.net.Uri

object StreamConfig {
    // Blur FM stream URLs with quality options
    // Standard: 128 kbps MP3 (main stream)
    // High: 320 kbps MP3 (high quality)
    // Data Saver: 64 kbps MP3 (low bandwidth)
    // Ultra: 32 kbps AAC+ (ultra light)
    // All URLs redirect (302) to icecast.blurfm.com for server flexibility
    val OPTIONS: List<Pair<String, String>> = listOf(
        Pair("Standard", "https://stream.blurfm.com/standard"),
        Pair("High", "https://stream.blurfm.com/high"),
        Pair("Data Saver", "https://stream.blurfm.com/datasaver"),
        Pair("Ultra", "https://stream.blurfm.com/ultra")
    )

    // Default selection: Standard (128 kbps)
    const val DEFAULT_INDEX: Int = 0

    // Optional: station artwork (can be null)
    val ARTWORK_URI: Uri? = null
}

