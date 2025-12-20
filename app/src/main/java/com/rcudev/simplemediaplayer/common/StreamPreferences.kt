package com.rcudev.simplemediaplayer.common

import android.content.Context
import javax.inject.Inject

class StreamPreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "blur_fm_prefs"
        private const val KEY_SELECTED_QUALITY = "selected_quality"
    }

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    fun getSelectedIndex(): Int = prefs.getInt(KEY_SELECTED_QUALITY, StreamConfig.DEFAULT_INDEX)

    fun setSelectedIndex(index: Int) {
        prefs.edit().putInt(KEY_SELECTED_QUALITY, index).apply()
    }
}

