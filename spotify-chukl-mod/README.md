# Spotify Chukl v1.8.1 (Fabric 1.21.11)

Shows the song you are playing on Spotify as a HUD in Minecraft. Works on Android (Zalith) and desktop.

## What's new in 1.8.1
- Setup with the app is now simpler: turn on "Device Broadcast Status" in Spotify. No special Android permission needed.

## What's new in 1.8.0
- Renamed from "Last.fm HUD" to **Spotify Chukl**.
- Works with the **Spotify Chukl Android app**: exact song, exact time bar, instant pause/seek, perfectly synced lyrics.
- Last.fm still works as a backup if the app is not running. Your old settings are copied over automatically.

## How to use
1. Install the Spotify Chukl app and open it.
2. In Spotify turn on Settings > Apps and devices > Device Broadcast Status.
3. Play a song, then start Minecraft.
4. The HUD appears by itself. Open chat and drag it to move it. Press Right Shift for settings.

Commands: `/spotifychukl toggle`, `style <name>`, `lyrics`, `pos <x> <y>`, `scale <0.5-4>`, `status`.
Optional Last.fm backup: `/spotifychukl key <key>` and `/spotifychukl user <name>`.
Config file: `config/spotifychukl.json`.
