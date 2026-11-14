package spotifychukl.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Style "vinyl": a record (cover as the label) on a liquid glass card, with title, artist and a knob progress bar. */
public final class VinylStyle implements HudStyle {
    private static final int W = 172, H = 66, GAP = 3, LYRICS_H = 24;

    @Override public String id() { return "vinyl"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? H + GAP + LYRICS_H : H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int accent = Draw.spotify(f);
        Draw.card(ctx, 0, 0, W, H, 12, accent);

        int cx = 34, cy = 33;
        Draw.roundRect(ctx, cx - 28, cy - 28, 56, 56, 28, 0xFF0B0B0E); // record
        Draw.roundRect(ctx, cx - 23, cy - 23, 46, 46, 23, 0xFF15161B); // groove ring
        if (f.texture() != null) {
            String old = spotifychukl.Config.get().coverShape;
            spotifychukl.Config.get().coverShape = "circle"; // the label of a record is always round
            Draw.cover(ctx, f.texture(), cx - 17, cy - 17, 34);
            spotifychukl.Config.get().coverShape = old;
        } else {
            Draw.roundRect(ctx, cx - 17, cy - 17, 34, 34, 17, accent);
        }
        Draw.roundRect(ctx, cx - 3, cy - 3, 6, 6, 3, 0xFF101014); // spindle hole
        double a = System.currentTimeMillis() / 700.0;
        int sx = cx + (int) Math.round(Math.cos(a) * 25), sy = cy + (int) Math.round(Math.sin(a) * 25);
        ctx.fill(sx - 1, sy - 1, sx + 1, sy + 1, 0x70FFFFFF);

        int tx = 70, maxW = W - tx - 10;
        Draw.text(ctx, font, f.title(), tx, 11, maxW, 0xFFFFFFFF);
        Draw.text(ctx, font, f.artist(), tx, 22, maxW, 0xFFB8BECC);
        long pos = f.durationMs() > 0 ? Math.min(f.durationMs(), Math.max(0, f.elapsedMs())) : 0;
        Draw.knobBar(ctx, tx, 41, maxW, f.durationMs() > 0 ? pos / (float) f.durationMs() : 0.08f, accent);
        if (f.durationMs() > 0) {
            String end = Draw.endTime(pos, f.durationMs());
            ctx.drawText(font, Draw.time(pos), tx, 49, 0xFFA3A9B8, false);
            ctx.drawText(font, end, tx + maxW - font.getWidth(end), 49, 0xFFA3A9B8, false);
        }

        if (f.lyrics() != null) {
            int y0 = H + GAP;
            Draw.card(ctx, 0, y0, W, LYRICS_H, 12, accent);
            String line = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            Draw.text(ctx, font, line, 9, y0 + 8, W - 18, 0xFFFFFFFF);
        }
    }
}
