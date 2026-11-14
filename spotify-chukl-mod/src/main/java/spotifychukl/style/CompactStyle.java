package spotifychukl.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Style "compact": one small glass chip with "title - artist". The lightest option. */
public final class CompactStyle implements HudStyle {
    private static final int W = 200, H = 14, LYRICS_H = 14;

    @Override public String id() { return "compact"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? H + 2 + LYRICS_H : H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int accent = Draw.spotify(f);
        String s = f.title() + " - " + f.artist();
        int tw = Math.min(W - 18, font.getWidth(s));
        Draw.card(ctx, 0, 0, tw + 18, H, 7, accent);
        ctx.fill(5, 4, 6, H - 4, accent);
        Draw.text(ctx, font, s, 10, 3, W - 18, 0xFFFFFFFF);
        if (f.lyrics() != null) {
            String line = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            int lw = Math.min(W - 18, font.getWidth(line));
            if (lw > 0) {
                Draw.card(ctx, 0, H + 2, lw + 18, LYRICS_H, 7, accent);
                Draw.text(ctx, font, line, 9, H + 5, W - 18, accent);
            }
        }
    }
}
