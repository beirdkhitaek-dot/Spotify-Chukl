package com.chukl.spotifychukl

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

/**
 * Android only lets an app see other apps' media sessions if it has notification access.
 * That is the whole trick: no Spotify login, no API keys.
 */
class TrackListener : NotificationListenerService() {

    private var manager: MediaSessionManager? = null
    private var controller: MediaController? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        attach(list)
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onSessionDestroyed() { NowPlaying.spotifyFound = false; NowPlaying.playing = false }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        KeepAliveService.start(this)
        val mgr = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        manager = mgr
        val me = ComponentName(this, TrackListener::class.java)
        try {
            mgr.addOnActiveSessionsChangedListener(sessionsListener, me)
            attach(mgr.getActiveSessions(me))
        } catch (_: SecurityException) {
        }
    }

    override fun onListenerDisconnected() {
        manager?.removeOnActiveSessionsChangedListener(sessionsListener)
        controller?.unregisterCallback(callback)
        controller = null
        super.onListenerDisconnected()
    }

    private fun attach(list: List<MediaController>?) {
        val spotify = list?.firstOrNull { it.packageName == "com.spotify.music" }
        if (spotify?.sessionToken == controller?.sessionToken && spotify != null) return
        controller?.unregisterCallback(callback)
        controller = spotify
        if (spotify == null) {
            NowPlaying.spotifyFound = false
            NowPlaying.playing = false
            return
        }
        spotify.registerCallback(callback)
        refresh()
    }

    private fun refresh() {
        val c = controller ?: return
        val meta = c.metadata
        val state = c.playbackState
        NowPlaying.spotifyFound = true
        NowPlaying.title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        NowPlaying.artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        NowPlaying.album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        NowPlaying.durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0
        NowPlaying.playing = state?.state == PlaybackState.STATE_PLAYING
        NowPlaying.positionMs = state?.position ?: 0
        NowPlaying.updatedAt = System.currentTimeMillis()
    }
}
