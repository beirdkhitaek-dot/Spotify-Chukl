package com.chukl.spotifychukl

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Soft green "liquid glass" look: frosted translucent cards over a calm gradient. */
class MainActivity : Activity() {
    private val ink = Color.parseColor("#0E3B2B")
    private val inkSoft = Color.parseColor("#4F7A68")
    private val accent = Color.parseColor("#1FA971")

    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var chip: TextView
    private lateinit var timeLeft: TextView
    private lateinit var timeRight: TextView
    private lateinit var bar: GlassBar
    private lateinit var step1State: TextView
    private lateinit var step3State: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { render(); handler.postDelayed(this, 500) }
    }

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun glass(radius: Int = 28, fill: String = "#59FFFFFF"): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(radius).toFloat()
        setColor(Color.parseColor(fill))
        setStroke(dp(1), Color.parseColor("#B3FFFFFF"))
    }

    private fun text(s: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); setTextColor(color)
        if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun card(vararg views: View): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = glass()
        elevation = dp(6).toFloat()
        setPadding(dp(22), dp(20), dp(22), dp(20))
        views.forEach { addView(it) }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(16) }
    }

    private fun pill(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor("#2BC48A"), Color.parseColor("#14946B")))
            .apply { cornerRadius = dp(26).toFloat() }
        setPadding(dp(20), dp(14), dp(20), dp(14))
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(14) }
    }

    /** Frosted progress bar. */
    inner class GlassBar(ctx: android.content.Context) : View(ctx) {
        var fraction = 0f
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c: Canvas) {
            val h = height.toFloat(); val r = h / 2
            p.color = Color.parseColor("#4DFFFFFF"); c.drawRoundRect(RectF(0f, 0f, width.toFloat(), h), r, r, p)
            p.color = accent
            val w = (width * fraction).coerceAtLeast(h)
            if (fraction > 0f) c.drawRoundRect(RectF(0f, 0f, w, h), r, r, p)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#CFF4E1"), Color.parseColor("#F1FBF6"), Color.parseColor("#A9E3C9")))
        }
        // soft glowing blobs behind the glass give it depth
        fun blob(size: Int, color: String, gx: Int, gy: Int) = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(color)) }
            layoutParams = FrameLayout.LayoutParams(dp(size), dp(size), gx or gy)
        }
        root.addView(blob(260, "#6638D39B", Gravity.START, Gravity.TOP).apply { translationX = -dp(90).toFloat(); translationY = dp(40).toFloat() })
        root.addView(blob(300, "#66FFFFFF", Gravity.END, Gravity.CENTER_VERTICAL).apply { translationX = dp(110).toFloat() })
        root.addView(blob(240, "#5578D8B0", Gravity.START, Gravity.BOTTOM).apply { translationX = -dp(60).toFloat(); translationY = dp(60).toFloat() })

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(56), dp(20), dp(32))
        }
        col.addView(text("Spotify Chukl", 32f, ink, true))
        col.addView(text("Shows your song in the Minecraft HUD", 15f, inkSoft).apply { setPadding(0, dp(4), 0, 0) })

        chip = text("", 13f, accent, true)
        songTitle = text("", 24f, ink, true).apply { setPadding(0, dp(8), 0, 0); maxLines = 2 }
        songArtist = text("", 16f, inkSoft).apply { setPadding(0, dp(2), 0, dp(14)); maxLines = 1 }
        bar = GlassBar(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)) }
        timeLeft = text("0:00", 12f, inkSoft)
        timeRight = text("0:00", 12f, inkSoft).apply { gravity = Gravity.END }
        val times = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, 0)
            addView(timeLeft, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(timeRight, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        col.addView(card(chip, songTitle, songArtist, bar, times))

        step1State = text("", 13f, accent, true)
        step3State = text("", 13f, accent, true)
        col.addView(card(
            text("Setup", 20f, ink, true),
            text("1  Turn on sharing in Spotify", 16f, ink, true).apply { setPadding(0, dp(14), 0, 0) },
            text("Spotify  >  Settings  >  Apps and devices  >  Device Broadcast Status  >  On", 14f, inkSoft).apply { setPadding(0, dp(4), 0, 0) },
            step1State.apply { setPadding(0, dp(6), 0, 0) },
            pill("Open Spotify") { openSpotify() },
            text("2  Play a song, then start Minecraft", 16f, ink, true).apply { setPadding(0, dp(18), 0, 0) },
            text("The HUD appears by itself.", 14f, inkSoft).apply { setPadding(0, dp(4), 0, 0) },
            text("3  Keep it running (important)", 16f, ink, true).apply { setPadding(0, dp(18), 0, 0) },
            text("Phones stop background apps while you play. If new songs stop showing, this is why. Allow it below.", 14f, inkSoft).apply { setPadding(0, dp(4), 0, 0) },
            step3State.apply { setPadding(0, dp(6), 0, 0) },
            pill("Allow background running") { allowBackground() },
            pill("Open app settings") { openAppSettings() }
        ))

        root.addView(ScrollView(this).apply { isFillViewport = true; addView(col) })
        setContentView(root)
        KeepAliveService.start(this)
    }

    private fun ignoringBattery(): Boolean =
        (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)

    private fun allowBackground() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    /** Phone makers hide extra switches here (Auto-start, Battery: No restrictions). */
    private fun openAppSettings() {
        startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun openSpotify() {
        val i = packageManager.getLaunchIntentForPackage("com.spotify.music")
        if (i != null) startActivity(i) else Toast.makeText(this, "Spotify is not installed", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() { super.onResume(); KeepAliveService.start(this); handler.post(tick) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(tick) }

    private fun fmt(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }

    private fun render() {
        val found = NowPlaying.spotifyFound
        val ago = if (NowPlaying.lastEventAt > 0) (System.currentTimeMillis() - NowPlaying.lastEventAt) / 1000 else -1
        step1State.text = when {
            !found -> "Waiting for Spotify... play a song after turning it on."
            ago in 0..59 -> "Done. Spotify is sharing (last update ${ago}s ago)."
            else -> "Done. Spotify is sharing (last update ${ago / 60} min ago)."
        }
        step3State.text = if (ignoringBattery()) "Done. Background running is allowed." else "Not allowed yet. Tap the button below."
        chip.text = when {
            !found -> "NOT CONNECTED YET"
            NowPlaying.playing -> "PLAYING"
            else -> "PAUSED"
        }
        songTitle.text = if (NowPlaying.title.isEmpty()) "Nothing playing" else NowPlaying.title
        songArtist.text = NowPlaying.artist
        var pos = NowPlaying.positionMs
        if (NowPlaying.playing && NowPlaying.updatedAt > 0) pos += System.currentTimeMillis() - NowPlaying.updatedAt
        val dur = NowPlaying.durationMs
        if (dur > 0 && pos > dur) pos = dur
        bar.fraction = if (dur > 0) pos.toFloat() / dur else 0f
        bar.invalidate()
        timeLeft.text = fmt(pos)
        timeRight.text = fmt(dur)
    }
}
