package com.rcudev.simplemediaplayer.common.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.rcudev.player_service.service.PlayerEvent
import com.rcudev.player_service.service.SimpleMediaServiceHandler
import com.rcudev.player_service.service.SimpleMediaState
import com.rcudev.simplemediaplayer.common.StreamConfig
import com.rcudev.simplemediaplayer.common.StreamPreferences
import com.rcudev.simplemediaplayer.data.repository.NowPlayingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class SimpleMediaViewModel @Inject constructor(
    private val simpleMediaServiceHandler: SimpleMediaServiceHandler,
    private val streamPreferences: StreamPreferences,
    private val nowPlayingRepository: NowPlayingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var duration by savedStateHandle.saveable { mutableStateOf(0L) }
    var progress by savedStateHandle.saveable { mutableStateOf(0f) }
    var progressString by savedStateHandle.saveable { mutableStateOf("00:00") }
    var isPlaying by savedStateHandle.saveable { mutableStateOf(false) }

    // Selected quality index persisted in preferences
    var selectedQualityIndex by savedStateHandle.saveable { mutableStateOf(StreamConfig.DEFAULT_INDEX) }

    // Stream metadata (title/artist/artwork from ICY metadata + iTunes)
    var streamTitle by savedStateHandle.saveable { mutableStateOf("Blur FM") }
    var streamArtist by savedStateHandle.saveable { mutableStateOf("En vivo") }

    // Artwork URL doesn't need to persist across process death
    var artworkUrl by mutableStateOf<String?>(null)

    private var nowPlayingRefreshJob: Job? = null
    private var latestRawMetadata: String? = null

    private val _uiState = MutableStateFlow<UIState>(UIState.Initial)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load persisted selection
            selectedQualityIndex = streamPreferences.getSelectedIndex()

            loadData()

            // Listen to player state
            launch {
                simpleMediaServiceHandler.simpleMediaState.collect { mediaState ->
                    when (mediaState) {
                        is SimpleMediaState.Buffering -> calculateProgressValues(mediaState.progress)
                        SimpleMediaState.Initial -> {
                            _uiState.value = UIState.Initial
                        }
                        is SimpleMediaState.Playing -> {
                            isPlaying = mediaState.isPlaying
                        }
                        is SimpleMediaState.Progress -> calculateProgressValues(mediaState.progress)
                        is SimpleMediaState.Ready -> {
                            duration = mediaState.duration
                            _uiState.value = UIState.Ready
                        }
                    }
                }
            }

            // Listen to ICY metadata (Now Playing)
            launch {
                simpleMediaServiceHandler.icyMetadata.collect { rawMetadata ->
                    if (!rawMetadata.isNullOrBlank()) {
                        latestRawMetadata = rawMetadata
                        refreshNowPlaying(rawMetadata)
                    }
                }
            }

            // Always keep the refresh loop running so metadata updates even if not playing
            startNowPlayingRefresh()
        }
    }

    private fun statusUrls(): List<String> = listOf(
        "https://www.blurfm.com/icecast-proxy.php",
        "https://icecast.blurfm.com/status-json.xsl"
    )

    private fun startNowPlayingRefresh() {
        if (nowPlayingRefreshJob?.isActive == true) return
        nowPlayingRefreshJob = viewModelScope.launch {
            while (true) {
                // Prefer remote status JSON to update even if not playing. Try proxy then direct.
                val remote = statusUrls().firstNotNullOfOrNull { url ->
                    nowPlayingRepository.fetchRemoteNowPlaying(url)
                }
                if (remote != null) {
                    streamTitle = remote.title
                    streamArtist = remote.artist
                    artworkUrl = remote.artworkUrl
                    latestRawMetadata = remote.rawMetadata
                } else {
                    latestRawMetadata?.let { refreshNowPlaying(it) }
                }
                // Poll every 5 seconds
                delay(5_000)
            }
        }
    }

    private fun stopNowPlayingRefresh() {
        nowPlayingRefreshJob?.cancel()
        nowPlayingRefreshJob = null
    }

    private suspend fun refreshNowPlaying(rawMetadata: String) {
        val nowPlaying = nowPlayingRepository.processMetadata(rawMetadata)
        streamTitle = nowPlaying.title
        streamArtist = nowPlaying.artist
        artworkUrl = nowPlaying.artworkUrl
    }

    override fun onCleared() {
        stopNowPlayingRefresh()
        viewModelScope.launch {
            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Stop)
        }
    }

    fun onUIEvent(uiEvent: UIEvent) = viewModelScope.launch {
        when (uiEvent) {
            UIEvent.Backward -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Backward)
            UIEvent.Forward -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Forward)
            UIEvent.PlayPause -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.PlayPause)
            is UIEvent.UpdateProgress -> {
                progress = uiEvent.newProgress
                simpleMediaServiceHandler.onPlayerEvent(
                    PlayerEvent.UpdateProgress(
                        uiEvent.newProgress
                    )
                )
            }
        }
    }

    fun formatDuration(duration: Long): String {
        val minutes: Long = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
        val seconds: Long = (TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)
                - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun calculateProgressValues(currentProgress: Long) {
        progress = if (currentProgress > 0) (currentProgress.toFloat() / duration) else 0f
        progressString = formatDuration(currentProgress)
    }

    // Expose selected quality label for UI
    val selectedQualityLabel: String
        get() = StreamConfig.OPTIONS.getOrNull(selectedQualityIndex)?.first ?: "Standard"

    private fun loadData() {
        // Build media item from selected stream
        val (label, url) = StreamConfig.OPTIONS.getOrNull(selectedQualityIndex)
            ?: StreamConfig.OPTIONS[StreamConfig.DEFAULT_INDEX]

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setFolderType(MediaMetadata.FOLDER_TYPE_ALBUMS)
                    .setArtworkUri(StreamConfig.ARTWORK_URI)
                    .setAlbumTitle("Blur FM")
                    .setDisplayTitle("Blur FM — $label")
                    .build()
            ).build()

        simpleMediaServiceHandler.addMediaItem(mediaItem)
    }

    // Called from UI when the user selects a different quality
    fun onQualitySelected(index: Int) = viewModelScope.launch {
        if (index < 0 || index >= StreamConfig.OPTIONS.size) return@launch
        selectedQualityIndex = index
        streamPreferences.setSelectedIndex(index)

        // Clear metadata cache when changing streams
        nowPlayingRepository.clearCache()

        // Reset latest metadata so refresh loop waits for new data
        latestRawMetadata = null

        val (label, url) = StreamConfig.OPTIONS[index]
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setFolderType(MediaMetadata.FOLDER_TYPE_ALBUMS)
                    .setArtworkUri(StreamConfig.ARTWORK_URI)
                    .setAlbumTitle("Blur FM")
                    .setDisplayTitle("Blur FM — $label")
                    .build()
            ).build()

        // Replace current media item and start playback on the new stream
        simpleMediaServiceHandler.replaceMediaItemAndPlay(mediaItem)
    }

}

sealed class UIEvent {
    object PlayPause : UIEvent()
    object Backward : UIEvent()
    object Forward : UIEvent()
    data class UpdateProgress(val newProgress: Float) : UIEvent()
}

sealed class UIState {
    object Initial : UIState()
    object Ready : UIState()
}