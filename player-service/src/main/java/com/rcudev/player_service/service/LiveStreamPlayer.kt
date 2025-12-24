package com.rcudev.player_service.service

import android.util.Log
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
    private var onNeedMediaItem: () -> MediaItem?
) : ForwardingPlayer(exoPlayer) {

    private val TAG = "LiveStreamPlayer"

    /**
     * Set the callback for retrieving media item when needed
     */
    fun setMediaItemCallback(callback: () -> MediaItem?) {
        onNeedMediaItem = callback
    }

    /**
     * Override pause() to behave like "stop live stream".
     * This disconnects completely instead of buffering.
     */
    override fun pause() {
        Log.d(TAG, "Pause called - stopping and clearing buffer for live stream")
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
            Log.d(TAG, "No media items after pause - fetching and reconnecting to live")
            val mediaItem = onNeedMediaItem()
            if (mediaItem != null) {
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            } else {
                Log.w(TAG, "Cannot play - no media item available")
                return
            }
        } else if (exoPlayer.playbackState == Player.STATE_IDLE) {
            Log.d(TAG, "Media item exists but not prepared - preparing to connect to live")
            exoPlayer.prepare()
        } else {
            Log.d(TAG, "Resuming playback (media already prepared)")
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
