package com.capsule.music.tracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.capsule.music.CapsuleApp
import com.capsule.music.MainActivity
import com.capsule.music.data.PlaybackEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastTrackTitle: String? = null
    private var lastArtist: String? = null
    private var trackStartTime: Long = 0L

    companion object {
        private const val CHANNEL_ID = "capsule_tracker_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        showStatusNotification("Ожидание музыки... 💤")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        showStatusNotification("Трекер активен 🟢")
        registerMediaControllers()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        registerMediaControllers()
    }

    private fun registerMediaControllers() {
        val sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
        try {
            val controllers = sessionManager.getActiveSessions(
                android.content.ComponentName(this, MusicNotificationListener::class.java)
            )
            for (controller in controllers) {
                controller.registerCallback(object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        handleMediaUpdate(controller)
                    }

                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        handleMediaUpdate(controller)
                    }
                })
                handleMediaUpdate(controller)
            }
        } catch (_: SecurityException) {}
    }

    private fun handleMediaUpdate(controller: MediaController) {
        val metadata = controller.metadata ?: return
        val state = controller.playbackState ?: return

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val isPlaying = state.state == PlaybackState.STATE_PLAYING

        val currentTime = System.currentTimeMillis()

        if (isPlaying) {
            showStatusNotification("🎵 $artist — $title")
        } else {
            showStatusNotification("Пауза: $artist — $title ⏸")
        }

        if (title != lastTrackTitle || artist != lastArtist) {
            // Если предыдущий трек играл более 25 секунд — сохраняем
            if (lastTrackTitle != null && trackStartTime > 0) {
                val playedDuration = currentTime - trackStartTime
                if (playedDuration >= 25_000) {
                    saveTrackToDb(lastTrackTitle!!, lastArtist ?: "Unknown Artist", playedDuration)
                }
            }

            lastTrackTitle = title
            lastArtist = artist
            trackStartTime = if (isPlaying) currentTime else 0L
        } else {
            if (isPlaying && trackStartTime == 0L) {
                trackStartTime = currentTime
            }
        }
    }

    private fun saveTrackToDb(title: String, artist: String, duration: Long) {
        scope.launch {
            CapsuleApp.database.playbackDao().insertEvent(
                PlaybackEvent(
                    trackTitle = title,
                    artistName = artist,
                    durationMs = duration,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // Показ закрепленного сервисного уведомления
    private fun showStatusNotification(statusText: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Music Capsule")
            .setContentText(statusText)
            .setOngoing(true) // Нельзя смахнуть случайно
            .setPriority(NotificationCompat.PRIORITY_LOW) // Тихое, без звукового писка
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Фоновый трекер музыки",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Показывает статус работы трекера Music Capsule"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
