package com.rcudev.player_service.service

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Wrapper around ExoPlayer that converts pause() into stop() for live radio behavior.
 *
 * For live radio streaming:
 * - pause() should disconnect from stream completely (stop + clear buffer)
 * - play() should reconnect to live stream (not resume buffered content)
 *
 * This wrapper intercepts pause() and converts it to:
 * - stop()
 * - clearMediaItems()
 *
 * When play() is called after pause, the caller must re-add the MediaItem.
 */
@UnstableApi
class LiveStreamPlayer(
    private val exoPlayer: ExoPlayer,
    private val onNeedMediaItem: () -> MediaItem?
) : ForwardingPlayer(exoPlayer) {

    /**
     * Override pause() to behave like "stop live stream".
     * This disconnects completely instead of buffering.
     */
    override fun pause() {
        // Stop playback and clear buffer completely
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        // Update playWhenReady to reflect stopped state
        exoPlayer.playWhenReady = false
    }

    /**
     * Override play() to reconnect to live stream.
     * If no media is loaded, fetch it via callback.
     */
    override fun play() {
        // If no media items (after pause/stop), re-add the current stream
        if (exoPlayer.mediaItemCount == 0) {
            val mediaItem = onNeedMediaItem()
            if (mediaItem != null) {
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }
        }

        // Start playback
        exoPlayer.play()
    }

    /**
     * Get the underlying ExoPlayer instance.
     * Useful for direct access when needed.
     */
    override fun getWrappedPlayer(): ExoPlayer = exoPlayer
}

