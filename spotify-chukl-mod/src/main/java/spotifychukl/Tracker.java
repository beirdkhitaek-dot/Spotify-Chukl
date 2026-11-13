package spotifychukl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 * Finds the song you are playing. First choice is the Spotify Chukl phone app (exact song, exact position,
 * talks only to 127.0.0.1). If the app is not running it falls back to Last.fm when a key is set.
 */
public final class Tracker {
    public record Now(String title, String artist, long startedMs, long durationMs) {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "SpotifyChukl poller");
        t.setDaemon(true);
        return t;
    });

    private static volatile Now now;
    private static volatile int[] art;
    private static volatile int artSerial;
    private static volatile String status = "Starting";

    private static String lastKey = "";
    private static long lastStarted, lastDuration;
    private static String lastArtUrl = "";
    private static String lastFallbackArt = "";
    private static boolean usingApp;
    private static final String APP_URL = "http://127.0.0.1:8765/";

    private static boolean placeholder(String url) {
        return url.isEmpty() || url.contains("2a96cbd8b46e442fc41c2b86b821562f");
    }

    /** Cover lookup on iTunes (free, no key) for tracks where Last.fm only has its grey placeholder. */
    private static String itunesArt(String artist, String title) {
        try {
            HttpResponse<String> r = get("https://itunes.apple.com/search?media=music&entity=song&limit=1&term=" + enc(artist + " " + title));
            if (r.statusCode() != 200) return "";
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!o.has("results") || o.getAsJsonArray("results").size() == 0) return "";
            String u = str(o.getAsJsonArray("results").get(0).getAsJsonObject(), "artworkUrl100");
            return u.replace("100x100bb", "300x300bb");
        } catch (Exception e) {
            return "";
        }
    }
    private static int errors;
    private static long backoffUntil;

    private Tracker() {}

    public static Now now() { return now; }
    public static int[] art() { return art; }
    public static int artSerial() { return artSerial; }
    public static String status() { return status; }
    public static boolean usingApp() { return usingApp; }

    public static void start() {
        EXECUTOR.scheduleWithFixedDelay(Tracker::poll, 1, 1, TimeUnit.SECONDS);
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String str(JsonObject o, String k) {
        return o != null && o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private static HttpResponse<String> get(String url) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
            .header("User-Agent", "SpotifyChukl/1.0").GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Returns true when the Spotify Chukl app answered (even if nothing is playing), so Last.fm is not needed. */
    private static boolean pollApp() {
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(APP_URL)).timeout(Duration.ofMillis(1200))
                .header("User-Agent", "SpotifyChukl/1.8").GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) return false;
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!"Spotify Chukl".equals(str(o, "app"))) return false;
            usingApp = true;
            boolean spotify = o.has("spotify") && o.get("spotify").getAsBoolean();
            boolean playing = o.has("playing") && o.get("playing").getAsBoolean();
            String title = clean(str(o, "title"));
            String artist = clean(str(o, "artist"));
            if (!spotify) { now = null; status = "Open Spotify and play a song"; Lyrics.clear(); return true; }
            if (!playing || title.isEmpty()) { now = null; status = "Paused"; return true; }
            long pos = o.has("position_ms") ? o.get("position_ms").getAsLong() : 0;
            long dur = o.has("duration_ms") ? o.get("duration_ms").getAsLong() : 0;

            String key = artist + "\u0000" + title;
            if (!key.equals(lastKey)) {
                lastKey = key;
                lastFallbackArt = itunesArt(artist, title);
            }
            // Re-sync every second so seeking in Spotify shows up on the HUD right away.
            lastStarted = System.currentTimeMillis() - pos;
            lastDuration = dur;
            if (Config.get().lyrics) Lyrics.load(artist, title, dur, key); else Lyrics.clear();
            String artUrl = lastFallbackArt;
            if (!artUrl.equals(lastArtUrl)) {
                lastArtUrl = artUrl;
                art = loadArt(artUrl);
                artSerial++;
            }
            now = new Now(title, artist, lastStarted, lastDuration);
            status = "Playing (Spotify Chukl app)";
            return true;
        } catch (Exception e) {
            usingApp = false;
            return false;
        }
    }

    private static void poll() {
        try {
            Config c = Config.get();
            if (!c.enabled) { now = null; status = "Disabled"; return; }
            if (pollApp()) return;
            if (c.apiKey.isBlank() || c.user.isBlank()) { now = null; status = "Open the Spotify Chukl app on your phone"; return; }
            if (System.currentTimeMillis() < backoffUntil) return;

            HttpResponse<String> r = get("https://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&limit=1&format=json&user="
                + enc(c.user.trim()) + "&api_key=" + enc(c.apiKey.trim()));
            if (r.statusCode() == 429) { backoffUntil = System.currentTimeMillis() + 30_000; status = "Rate limited"; return; }
            if (r.statusCode() != 200) { fail("Last.fm error " + r.statusCode() + " (check key / username)"); return; }

            JsonObject root = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!root.has("recenttracks") || !root.get("recenttracks").isJsonObject()) { fail("Unexpected Last.fm reply"); return; }
            errors = 0;
            JsonElement t = root.getAsJsonObject("recenttracks").get("track");
            JsonObject tr = t == null ? null
                : t.isJsonArray() ? (t.getAsJsonArray().size() > 0 ? t.getAsJsonArray().get(0).getAsJsonObject() : null)
                : t.isJsonObject() ? t.getAsJsonObject() : null;
            boolean playing = tr != null && tr.has("@attr") && tr.get("@attr").isJsonObject()
                && "true".equals(str(tr.getAsJsonObject("@attr"), "nowplaying"));
            if (!playing) { now = null; status = "Nothing playing"; Lyrics.clear(); return; }

            String title = clean(str(tr, "name"));
            String artist = tr.has("artist") && tr.get("artist").isJsonObject() ? clean(str(tr.getAsJsonObject("artist"), "#text")) : "";
            String artUrl = "";
            if (tr.has("image") && tr.get("image").isJsonArray()) {
                for (JsonElement e : tr.getAsJsonArray("image")) {
                    String u = str(e.getAsJsonObject(), "#text");
                    if (!u.isEmpty()) artUrl = u; // last entry is the largest
                }
            }

            String key = artist + "\u0000" + title;
            if (!key.equals(lastKey)) {
                lastFallbackArt = placeholder(artUrl) ? itunesArt(artist, title) : "";
                lastKey = key;
                lastStarted = System.currentTimeMillis();
                lastDuration = 0;
                try {
                    HttpResponse<String> i = get("https://ws.audioscrobbler.com/2.0/?method=track.getinfo&format=json&artist="
                        + enc(artist) + "&track=" + enc(title) + "&api_key=" + enc(c.apiKey.trim()));
                    if (i.statusCode() == 200) {
                        JsonObject o = JsonParser.parseString(i.body()).getAsJsonObject();
                        if (o.has("track") && o.get("track").isJsonObject()) {
                            String d = str(o.getAsJsonObject("track"), "duration");
                            if (!d.isEmpty()) lastDuration = Math.max(0, Long.parseLong(d));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            if (c.lyrics) Lyrics.load(artist, title, lastDuration, key); else Lyrics.clear();
            if (placeholder(artUrl)) artUrl = lastFallbackArt;
            if (!artUrl.equals(lastArtUrl)) {
                lastArtUrl = artUrl;
                art = loadArt(artUrl);
                artSerial++;
            }
            now = new Now(title, artist, lastStarted, lastDuration);
            status = "Playing";
        } catch (Exception e) {
            fail("Network error");
        }
    }

    private static void fail(String message) {
        status = message;
        if (++errors >= 5) now = null;
    }

    private static String clean(String s) {
        s = s.replaceAll("[\\p{Cntrl}]", " ").trim();
        return s.substring(0, Math.min(200, s.length()));
    }

    private static int[] loadArt(String url) {
        try {
            if (url.isEmpty() || url.contains("2a96cbd8b46e442fc41c2b86b821562f")) return null; // Last.fm's "no cover" placeholder
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            if (!"https".equals(uri.getScheme()) || !(host.equals("lastfm.freetls.fastly.net") || host.endsWith(".last.fm") || host.endsWith(".mzstatic.com"))) return null;
            HttpResponse<InputStream> r = HTTP.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
            byte[] bytes;
            try (InputStream in = r.body()) { bytes = in.readNBytes(2_000_001); }
            if (r.statusCode() != 200 || bytes.length > 2_000_000) return null;
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
            if (src == null) return null;
            int w = src.getWidth(), h = src.getHeight();
            if (w < 1 || h < 1 || w > 4096 || h > 4096) return null;
            BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = out.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int edge = Math.min(w, h);
                g.drawImage(src, 0, 0, 64, 64, (w - edge) / 2, (h - edge) / 2, (w + edge) / 2, (h + edge) / 2, null);
            } finally {
                g.dispose();
            }
            return out.getRGB(0, 0, 64, 64, null, 0, 64);
        } catch (Exception e) {
            return null;
        }
    }
}
