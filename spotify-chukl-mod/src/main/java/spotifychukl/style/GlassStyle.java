package spotifychukl.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Style "glass": liquid-glass swap-box card. Frosted body, bright top rim, cover with soft glow, knob progress bar. */
public final class GlassStyle implements HudStyle {
    private static final int W = 172, CARD_H = 58, GAP = 3, LYRICS_H = 36;

    @Override public String id() { return "glass"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? CARD_H + GAP + LYRICS_H : CARD_H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int accent = f.accent();
        Draw.card(ctx, 0, 0, W, CARD_H, 10, accent);

        int tx = 12;
        if (f.texture() != null) {
            Draw.roundRect(ctx, 5, 5, 48, 48, 9, Draw.alpha(accent, 0.28f));
            Draw.cover(ctx, f.texture(), 8, 8, 42);
            tx = 62;
        }
        int maxW = W - tx - 10;
        Draw.text(ctx, font, f.title(), tx, 9, maxW, 0xFFFFFFFF);
        Draw.text(ctx, font, f.artist(), tx, 20, maxW, 0xFFB8BECC);

        long pos = f.durationMs() > 0 ? Math.min(f.durationMs(), Math.max(0, f.elapsedMs())) : 0;
        Draw.knobBar(ctx, tx, 37, maxW, f.durationMs() > 0 ? pos / (float) f.durationMs() : 0.08f, accent);
        if (f.durationMs() > 0) {
            String end = Draw.endTime(pos, f.durationMs());
            ctx.drawText(font, Draw.time(pos), tx, 45, 0xFFA3A9B8, false);
            ctx.drawText(font, end, tx + maxW - font.getWidth(end), 45, 0xFFA3A9B8, false);
        }

        if (f.lyrics() != null) {
            int y0 = CARD_H + GAP;
            Draw.card(ctx, 0, y0, W, LYRICS_H, 10, accent);
            String[] l = f.lyrics();
            Draw.text(ctx, font, l[0], 10, y0 + 6, W - 20, 0xFF8A90A0);
            Draw.text(ctx, font, l[1], 10, y0 + 16, W - 20, accent);
            Draw.text(ctx, font, l[2], 10, y0 + 26, W - 20, 0xFF8A90A0);
        }
    }
}
