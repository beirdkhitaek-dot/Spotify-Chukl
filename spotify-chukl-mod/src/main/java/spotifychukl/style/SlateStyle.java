package spotifychukl.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Style "slate": a grey notification card with a second card peeking out behind it. Monochrome, no colour.
 * Shows a small app label, title, artist and a thin white progress line.
 */
public final class SlateStyle implements HudStyle {
    private static final int W = 176, TOP_H = 58, GAP = 2, LYRICS_H = 24;

    @Override public String id() { return "slate"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? TOP_H + GAP + LYRICS_H : TOP_H; }

    private static void gray(DrawContext ctx, int x, int y, int w, int h, int r, boolean back) {
        int a = back ? 0x88 : 0xD2;
        Draw.glass(ctx, x, y, w, h, r, Draw.bg((a << 24) | 0x3A3D46), Draw.bg((a << 24) | 0x1E2026), back ? 0x40FFFFFF : 0x70FFFFFF);
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        gray(ctx, 6, 6, W - 12, TOP_H - 6, 8, true);
        Draw.shadow(ctx, 0, 0, W, TOP_H - 6, 9);
        gray(ctx, 0, 0, W, TOP_H - 6, 9, false);

        int tx = 10;
        if (f.texture() != null) {
            Draw.cover(ctx, f.texture(), 7, 8, 36);
            tx = 50;
        }
        int maxW = W - tx - 10;
        ctx.drawText(font, "SPOTIFY CHUKL", tx, 6, 0xFF8E93A0, false);
        String now = "now";
        ctx.drawText(font, now, W - 8 - font.getWidth(now), 6, 0xFF6E7380, false);
        Draw.text(ctx, font, f.title(), tx, 17, maxW, 0xFFFFFFFF);
        Draw.text(ctx, font, f.artist(), tx, 28, maxW, 0xFFA8ADB8);

        int barY = 41;
        Draw.roundRect(ctx, tx, barY, maxW, 2, 1, 0x44FFFFFF);
        if (f.durationMs() > 0) {
            long pos = Math.min(f.durationMs(), Math.max(0, f.elapsedMs()));
            Draw.roundRect(ctx, tx, barY, Math.max(2, (int) (maxW * pos / f.durationMs())), 2, 1, 0xE6FFFFFF);
        }

        if (f.lyrics() != null) {
            int y0 = TOP_H + GAP;
            gray(ctx, 0, y0, W, LYRICS_H, 8, false);
            String cur = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            String next = f.lyrics()[1].isEmpty() ? "" : f.lyrics()[2];
            Draw.text(ctx, font, cur, 8, y0 + 4, W - 16, 0xFFFFFFFF);
            Draw.text(ctx, font, next, 8, y0 + 14, W - 16, 0xFF6E7380);
        }
    }
}
