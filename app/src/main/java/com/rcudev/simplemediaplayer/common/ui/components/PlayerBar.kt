package com.rcudev.simplemediaplayer.common.ui.components

import androidx.compose.runtime.Composable
import com.rcudev.simplemediaplayer.common.ui.UIEvent

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun PlayerBar(
    progress: Float,
    durationString: String,
    progressString: String,
    onUiEvent: (UIEvent) -> Unit
) {
    // For live radio, we don't show seek controls
    // This component is kept for potential future use
}