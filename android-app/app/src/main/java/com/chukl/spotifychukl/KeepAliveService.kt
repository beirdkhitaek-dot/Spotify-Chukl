package com.chukl.spotifychukl

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder

/** Keeps the app alive while you play Minecraft, listens to Spotify and runs the local HUD server. */
class KeepAliveService : Service() {
    private var receiver: SpotifyReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("chukl", "Spotify Chukl", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "chukl")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Spotify Chukl is running")
            .setContentText("Sending your song to the HUD")
            .build()
        startForeground(1, n)
        if (receiver == null) {
            val r = SpotifyReceiver()
            val f = IntentFilter().apply {
                addAction("com.spotify.music.metadatachanged")
                addAction("com.spotify.music.playbackstatechanged")
                addAction("com.spotify.music.queuechanged")
            }
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(r, f, Context.RECEIVER_EXPORTED) else registerReceiver(r, f)
            receiver = r
        }
        Server.start()
        return START_STICKY
    }

    override fun onDestroy() {
        receiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    companion object {
        fun start(ctx: Context) {
            try { ctx.startForegroundService(Intent(ctx, KeepAliveService::class.java)) } catch (_: Exception) {}
        }
    }
}
