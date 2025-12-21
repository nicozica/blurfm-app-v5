package com.rcudev.simplemediaplayer.common.ui

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class SimpleMediaViewModel @Inject constructor(
    private val simpleMediaServiceHandler: SimpleMediaServiceHandler,
    private val streamPreferences: StreamPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var duration by savedStateHandle.saveable { mutableStateOf(0L) }
    var progress by savedStateHandle.saveable { mutableStateOf(0f) }
    var progressString by savedStateHandle.saveable { mutableStateOf("00:00") }
    var isPlaying by savedStateHandle.saveable { mutableStateOf(false) }

    // Selected quality index persisted in preferences
    var selectedQualityIndex by savedStateHandle.saveable { mutableStateOf(StreamConfig.DEFAULT_INDEX) }

    // Stream metadata (title/artist from ICY metadata if available)
    var streamTitle by savedStateHandle.saveable { mutableStateOf("Blur FM") }
    var streamArtist by savedStateHandle.saveable { mutableStateOf("") }

    private val _uiState = MutableStateFlow<UIState>(UIState.Initial)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load persisted selection
            selectedQualityIndex = streamPreferences.getSelectedIndex()

            loadData()

            simpleMediaServiceHandler.simpleMediaState.collect { mediaState ->
                when (mediaState) {
                    is SimpleMediaState.Buffering -> calculateProgressValues(mediaState.progress)
                    SimpleMediaState.Initial -> _uiState.value = UIState.Initial
                    is SimpleMediaState.Playing -> isPlaying = mediaState.isPlaying
                    is SimpleMediaState.Progress -> calculateProgressValues(mediaState.progress)
                    is SimpleMediaState.Ready -> {
                        duration = mediaState.duration
                        _uiState.value = UIState.Ready
                    }
                }
            }
        }
    }

    override fun onCleared() {
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