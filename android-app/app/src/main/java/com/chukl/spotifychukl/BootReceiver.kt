package com.chukl.spotifychukl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Starts Spotify Chukl again after the phone restarts, so it is already listening when you open Spotify. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) KeepAliveService.start(context)
    }
}
