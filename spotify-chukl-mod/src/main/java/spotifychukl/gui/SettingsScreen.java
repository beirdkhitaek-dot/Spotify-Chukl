package spotifychukl.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.ArrayList;
import java.util.List;
import spotifychukl.Config;
import spotifychukl.Tracker;
import spotifychukl.HudRenderer;
import spotifychukl.style.Draw;
import spotifychukl.style.Frame;
import spotifychukl.style.HudStyle;
import spotifychukl.style.HudStyles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Settings screen (Right Shift). Responsive: the layout is built for a design size and the whole panel is scaled down
 * to fit small screens, every label is trimmed to its box, and content is split over tabs.
 */
public final class SettingsScreen extends Screen {
    private interface Act { void run(double x, double y); }
    private record Hit(int x, int y, int w, int h, Act press, Act drag) {}

    private static final int DESIGN_W = 290, DESIGN_H = 170;
    private static final String[] TABS = {"Style", "Colour", "Options", "Backup"};
    private static final String[] THEMES = {"glass", "light", "mint"};
    private static final int[] PRESETS = {0xE0457B, 0x4C7DFF, 0xFF9A1F, 0x2ECC71, 0x2DB7F5, 0x9B6BFF};

    private static Identifier wheel;
    private static int tab;

    // palette of the current theme
    private int dim, panel, border, field, hover, text, muted, panelTop, panelBot, rim, onAcc;

    private final List<Hit> hits = new ArrayList<>();
    private Hit active;
    private TextFieldWidget userField, keyField;
    private int px, py, pw, ph;
    private float s = 1f;
    private float hue, sat, val;

    public SettingsScreen() {
        super(Text.literal("Spotify Chukl"));
        syncWheelFromColor();
    }

    private int accent() { return 0xFF000000 | (Config.get().accentColor & 0xFFFFFF); }

    private String theme() { return Config.get().guiTheme; }

    private boolean glass() { return true; }

    private void applyTheme() {
        switch (theme()) {
            case "light" -> { dim = 0x70101820; panelTop = 0xF2FAFBFD; panelBot = 0xEEE3E8EF; rim = 0xFFFFFFFF; border = 0x33000000; field = 0x1C000000; hover = 0x2E000000; text = 0xFF15181F; muted = 0xFF5B6270; }
            case "mint" -> { dim = 0x80102018; panelTop = 0xF0DDF6EA; panelBot = 0xEEB9E7D1; rim = 0xFFFFFFFF; border = 0x40206A4C; field = 0x40FFFFFF; hover = 0x66FFFFFF; text = 0xFF0E3B2B; muted = 0xFF4F7A68; }
            default -> { dim = 0x980A0E16; panelTop = 0xEE2A3040; panelBot = 0xEE11141D; rim = 0x80FFFFFF; border = 0x44FFFFFF; field = 0x26FFFFFF; hover = 0x3DFFFFFF; text = 0xFFF2F4F8; muted = 0xFF9AA1B2; }
        }
        panel = panelTop;
        int a = accent();
        int lum = (((a >> 16) & 255) * 3 + ((a >> 8) & 255) * 6 + (a & 255)) / 10;
        onAcc = lum > 150 ? 0xFF101014 : 0xFFFFFFFF;
    }

    private void glow(DrawContext c, int x, int y, int w, int h, int r, int rgb, int strength) {
        for (int i = 3; i >= 1; i--) {
            int a = Math.max(4, strength / (i + 1));
            Draw.roundRect(c, x - i * 2, y - i * 2, w + i * 4, h + i * 4, r + i * 2, (a << 24) | (rgb & 0xFFFFFF));
        }
    }

    private void card(DrawContext c, int x, int y, int w, int h) {
        Draw.glass(c, x, y, w, h, 9, field, field, border);
    }

    private static int hsv(float h, float s, float v) {
        float c = v * s, x = c * (1 - Math.abs((h * 6) % 2 - 1)), m = v - c, r, g, b;
        int seg = (int) (h * 6) % 6;
        switch (seg) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }

