package com.capsule.music.tracker

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.capsule.music.CapsuleApp
import com.capsule.music.data.PlaybackEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastTrackTitle: String? = null
    private var lastArtist: String? = null
    private var trackStartTime: Long = 0L

    override fun onListenerConnected() {
        super.onListenerConnected()
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

        if (title != lastTrackTitle || artist != lastArtist) {
            // Если предыдущий трек играл хотя бы 25 секунд — сохраняем в базу
            if (lastTrackTitle != null && trackStartTime > 0) {
                val playedDuration = currentTime - trackStartTime
                if (playedDuration >= 25_000) {
                    saveTrackToDb(lastTrackTitle!!, lastArtist ?: "Unknown Artist", playedDuration)
                }
            }

            // Открываем новую сессию
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
}
