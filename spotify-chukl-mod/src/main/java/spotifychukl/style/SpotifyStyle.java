package spotifychukl.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Style "spotify": green-tinted liquid glass card, cover on the left, animated bars, knob progress bar. */
public final class SpotifyStyle implements HudStyle {
    private static final int W = 180, H = 60, GAP = 3, LYRICS_H = 24;

    @Override public String id() { return "spotify"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? H + GAP + LYRICS_H : H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int green = Draw.spotify(f);
        Draw.shadow(ctx, 0, 0, W, H, 10);
        Draw.glass(ctx, 0, 0, W, H, 10, Draw.bg(Draw.mix(0xCC1F3A2D, green, 0.18f)), Draw.bg(Draw.mix(0xCC0E1913, green, 0.10f)),
            Draw.mix(Draw.GLASS_RIM, green, 0.45f));

        int tx = 12;
        if (f.texture() != null) {
            Draw.roundRect(ctx, 5, 5, 50, 50, 9, Draw.alpha(green, 0.25f));
            Draw.cover(ctx, f.texture(), 8, 8, 44);
            tx = 62;
        }
        int maxW = W - tx - 24;
        Draw.text(ctx, font, f.title(), tx, 9, maxW, 0xFFFFFFFF);
        Draw.text(ctx, font, f.artist(), tx, 20, maxW, 0xFFB9CFC4);

        long t = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            int h = 3 + (int) Math.round(5 * (0.5 + 0.5 * Math.sin(t / 170.0 + i * 1.9)));
            int bx = W - 18 + i * 4;
            Draw.roundRect(ctx, bx, 17 - h, 2, h, 1, green);
        }

        int barW = W - tx - 10;
        long pos = f.durationMs() > 0 ? Math.min(f.durationMs(), Math.max(0, f.elapsedMs())) : 0;
        Draw.knobBar(ctx, tx, 39, barW, f.durationMs() > 0 ? pos / (float) f.durationMs() : 0.08f, green);
        if (f.durationMs() > 0) {
            String end = Draw.endTime(pos, f.durationMs());
            ctx.drawText(font, Draw.time(pos), tx, 47, 0xFFA8BFB3, false);
            ctx.drawText(font, end, tx + barW - font.getWidth(end), 47, 0xFFA8BFB3, false);
        }

        if (f.lyrics() != null) {
            int y0 = H + GAP;
            Draw.shadow(ctx, 0, y0, W, LYRICS_H, 10);
            Draw.glass(ctx, 0, y0, W, LYRICS_H, 10, Draw.bg(Draw.mix(0xCC1F3A2D, green, 0.18f)), Draw.bg(Draw.mix(0xCC0E1913, green, 0.10f)),
                Draw.mix(Draw.GLASS_RIM, green, 0.45f));
            String line = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            Draw.text(ctx, font, line, 9, y0 + 8, W - 18, 0xFFFFFFFF);
        }
    }
}
