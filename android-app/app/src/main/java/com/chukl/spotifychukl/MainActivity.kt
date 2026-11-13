package com.chukl.spotifychukl

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var song: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { render(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }
        val title = TextView(this).apply { text = "Spotify Chukl"; textSize = 28f; gravity = Gravity.CENTER }
        status = TextView(this).apply { textSize = 16f; gravity = Gravity.CENTER; setPadding(0, 40, 0, 20) }
        song = TextView(this).apply { textSize = 20f; gravity = Gravity.CENTER; setPadding(0, 20, 0, 40) }
        val button = Button(this).apply {
            text = "Step 1: Allow access"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        val help = TextView(this).apply {
            text = "Turn on Spotify Chukl in the list, then play a song in Spotify. " +
                "Your HUD reads it from http://127.0.0.1:${Server.PORT}"
            textSize = 13f; gravity = Gravity.CENTER; setPadding(0, 40, 0, 0)
        }
        listOf(title, status, song, button, help).forEach { root.addView(it) }
        setContentView(root)
        KeepAliveService.start(this)
    }

    override fun onResume() { super.onResume(); handler.post(tick) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(tick) }

    private fun hasAccess(): Boolean =
        Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true

    private fun render() {
        status.text = when {
            !hasAccess() -> "Not allowed yet. Tap the button below."
            !NowPlaying.spotifyFound -> "Ready. Open Spotify and play a song."
            NowPlaying.playing -> "Playing"
            else -> "Paused"
        }
        song.text = if (NowPlaying.title.isEmpty()) "" else "${NowPlaying.title}\n${NowPlaying.artist}"
    }
}
