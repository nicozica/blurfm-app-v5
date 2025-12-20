package com.rcudev.simplemediaplayer.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rcudev.simplemediaplayer.common.StreamConfig
import com.rcudev.simplemediaplayer.common.ui.Destination
import com.rcudev.simplemediaplayer.common.ui.SimpleMediaViewModel
import com.rcudev.simplemediaplayer.common.ui.UIState
import com.rcudev.simplemediaplayer.common.ui.components.SimpleMediaPlayerUI

@Composable
internal fun SimpleMediaScreen(
    vm: SimpleMediaViewModel,
    navController: NavController,
    startService: () -> Unit,
) {
    val state = vm.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (state.value) {
            UIState.Initial -> CircularProgressIndicator(
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.Center)
            )
            is UIState.Ready -> {
                LaunchedEffect(true) { // This is only call first time
                    startService()
                }

                ReadyContent(vm = vm, navController = navController)
            }
        }

    }
}

@Composable
private fun ReadyContent(
    vm: SimpleMediaViewModel,
    navController: NavController,
) {

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // Quality selector
        Box(modifier = Modifier
            .wrapContentSize(Alignment.TopStart)
            .padding(bottom = 12.dp)) {
            Text(
                text = "Quality: ${vm.selectedQualityLabel}",
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(8.dp)
            )

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                StreamConfig.OPTIONS.forEachIndexed { index, pair ->
                    DropdownMenuItem(text = { Text(pair.first) }, onClick = {
                        expanded = false
                        vm.onQualitySelected(index)
                    })
                }
            }
        }

        SimpleMediaPlayerUI(
            durationString = vm.formatDuration(vm.duration),
            playResourceProvider = {
                if (vm.isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            },
            progressProvider = { Pair(vm.progress, vm.progressString) },
            onUiEvent = vm::onUIEvent,
        )

        FloatingActionButton(
            onClick = { navController.navigate(Destination.Secondary.route) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "Navigate to Secondary",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}