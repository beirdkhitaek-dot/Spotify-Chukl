package com.chukl.spotifychukl

import org.json.JSONObject

/** Latest Spotify track, shared between the listener, the local server and the screen. */
object NowPlaying {
    @Volatile var title: String = ""
    @Volatile var artist: String = ""
    @Volatile var album: String = ""
    @Volatile var playing: Boolean = false
    @Volatile var positionMs: Long = 0
    @Volatile var durationMs: Long = 0
    @Volatile var updatedAt: Long = 0
    @Volatile var spotifyFound: Boolean = false

    fun toJson(): String {
        // Position keeps moving while playing, so work it out from the last update.
        var pos = positionMs
        if (playing && updatedAt > 0) pos += System.currentTimeMillis() - updatedAt
        if (durationMs > 0 && pos > durationMs) pos = durationMs
        return JSONObject()
            .put("app", "Spotify Chukl")
            .put("spotify", spotifyFound)
            .put("playing", playing)
            .put("title", title)
            .put("artist", artist)
            .put("album", album)
            .put("position_ms", pos)
            .put("duration_ms", durationMs)
            .toString()
    }
}
