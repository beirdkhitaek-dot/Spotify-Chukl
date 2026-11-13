package com.chukl.spotifychukl

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/** Keeps the app alive while you play Minecraft. */
class KeepAliveService : Service() {
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
        Server.start()
        return START_STICKY
    }

    companion object {
        fun start(ctx: Context) {
            try { ctx.startForegroundService(Intent(ctx, KeepAliveService::class.java)) } catch (_: Exception) {}
        }
    }
}
