package com.rcudev.simplemediaplayer.common

import android.net.Uri

object StreamConfig {
    // Labels and URLs for the 4 stream qualities. Replace the URL placeholders with the real station URLs.
    // Order: Standard, High, Data Saver, Ultra
    val OPTIONS: List<Pair<String, String>> = listOf(
        Pair("Standard", "https://example.com/blurfm_standard.mp3"),
        Pair("High", "https://example.com/blurfm_high.mp3"),
        Pair("Data Saver", "https://example.com/blurfm_datasaver.mp3"),
        Pair("Ultra", "https://example.com/blurfm_ultra.mp3")
    )

    // Default selection: Standard (index 0)
    const val DEFAULT_INDEX: Int = 0

    // Optional: station artwork (can be null)
    val ARTWORK_URI: Uri? = null
}

