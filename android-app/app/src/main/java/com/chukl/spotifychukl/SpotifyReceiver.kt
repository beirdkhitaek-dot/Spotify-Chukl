package com.chukl.spotifychukl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Spotify can announce what it plays ("Device Broadcast Status" setting). Listening to that needs no special
 * permission, so Android and Play Protect have nothing scary to block.
 */
class SpotifyReceiver : BroadcastReceiver() {
    private fun num(i: Intent, key: String): Long =
        (i.extras?.get(key) as? Number)?.toLong() ?: 0L

    override fun onReceive(context: Context, i: Intent) {
        val now = System.currentTimeMillis()
        NowPlaying.spotifyFound = true
        NowPlaying.lastEventAt = now
        val newTrack = i.getStringExtra("track")
        if (newTrack != null) {
            if (newTrack != NowPlaying.title) { NowPlaying.positionMs = 0; NowPlaying.updatedAt = now }
            NowPlaying.title = newTrack
            NowPlaying.artist = i.getStringExtra("artist") ?: ""
            NowPlaying.album = i.getStringExtra("album") ?: ""
            val len = num(i, "length")
            if (len > 0) NowPlaying.durationMs = len
        }
        if (i.action == "com.spotify.music.playbackstatechanged") {
            val playing = i.getBooleanExtra("playing", false)
            val sent = num(i, "timeSent")
            val lag = if (sent in 1..now) (now - sent).coerceAtMost(10_000) else 0L
            NowPlaying.playing = playing
            NowPlaying.positionMs = num(i, "playbackPosition") + if (playing) lag else 0L
            NowPlaying.updatedAt = now
        }
    }
}
