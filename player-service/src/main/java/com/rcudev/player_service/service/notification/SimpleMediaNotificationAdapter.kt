package com.rcudev.player_service.service.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager
import com.rcudev.player_service.R

@UnstableApi
class SimpleMediaNotificationAdapter(
    private val context: Context,
    private val pendingIntent: PendingIntent?
) : PlayerNotificationManager.MediaDescriptionAdapter {

    override fun getCurrentContentTitle(player: Player): CharSequence =
        "Blur FM"

    override fun createCurrentContentIntent(player: Player): PendingIntent? =
        pendingIntent

    override fun getCurrentContentText(player: Player): CharSequence {
        // Extract quality from metadata display title or use default
        val displayTitle = player.mediaMetadata.displayTitle?.toString() ?: ""
        return if (displayTitle.contains("—")) {
            displayTitle.substringAfter("—").trim()
        } else {
            "Quality: Standard"
        }
    }

    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        // Use app icon as large icon for notification
        return BitmapFactory.decodeResource(context.resources, R.drawable.ic_microphone)
    }

}
