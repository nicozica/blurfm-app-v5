package com.rcudev.simplemediaplayer.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.rcudev.simplemediaplayer.R
import com.rcudev.simplemediaplayer.common.StreamConfig
import com.rcudev.simplemediaplayer.common.ui.SimpleMediaViewModel
import com.rcudev.simplemediaplayer.common.ui.UIEvent
import com.rcudev.simplemediaplayer.common.ui.UIState

@Composable
internal fun SimpleMediaScreen(
    vm: SimpleMediaViewModel,
    navController: NavController,
    startService: () -> Unit,
) {
    val state = vm.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (state.value) {
            UIState.Initial -> {
                // Gradient background even while loading
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A237E),
                                    Color(0xFF283593),
                                    Color(0xFF1E88E5)
                                )
                            )
                        )
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center),
                    color = Color.White
                )
            }
            is UIState.Ready -> {
                LaunchedEffect(true) {
                    startService()
                }
                BlurFMRadioScreen(vm = vm)
            }
        }
    }
}

@Composable
private fun BlurFMRadioScreen(vm: SimpleMediaViewModel) {
    var showQualityDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF283593),
                        Color(0xFF1E88E5)
                    )
                )
            )
    ) {
        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Logo + Quality button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(48.dp))

                // Logo centered
                Image(
                    painter = painterResource(R.drawable.blur_fm_logo),
                    contentDescription = "Blur FM Logo",
                    modifier = Modifier
                        .height(60.dp)
                        .weight(1f),
                    contentScale = ContentScale.Fit
                )

                // Quality/Settings button
                IconButton(
                    onClick = { showQualityDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Quality Settings",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Center: Cover Art
            Card(
                modifier = Modifier
                    .size(280.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                // Use AsyncImage from Coil to load artwork from iTunes
                // Falls back to local drawable if URL is null or fails to load
                AsyncImage(
                    model = vm.artworkUrl ?: R.drawable.blur_fm_cover,
                    contentDescription = "Album Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.blur_fm_cover),
                    error = painterResource(R.drawable.blur_fm_cover)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metadata: Title / Artist
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = vm.streamTitle,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                if (vm.streamArtist.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = vm.streamArtist,
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Play / Stop Button
            FloatingActionButton(
                onClick = { vm.onUIEvent(UIEvent.PlayPause) },
                modifier = Modifier.size(80.dp),
                containerColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(
                        if (vm.isPlaying) android.R.drawable.ic_media_pause
                        else android.R.drawable.ic_media_play
                    ),
                    contentDescription = if (vm.isPlaying) "Stop" else "Play",
                    tint = Color(0xFF1E88E5),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quality label
            Text(
                text = "Quality: ${vm.selectedQualityLabel}",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Quality selector dialog
    if (showQualityDialog) {
        QualityDialog(
            currentQuality = vm.selectedQualityIndex,
            onDismiss = { showQualityDialog = false },
            onQualitySelected = { index ->
                vm.onQualitySelected(index)
                showQualityDialog = false
            }
        )
    }
}

@Composable
private fun QualityDialog(
    currentQuality: Int,
    onDismiss: () -> Unit,
    onQualitySelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stream Quality") },
        text = {
            Column {
                StreamConfig.OPTIONS.forEachIndexed { index, (label, _) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(index) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == currentQuality,
                            onClick = { onQualitySelected(index) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}