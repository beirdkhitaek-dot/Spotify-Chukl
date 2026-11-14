package spotifychukl.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Style "mini": small glass pill with a round cover, one "title - artist" line and a thin glowing progress line. */
public final class MiniStyle implements HudStyle {
    private static final int W = 172, H = 28;

    @Override public String id() { return "mini"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int accent = Draw.spotify(f);
        Draw.card(ctx, 0, 0, W, H, 14, accent);
        int tx = 12;
        if (f.texture() != null) {
            String old = spotifychukl.Config.get().coverShape;
            spotifychukl.Config.get().coverShape = "circle";
            Draw.cover(ctx, f.texture(), 4, 4, 20);
            spotifychukl.Config.get().coverShape = old;
            tx = 31;
        }
        int maxW = W - tx - 11;
        Draw.text(ctx, font, f.title() + " - " + f.artist(), tx, 6, maxW, 0xFFFFFFFF);
        Draw.roundRect(ctx, tx, 18, maxW, 2, 1, 0x44FFFFFF);
        if (f.durationMs() > 0) {
            long pos = Math.min(f.durationMs(), Math.max(0, f.elapsedMs()));
            int fill = Math.max(2, (int) (maxW * pos / f.durationMs()));
            Draw.roundRect(ctx, tx, 17, fill, 4, 2, Draw.alpha(accent, 0.30f));
            Draw.roundRect(ctx, tx, 18, fill, 2, 1, accent);
        }
    }
}
