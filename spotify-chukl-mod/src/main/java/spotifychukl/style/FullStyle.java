package spotifychukl.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Style "full": a modal-window player. Big cover, "NOW PLAYING" chip, knob progress bar, elapsed / remaining time
 * and a display-only row of player buttons.
 */
public final class FullStyle implements HudStyle {
    private static final int W = 208, H = 92, GAP = 3, LYRICS_H = 34;

    @Override public String id() { return "full"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? H + GAP + LYRICS_H : H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int accent = Draw.spotify(f);
        Draw.card(ctx, 0, 0, W, H, 12, accent);

        int tx = 14;
        if (f.texture() != null) {
            Draw.roundRect(ctx, 6, 6, 62, 62, 11, Draw.alpha(accent, 0.30f));
            Draw.cover(ctx, f.texture(), 9, 9, 56);
            tx = 78;
        }
        int maxW = W - tx - 12;
        Draw.chip(ctx, font, "NOW PLAYING", tx, 8, accent);
        Draw.text(ctx, font, f.title(), tx, 23, maxW, 0xFFFFFFFF);
        Draw.text(ctx, font, f.artist(), tx, 34, maxW, 0xFFB8BECC);

        long pos = f.durationMs() > 0 ? Math.min(f.durationMs(), Math.max(0, f.elapsedMs())) : 0;
        Draw.knobBar(ctx, 12, 74, W - 24, f.durationMs() > 0 ? pos / (float) f.durationMs() : 0.08f, accent);
        if (f.durationMs() > 0) {
            String end = Draw.endTime(pos, f.durationMs());
            ctx.drawText(font, Draw.time(pos), 12, 82, 0xFFA3A9B8, false);
            ctx.drawText(font, end, W - 12 - font.getWidth(end), 82, 0xFFA3A9B8, false);
        }
        // display-only buttons
        int by = 49, cx = tx + maxW / 2;
        Draw.iconSkip(ctx, cx - 30, by, 9, false, 0xFFC9CEDA);
        Draw.roundRect(ctx, cx - 9, by - 5, 18, 18, 9, Draw.alpha(accent, 0.85f));
        Draw.iconPause(ctx, cx - 3, by, 9, 0xFF101014);
        Draw.iconSkip(ctx, cx + 22, by, 9, true, 0xFFC9CEDA);

        if (f.lyrics() != null) {
            int y0 = H + GAP;
            Draw.card(ctx, 0, y0, W, LYRICS_H, 12, accent);
            String cur = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            String next = f.lyrics()[1].isEmpty() ? "" : f.lyrics()[2];
            Draw.text(ctx, font, cur, 11, y0 + 7, W - 22, 0xFFFFFFFF);
            Draw.text(ctx, font, next, 11, y0 + 19, W - 22, 0xFF8A90A0);
        }
    }
}
