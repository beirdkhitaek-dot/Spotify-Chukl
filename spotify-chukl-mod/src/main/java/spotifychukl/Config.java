package spotifychukl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** config/spotifychukl.json */
public final class Config {
    public boolean enabled = true;
    public String apiKey = "";
    public String user = "";
    public int x = 6;
    public int y = 40;
    public float scale = 1.0f;
    public String style = "glass";
    public boolean lyrics = true;
    public int lyricsOffsetMs = 0;
    public int accentColor = 0xE0457B;
    public boolean customAccent = false;
    public String guiTheme = "glass";
    public int opacity = 100;
    public boolean remainingTime = true;
    public boolean scrollText = true;
    public String coverShape = "rounded";

    private static Config instance = new Config();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("spotifychukl.json");
    }

    public static Config get() {
        return instance;
    }

    public static synchronized void load() {
        try {
            Path old = FabricLoader.getInstance().getConfigDir().resolve("lastfmhud.json");
            Path src = Files.exists(file()) ? file() : old; // keep settings from the old Last.fm HUD
            if (Files.exists(src) && Files.size(src) < 65536) {
                Config read = new Gson().fromJson(Files.readString(src), Config.class);
                if (read != null) instance = read;
            }
        } catch (Exception ignored) {
            instance = new Config();
        }
        if (instance.apiKey == null) instance.apiKey = "";
        if (instance.user == null) instance.user = "";
        if (instance.style == null || instance.style.isBlank()) instance.style = "glass";
        if (instance.guiTheme == null || !java.util.List.of("glass", "light", "mint").contains(instance.guiTheme)) instance.guiTheme = "glass";
        if (instance.coverShape == null || !java.util.List.of("square", "rounded", "circle").contains(instance.coverShape)) instance.coverShape = "rounded";
        instance.opacity = Math.max(20, Math.min(100, instance.opacity));
        if (instance.scale < 0.5f || instance.scale > 4f) instance.scale = 1.0f;
        instance.lyricsOffsetMs = Math.max(-10000, Math.min(10000, instance.lyricsOffsetMs));
        save();
    }

    public static synchronized void save() {
        try {
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(instance));
        } catch (IOException ignored) {
        }
    }
}