    private void applyWheelColor() { Config.get().accentColor = hsv(hue, sat, val); }

    private void syncWheelFromColor() {
        int rgb = Config.get().accentColor & 0xFFFFFF;
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), d = max - Math.min(r, Math.min(g, b));
        val = Math.max(0.35f, max);
        sat = max == 0 ? 0 : d / max;
        float h = d == 0 ? 0 : max == r ? ((g - b) / d) % 6 : max == g ? (b - r) / d + 2 : (r - g) / d + 4;
        hue = (h / 6f + 1f) % 1f;
    }

    private static final class WheelTexture extends NativeImageBackedTexture {
        WheelTexture(NativeImage image) {
            super(() -> "SpotifyChukl wheel", image);
            this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        }
    }

    private static void ensureWheel(MinecraftClient mc) {
        if (wheel != null) return;
        NativeImage img = new NativeImage(128, 128, false);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                double dx = x - 63.5, dy = y - 63.5, d = Math.sqrt(dx * dx + dy * dy);
                double a = Math.max(0, Math.min(1, 63.5 - d + 0.5));
                int argb = 0;
                if (a > 0) {
                    float h = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
                    argb = ((int) (a * 255) << 24) | hsv(h, (float) Math.min(1, d / 63.5), 1f);
                }
                img.setColorArgb(x, y, argb);
            }
        }
        Identifier id = Identifier.of("spotifychukl", "wheel");
        mc.getTextureManager().registerTexture(id, new WheelTexture(img));
        wheel = id;
    }

    // ---- layout -----------------------------------------------------------------------------------------------

    private void layout() {
        s = Math.min(1f, Math.min((width - 8f) / DESIGN_W, (height - 8f) / DESIGN_H));
        s = Math.max(0.3f, s);
        int vw = (int) (width / s), vh = (int) (height / s);
        pw = Math.min(300, vw - 8);
        ph = Math.min(190, vh - 8);
        px = (vw - pw) / 2;
        py = (vh - ph) / 2;
    }

    @Override
    protected void init() {
        layout();
        userField = new TextFieldWidget(textRenderer, 0, 0, 100, 14, Text.literal("Last.fm username (optional)"));
        userField.setDrawsBackground(false);
        userField.setMaxLength(64);
        userField.setEditableColor(0xFFF0F0F4);
        userField.setPlaceholder(Text.literal("username").withColor(0x707078));
        userField.setText(Config.get().user);
        userField.setChangedListener(v -> Config.get().user = v.trim());
        addDrawableChild(userField);
        keyField = new TextFieldWidget(textRenderer, 0, 0, 100, 14, Text.literal("API key"));
        keyField.setDrawsBackground(false);
        keyField.setMaxLength(64);
        keyField.setEditableColor(0xFFF0F0F4);
        keyField.setPlaceholder(Text.literal("Last.fm key (optional)").withColor(0x707078));
        keyField.setText(Config.get().apiKey);
        keyField.setChangedListener(v -> Config.get().apiKey = v.trim());
        addDrawableChild(keyField);
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void renderBackground(DrawContext c, int x, int y, float delta) {}

    private boolean inside(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    /** Text that can never leave its box. */
    private void put(DrawContext c, String str, int x, int y, int maxW, int color) {
        c.drawText(textRenderer, Draw.fit(textRenderer, str, maxW), x, y, color, false);
    }

    private void button(DrawContext c, String label, int x, int y, int w, int h, boolean on, int mx, int my, Act press) {
        boolean over = inside(mx, my, x, y, w, h);
        if (on) {
            glow(c, x, y, w, h, 7, accent(), 0x58);
            Draw.glass(c, x, y, w, h, Math.min(8, h / 2), Draw.mix(0xFFFFFFFF, accent(), 0.80f), accent(), 0xB0FFFFFF);
        } else {
            Draw.glass(c, x, y, w, h, Math.min(8, h / 2), over ? hover : field, over ? hover : field, border);
        }
        String t = Draw.fit(textRenderer, label, w - 6);
        c.drawText(textRenderer, t, x + (w - textRenderer.getWidth(t)) / 2, y + (h - 8) / 2 + 1, on ? onAcc : text, false);
        hits.add(new Hit(x, y, w, h, press, null));
    }

    private void slider(DrawContext c, String label, String value, int x, int y, int w, double frac, Act set) {
        int vw = textRenderer.getWidth(value);
        put(c, label, x, y, Math.max(10, w - vw - 6), muted);
        c.drawText(textRenderer, value, x + w - vw, y, text, false);
        int by = y + 12;
        int fillW = (int) (w * Math.max(0, Math.min(1, frac)));
        int kx = x + fillW;
        if (glass()) {
            Draw.roundRect(c, x, by, w, 4, 2, field);
            if (fillW > 0) { glow(c, x, by, fillW, 4, 2, accent(), 0x48); Draw.roundRect(c, x, by, Math.max(4, fillW), 4, 2, accent()); }
            glow(c, kx - 3, by - 1, 6, 6, 3, accent(), 0x50);
            Draw.roundRect(c, kx - 3, by - 1, 6, 6, 3, 0xFFFFFFFF);
        } else {
            c.fill(x, by, x + w, by + 4, field);
            c.fill(x, by, kx, by + 4, accent());
            c.fill(kx - 2, by - 3, kx + 2, by + 7, text);
        }
        hits.add(new Hit(x - 3, by - 5, w + 6, 14, set, set));
    }

    private static double frac(double x, int x0, int w) { return Math.max(0, Math.min(1, (x - x0) / (double) w)); }

    private void box(DrawContext c, int x, int y, int w, int h, boolean focused) {
        if (glass()) {
            if (focused) glow(c, x, y, w, h, 6, accent(), 0x40);
            Draw.roundRect(c, x, y, w, h, 6, focused ? accent() : border);
            Draw.glass(c, x + 1, y + 1, w - 2, h - 2, 5, field, field, 0x00000000);
        } else {
            c.fill(x, y, x + w, y + h, focused ? accent() : border);
            c.fill(x + 1, y + 1, x + w - 1, y + h - 1, field);
        }
    }

    // ---- render -----------------------------------------------------------------------------------------------

    @Override
    public void render(DrawContext c, int mxr, int myr, float delta) {
        layout();
        applyTheme();
        hits.clear();
        ensureWheel(MinecraftClient.getInstance());
        Config cfg = Config.get();
        int acc = accent();
        int mx = (int) (mxr / s), my = (int) (myr / s);

        c.fill(0, 0, width, height, dim);
        var mat = c.getMatrices();
        mat.pushMatrix();
        mat.scale(s, s);

        glow(c, px + pw / 2 - 90, py + ph - 14, 180, 10, 5, acc, 0x38);
        Draw.shadow(c, px, py, pw, ph, 12);
        Draw.glass(c, px, py, pw, ph, 12, panelTop, panelBot, rim);

        int x0 = px + 14, cw = pw - 28;
        // header: logo chip, title, live connection status, theme switch
        String themeLabel = Character.toUpperCase(theme().charAt(0)) + theme().substring(1);
        int tw = Math.min(cw - 80, textRenderer.getWidth(themeLabel) + 14);
        Draw.glass(c, x0, py + 7, 14, 14, 5, Draw.mix(0xFFFFFFFF, acc, 0.8f), acc, 0xB0FFFFFF);
        c.drawText(textRenderer, "\u266A", x0 + 4, py + 10, onAcc, false);
        String status = Tracker.status();
        int stW = Math.min(110, textRenderer.getWidth(status) + 16);
        put(c, "Spotify Chukl", x0 + 20, py + 10, cw - tw - stW - 34, text);
        int stX = px + pw - 14 - tw - 6 - stW;
        boolean good = Tracker.now() != null;
        Draw.glass(c, stX, py + 7, stW, 14, 7, field, field, border);
        Draw.roundRect(c, stX + 5, py + 12, 4, 4, 2, good ? 0xFF3DDC84 : 0xFFE0A030);
        put(c, status, stX + 12, py + 10, stW - 15, muted);
        button(c, themeLabel, px + pw - 14 - tw, py + 7, tw, 14, false, mx, my, (x, y) -> {
            int i = java.util.Arrays.asList(THEMES).indexOf(theme());
            cfg.guiTheme = THEMES[(i + 1) % THEMES.length];
        });

        // tabs: one frosted track with the active tab lit up
        int tabW = (cw - 3 * 3 - 6) / 4;
        Draw.glass(c, x0, py + 25, cw, 20, 10, field, field, border);
        for (int i = 0; i < TABS.length; i++) {
            final int ti = i;
            button(c, TABS[i], x0 + 3 + i * (tabW + 3), py + 27, tabW, 16, tab == i, mx, my, (x, y) -> {
                tab = ti;
                setFocused(null);
                userField.setFocused(false);
                keyField.setFocused(false);
            });
        }

        int cy0 = py + 52, ch = ph - 52 - 30;
        card(c, px + 8, cy0 - 3, pw - 16, ch + 6);

        boolean account = tab == 3;
        userField.visible = account;
        keyField.visible = account;
        switch (tab) {
            case 0 -> styleTab(c, cfg, x0, cy0, cw, ch, mx, my);
            case 1 -> colourTab(c, cfg, x0, cy0, cw, ch, mx, my);
            case 2 -> optionsTab(c, cfg, x0, cy0, cw, ch, mx, my);
            default -> accountTab(c, x0, cy0, cw);
        }

        button(c, "Done", px + pw - 14 - 48, py + ph - 24, 48, 16, true, mx, my, (x, y) -> close());
        put(c, "Drag the HUD in chat to move it", x0, py + ph - 20, cw - 60, muted);

        mat.popMatrix();
        super.render(c, mxr, myr, delta);
    }

    private void styleTab(DrawContext c, Config cfg, int x0, int cy0, int cw, int ch, int mx, int my) {
        List<String> ids = HudStyles.ids();
        int leftW = cw * 5 / 11;
        int bw = (leftW - 4) / 2;
        int rows = (ids.size() + 1) / 2;
        int rowH = Math.max(14, Math.min(20, ch / rows));
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            String label = Character.toUpperCase(id.charAt(0)) + id.substring(1);
            button(c, label, x0 + (i % 2) * (bw + 4), cy0 + (i / 2) * rowH, bw, rowH - 3, id.equalsIgnoreCase(cfg.style), mx, my, (x, y) -> cfg.style = id);
        }
        // live preview, scaled to fit its box
        int bx = x0 + leftW + 10, bwid = cw - leftW - 10;
        put(c, "Preview", bx, cy0, bwid, muted);
        int ay = cy0 + 12, ah = ch - 12;
        HudStyle st = HudStyles.get(cfg.style);
        boolean lyr = cfg.lyrics;
        int sw = st.width(), sh = st.height(lyr);
        float k = Math.min(1f, Math.min(bwid / (float) sw, ah / (float) sh));
        int acc = cfg.customAccent ? accent() : HudRenderer.currentTexture() != null ? HudRenderer.currentAccent() : 0xFFD51007;
        Frame f = new Frame("Blinding Lights", "The Weeknd", 92000, 200000, HudRenderer.currentTexture(), acc,
            lyr ? new String[] {"", "Sample lyric line", "Next lyric line"} : null);
        var mat = c.getMatrices();
        mat.pushMatrix();
        mat.translate(bx + (bwid - sw * k) / 2f, ay + (ah - sh * k) / 2f);
        mat.scale(k, k);
        try {
            st.draw(c, MinecraftClient.getInstance(), f);
        } finally {
            mat.popMatrix();
        }
    }

    private void colourTab(DrawContext c, Config cfg, int x0, int cy0, int cw, int ch, int mx, int my) {
        int ws = Math.max(60, Math.min(96, ch - 4));
        int wy = cy0 + (ch - ws) / 2;
        int tint = 0xFF000000 | (Math.round(val * 255) * 0x010101);
        c.drawTexture(RenderPipelines.GUI_TEXTURED, wheel, x0, wy, 0.0F, 0.0F, ws, ws, 128, 128, 128, 128, tint);
        double ang = hue * 2 * Math.PI;
        int half = ws / 2;
        int mxk = x0 + half + (int) Math.round(Math.cos(ang) * sat * half), myk = wy + half + (int) Math.round(Math.sin(ang) * sat * half);
        c.fill(mxk - 3, myk - 3, mxk + 3, myk + 3, 0xFFFFFFFF);
        c.fill(mxk - 2, myk - 2, mxk + 2, myk + 2, 0xFF000000 | hsv(hue, sat, val));
        final int wcx = x0 + half, wcy = wy + half;
        Act wheelAct = (x, y) -> {
            double dx = x - wcx, dy = y - wcy;
            sat = (float) Math.min(1, Math.sqrt(dx * dx + dy * dy) / half);
            hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
            applyWheelColor();
            cfg.customAccent = true;
        };
        hits.add(new Hit(x0, wy, ws, ws, wheelAct, wheelAct));

        int rx = x0 + ws + 16, rw = cw - ws - 16;
        slider(c, "Brightness", Math.round((val - 0.35f) / 0.65f * 100) + "%", rx, cy0, rw, (val - 0.35f) / 0.65f, (x, y) -> {
            val = (float) (0.35 + 0.65 * frac(x, rx, rw));
            applyWheelColor();
            cfg.customAccent = true;
        });
        int hy = cy0 + 30;
        if (glass()) { glow(c, rx, hy, rw, 14, 6, accent(), 0x40); Draw.glass(c, rx, hy, rw, 14, 6, Draw.mix(0xFFFFFFFF, accent(), 0.8f), accent(), 0xB0FFFFFF); }
        else c.fill(rx, hy, rx + rw, hy + 14, accent());
        String hex = String.format("#%06X", cfg.accentColor & 0xFFFFFF);
        c.drawText(textRenderer, hex, rx + (rw - textRenderer.getWidth(hex)) / 2, hy + 3, onAcc, false);
        int sw = Math.min(18, (rw - 5 * 4) / 6);
        for (int i = 0; i < PRESETS.length; i++) {
            int col = PRESETS[i], sx = rx + i * (sw + 4);
            if (glass()) Draw.roundRect(c, sx, cy0 + 50, sw, sw, 5, 0xFF000000 | col);
            else c.fill(sx, cy0 + 50, sx + sw, cy0 + 50 + sw, 0xFF000000 | col);
            hits.add(new Hit(sx, cy0 + 50, sw, sw, (x, y) -> {
                cfg.accentColor = col;
                cfg.customAccent = true;
                syncWheelFromColor();
            }, null));
        }
        button(c, cfg.customAccent ? "My color: ON" : "My color: OFF", rx, cy0 + 50 + sw + 6, rw, 16, cfg.customAccent, mx, my,
            (x, y) -> cfg.customAccent = !cfg.customAccent);
    }

    private void optionsTab(DrawContext c, Config cfg, int x0, int cy0, int cw, int ch, int mx, int my) {
        int lw = Math.min(130, cw * 48 / 100);
        slider(c, "Size", String.format("%.2fx", cfg.scale), x0, cy0, lw, (cfg.scale - 0.5) / 2.5,
            (x, y) -> cfg.scale = (float) (Math.round((0.5 + 2.5 * frac(x, x0, lw)) * 20) / 20.0));
        slider(c, "Lyrics sync", cfg.lyricsOffsetMs + " ms", x0, cy0 + 28, lw, (cfg.lyricsOffsetMs + 5000) / 10000.0,
            (x, y) -> cfg.lyricsOffsetMs = (int) (Math.round((-5000 + 10000 * frac(x, x0, lw)) / 50.0) * 50));
        slider(c, "Opacity", cfg.opacity + "%", x0, cy0 + 56, lw, (cfg.opacity - 20) / 80.0,
            (x, y) -> cfg.opacity = (int) (Math.round((20 + 80 * frac(x, x0, lw)) / 5.0) * 5));

        int rx = x0 + lw + 14, rw = cw - lw - 14, bw = (rw - 4) / 2;
        button(c, "HUD", rx, cy0, bw, 16, cfg.enabled, mx, my, (x, y) -> cfg.enabled = !cfg.enabled);
        button(c, "Lyrics", rx + bw + 4, cy0, bw, 16, cfg.lyrics, mx, my, (x, y) -> cfg.lyrics = !cfg.lyrics);
        button(c, "Remaining", rx, cy0 + 20, bw, 16, cfg.remainingTime, mx, my, (x, y) -> cfg.remainingTime = !cfg.remainingTime);
        button(c, "Scroll text", rx + bw + 4, cy0 + 20, bw, 16, cfg.scrollText, mx, my, (x, y) -> cfg.scrollText = !cfg.scrollText);
        String shape = cfg.coverShape.substring(0, 1).toUpperCase() + cfg.coverShape.substring(1);
        button(c, "Cover: " + shape, rx, cy0 + 40, rw, 16, false, mx, my, (x, y) -> {
            cfg.coverShape = switch (cfg.coverShape) { case "square" -> "rounded"; case "rounded" -> "circle"; default -> "square"; };
        });
        button(c, "Reset to defaults", rx, cy0 + 60, rw, 16, false, mx, my, (x, y) -> {
            Config d = new Config();
            cfg.x = d.x; cfg.y = d.y; cfg.scale = d.scale; cfg.style = d.style; cfg.lyrics = d.lyrics;
            cfg.lyricsOffsetMs = d.lyricsOffsetMs; cfg.accentColor = d.accentColor; cfg.customAccent = d.customAccent;
            cfg.opacity = d.opacity; cfg.remainingTime = d.remainingTime; cfg.scrollText = d.scrollText; cfg.coverShape = d.coverShape;
            syncWheelFromColor();
        });
    }

    private void accountTab(DrawContext c, int x0, int cy0, int cw) {
        int fw = Math.min(230, cw);
        userField.setEditableColor(text);
        keyField.setEditableColor(text);
        put(c, "Last.fm username (only if you use Last.fm)", x0, cy0, fw, muted);
        box(c, x0, cy0 + 10, fw, 18, userField.isFocused());
        put(c, "API key", x0, cy0 + 34, fw, muted);
        box(c, x0, cy0 + 44, fw, 18, keyField.isFocused());
        put(c, "Not needed if the Spotify Chukl app is running", x0, cy0 + 68, cw, muted);
        // text fields are real widgets drawn in screen space: place them (scaled) inside the boxes
        userField.setX(Math.round((x0 + 3) * s));
        userField.setY(Math.round((cy0 + 13) * s));
        userField.setWidth(Math.max(20, Math.round((fw - 6) * s)));
        keyField.setX(Math.round((x0 + 3) * s));
        keyField.setY(Math.round((cy0 + 47) * s));
        keyField.setWidth(Math.max(20, Math.round((fw - 6) * s)));
    }

    // ---- input ------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            double x = click.x() / s, y = click.y() / s;
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (inside((int) x, (int) y, h.x(), h.y(), h.w(), h.h())) {
                    setFocused(null);
                    userField.setFocused(false);
                    keyField.setFocused(false);
                    h.press().run(x, y);
                    active = h.drag() != null ? h : null;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (active != null && active.drag() != null) {
            active.drag().run(click.x() / s, click.y() / s);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (active != null) {
            active = null;
            Config.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput key) {
        boolean typing = (userField != null && userField.isFocused()) || (keyField != null && keyField.isFocused());
        if (key.key() == 256 || (key.key() == 344 && !typing)) { // Esc or Right Shift
            close();
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    public void removed() {
        Config.save();
        super.removed();
    }
}
