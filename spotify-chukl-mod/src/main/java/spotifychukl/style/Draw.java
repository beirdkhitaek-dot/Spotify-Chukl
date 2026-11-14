package spotifychukl.style;

import spotifychukl.Config;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Small drawing helpers shared by the styles. */
public final class Draw {
    public static final int SPOTIFY_GREEN = 0xFF1DB954;

    private Draw() {}

    /** Filled rounded rectangle built from horizontal strips. */
    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
        if (h - 2 * r > 0) ctx.fill(x, y + r, x + w, y + h - r, color);
    }

    public static String time(long ms) {
        long s = Math.max(0, ms / 1000);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    /** Right-hand time label: "-remaining" or total length, depending on the setting. */
    public static String endTime(long elapsed, long duration) {
        if (Config.get().remainingTime) return "-" + time(Math.max(0, duration - elapsed));
        return time(duration);
    }

    /** Applies the background-opacity setting to a translucent card colour. */
    public static int bg(int argb) {
        float o = Config.get().opacity / 100f;
        int a = Math.round(((argb >>> 24) & 255) * o);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** Spotify green, or the user's own colour when "My color" is on. */
    public static int spotify(Frame f) {
        return Config.get().customAccent ? f.accent() : SPOTIFY_GREEN;
    }

    /** Text cut to the given width with an ellipsis, so it never leaves its box. */
    public static String fit(TextRenderer font, String s, int maxW) {
        if (s == null) return "";
        if (font.getWidth(s) <= maxW) return s;
        int dots = font.getWidth("...");
        if (maxW <= dots) return "";
        return font.trimToWidth(s, maxW - dots) + "...";
    }

    /** Text inside a box: scrolls back and forth when too long (if enabled), otherwise ends with "...". */
    public static void text(DrawContext ctx, TextRenderer font, String s, int x, int y, int maxW, int color, boolean shadow) {
        if (s == null || s.isEmpty() || maxW <= 0) return;
        int w = font.getWidth(s);
        if (w <= maxW) {
            ctx.drawText(font, s, x, y, color, shadow);
            return;
        }
        if (!Config.get().scrollText) {
            ctx.drawText(font, fit(font, s, maxW), x, y, color, shadow);
            return;
        }
        int over = w - maxW;
        long travel = over * 30L;
        long cycle = travel * 2 + 3000;
        long p = System.currentTimeMillis() % cycle;
        long off;
        if (p < 1500) off = 0;
        else if (p < 1500 + travel) off = (p - 1500) / 30;
        else if (p < 1500 + travel + 1500) off = over;
        else off = over - (p - 3000 - travel) / 30;
        off = Math.max(0, Math.min(over, off));
        ctx.enableScissor(x, y - 1, x + maxW, y + 10);
        ctx.drawText(font, s, x - (int) off, y, color, shadow);
        ctx.disableScissor();
    }

    public static void text(DrawContext ctx, TextRenderer font, String s, int x, int y, int maxW, int color) {
        text(ctx, font, s, x, y, maxW, color, false);
    }

    /** Cover art in the chosen shape (square / rounded / circle). */
    public static void cover(DrawContext ctx, Identifier tex, int x, int y, int size) {
        String shape = Config.get().coverShape;
        int r = "circle".equals(shape) ? size / 2 : "rounded".equals(shape) ? Math.max(2, size / 8) : 0;
        if (r == 0) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0F, 0.0F, size, size, 64, 64, 64, 64, 0xFFFFFFFF);
            return;
        }
        int rh = Math.max(1, Math.round(64f / size));
        for (int i = 0; i < size; i++) {
            double dy = i < r ? r - i - 0.5 : i >= size - r ? i - (size - r) + 0.5 : -1;
            int inset = dy >= 0 ? r - (int) Math.round(Math.sqrt(Math.max(0, (double) r * r - dy * dy))) : 0;
            int w = size - 2 * inset;
            if (w <= 0) continue;
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x + inset, y + i, inset * 64f / size, i * 64f / size,
                w, 1, Math.max(1, Math.round(w * 64f / size)), rh, 64, 64, 0xFFFFFFFF);
        }
    }

    // ---- liquid glass helpers ---------------------------------------------------------------------------------

    public static final int GLASS_TOP = 0xCC2C3242, GLASS_BOT = 0xCC12151F, GLASS_RIM = 0x78FFFFFF;

    public static int lerp(int a, int b, float t) {
        int ar = a >>> 24, rr = (a >> 16) & 255, gr = (a >> 8) & 255, br = a & 255;
        int a2 = b >>> 24, r2 = (b >> 16) & 255, g2 = (b >> 8) & 255, b2 = b & 255;
        return (Math.round(ar + (a2 - ar) * t) << 24) | (Math.round(rr + (r2 - rr) * t) << 16)
            | (Math.round(gr + (g2 - gr) * t) << 8) | Math.round(br + (b2 - br) * t);
    }

    /** Mixes the colour of {@code accent} into {@code base} but keeps the alpha of {@code base}. */
    public static int mix(int base, int accent, float f) {
        int m = lerp(base | 0xFF000000, accent | 0xFF000000, f) & 0xFFFFFF;
        return (base & 0xFF000000) | m;
    }

    public static int alpha(int argb, float m) {
        int a = Math.max(0, Math.min(255, Math.round((argb >>> 24) * m)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static int insetOf(int row, int h, int r) {
        int k = Math.min(row, h - 1 - row);
        if (k >= r) return 0;
        double dy = r - k - 0.5;
        return r - (int) Math.round(Math.sqrt(Math.max(0, (double) r * r - dy * dy)));
    }

    /**
     * Liquid glass panel: vertical gradient body, a bright 1px rim that fades towards the bottom (light catching the
     * top edge) and a soft sheen band. {@code top}/{@code bot} are the body colours, {@code rim} the edge colour.
     */
    public static void glass(DrawContext ctx, int x, int y, int w, int h, int r, int top, int bot, int rim) {
        if (w < 4 || h < 4) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            float t = h <= 1 ? 0f : row / (float) (h - 1);
            int body = lerp(top, bot, t);
            int edge = alpha(rim, 1f - 0.65f * t);
            int in = insetOf(row, h, r);
            int x0 = x + in, x1 = x + w - in, yy = y + row;
            if (row == 0 || row == h - 1) {
                ctx.fill(x0, yy, x1, yy + 1, edge);
                continue;
            }
            int near = row < h / 2 ? insetOf(row - 1, h, r) : insetOf(row + 1, h, r);
            int ew = Math.max(1, Math.abs(in - near));
            ctx.fill(x0, yy, Math.min(x1, x0 + ew), yy + 1, edge);
            ctx.fill(Math.max(x0, x1 - ew), yy, x1, yy + 1, edge);
            if (x1 - ew > x0 + ew) ctx.fill(x0 + ew, yy, x1 - ew, yy + 1, body);
        }
        if (h >= 18 && w >= 18) roundRect(ctx, x + 3, y + 2, w - 6, Math.max(4, h / 3), Math.max(1, r - 2), 0x12FFFFFF);
    }

    /** Soft shadow under a panel so it floats above the game. */
    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int r) {
        for (int i = 3; i >= 1; i--) roundRect(ctx, x - i + 1, y + i, w + 2 * i - 2, h + i, r + i, 0x0C000000);
    }

    /** Standard dark glass card tinted a little with the accent colour. Returns nothing; honours the opacity setting. */
    public static void card(DrawContext ctx, int x, int y, int w, int h, int r, int accent) {
        shadow(ctx, x, y, w, h, r);
        glass(ctx, x, y, w, h, r, bg(mix(GLASS_TOP, accent, 0.14f)), bg(mix(GLASS_BOT, accent, 0.07f)), mix(GLASS_RIM, accent, 0.30f));
    }

    /** Glass progress bar: frosted track, accent fill and a glowing white knob. */
    public static void knobBar(DrawContext ctx, int x, int y, int w, float fraction, int accent) {
        roundRect(ctx, x, y, w, 4, 2, 0x4DFFFFFF);
        fraction = Math.max(0f, Math.min(1f, fraction));
        int fill = Math.max(4, (int) (w * fraction));
        roundRect(ctx, x, y, fill, 4, 2, accent);
        roundRect(ctx, x + fill - 5, y - 3, 10, 10, 5, alpha(accent, 0.30f));
        roundRect(ctx, x + fill - 3, y - 1, 6, 6, 3, 0xFFFFFFFF);
    }

    /** Small frosted pill with a label, for example "NOW PLAYING". Returns its width. */
    public static int chip(DrawContext ctx, TextRenderer font, String label, int x, int y, int color) {
        int w = font.getWidth(label) + 10;
        glass(ctx, x, y, w, 11, 5, alpha(color, 0.35f), alpha(color, 0.20f), alpha(color, 0.60f));
        ctx.drawText(font, label, x + 5, y + 2, color, false);
        return w;
    }

    private static int tri(int i, int h) {
        return Math.max(1, (int) Math.round((h - Math.abs(2 * i - (h - 1))) * 0.5));
    }

    public static void iconPlay(DrawContext ctx, int x, int y, int h, int color) {
        for (int i = 0; i < h; i++) ctx.fill(x, y + i, x + tri(i, h), y + i + 1, color);
    }

    public static void iconPause(DrawContext ctx, int x, int y, int h, int color) {
        int bw = Math.max(2, h / 4);
        ctx.fill(x, y, x + bw, y + h, color);
        ctx.fill(x + bw + 2, y, x + 2 * bw + 2, y + h, color);
    }

    /** Skip icon (triangle plus bar), pointing right when next is true. */
    public static void iconSkip(DrawContext ctx, int x, int y, int h, boolean next, int color) {
        int w = h / 2 + 1;
        for (int i = 0; i < h; i++) {
            int len = tri(i, h);
            if (next) ctx.fill(x, y + i, x + len, y + i + 1, color);
            else ctx.fill(x + w - len, y + i, x + w, y + i + 1, color);
        }
        ctx.fill(next ? x + w : x - 2, y, next ? x + w + 2 : x, y + h, color);
    }
}
