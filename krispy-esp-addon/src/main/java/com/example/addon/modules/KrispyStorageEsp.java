package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Krispy-style storage ESP: soft filled boxes with a glow, per-storage toggles and colors. */
public class KrispyStorageEsp extends Module {
    private static final int GLOW_LAYERS = 4;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgGlow = settings.createGroup("Glow");
    private final SettingGroup sgStorages = settings.createGroup("Storages");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Integer> alpha = sgGeneral.add(new IntSetting.Builder()
        .name("alpha")
        .description("Opacity of the box fill.")
        .defaultValue(53)
        .range(0, 255)
        .sliderMax(255)
        .build()
    );

    private final Setting<Double> boxSize = sgGeneral.add(new DoubleSetting.Builder()
        .name("box-size")
        .description("Size of each box. 1 is the full block, smaller shrinks it, bigger makes it larger.")
        .defaultValue(0.84)
        .range(0.2, 2.0)
        .sliderRange(0.2, 1.5)
        .build()
    );

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Maximum render distance in blocks. 0 means unlimited.")
        .defaultValue(0)
        .min(0)
        .sliderMax(256)
        .build()
    );

    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Draws a line to each storage block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<PistonEsp.TracerOrigin> tracerOrigin = sgGeneral.add(new EnumSetting.Builder<PistonEsp.TracerOrigin>()
        .name("tracer-origin")
        .description("Where tracers start: the crosshair or your body.")
        .defaultValue(PistonEsp.TracerOrigin.Body)
        .visible(tracers::get)
        .build()
    );

    private final Setting<Boolean> glow = sgGlow.add(new BoolSetting.Builder()
        .name("glow")
        .description("Soft glow around each box.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> glowStrength = sgGlow.add(new IntSetting.Builder()
        .name("glow-strength")
        .description("How strong the glow is.")
        .defaultValue(65)
        .range(0, 100)
        .sliderMax(100)
        .visible(glow::get)
        .build()
    );

    private final Setting<Integer> glowSpread = sgGlow.add(new IntSetting.Builder()
        .name("glow-spread")
        .description("How far the glow reaches.")
        .defaultValue(18)
        .range(6, 28)
        .sliderMax(28)
        .visible(glow::get)
        .build()
    );

    private static class Entry {
        final String id;
        final java.util.function.Predicate<BlockEntity> test;
        final Setting<Boolean> enabled;
        final Setting<SettingColor> color;

        Entry(String id, java.util.function.Predicate<BlockEntity> test, Setting<Boolean> enabled, Setting<SettingColor> color) {
            this.id = id;
            this.test = test;
            this.enabled = enabled;
            this.color = color;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Color side = new Color();
    private final Color line = new Color();

    public KrispyStorageEsp() {
        super(AddonTemplate.CATEGORY, "krispy-storage-esp", "Krispy-style storage ESP with a soft glow.");

        add("chest", Blocks.CHEST, 156, 91, 0);
        add("trapped-chest", Blocks.TRAPPED_CHEST, 255, 205, 64);
        add("ender-chest", Blocks.ENDER_CHEST, 127, 91, 255);
        add("shulker-box", be -> be instanceof ShulkerBoxBlockEntity, 236, 85, 204);
        add("spawner", Blocks.SPAWNER, 255, 76, 98);
        add("barrel", Blocks.BARREL, 232, 151, 70);
        add("trial-spawner", Blocks.TRIAL_SPAWNER, 95, 188, 255);
        add("vault", Blocks.VAULT, 151, 224, 88);
        add("furnace", Blocks.FURNACE, 102, 168, 255);
        add("blast-furnace", Blocks.BLAST_FURNACE, 102, 168, 255);
        add("smoker", Blocks.SMOKER, 102, 168, 255);
        add("hopper", Blocks.HOPPER, 0, 0, 255);
        add("dispenser", Blocks.DISPENSER, 95, 188, 255);
        add("dropper", Blocks.DROPPER, 118, 134, 162);
        add("chiseled-bookshelf", Blocks.CHISELED_BOOKSHELF, 189, 126, 255);
        add("brewing-stand", Blocks.BREWING_STAND, 249, 115, 140);
        add("crafter", Blocks.CRAFTER, 255, 195, 72);
        add("decorated-pot", Blocks.DECORATED_POT, 223, 119, 92);
    }

    private void add(String id, Block block, int r, int g, int b) {
        add(id, be -> be.getCachedState().isOf(block), r, g, b);
    }

    private void add(String id, java.util.function.Predicate<BlockEntity> test, int r, int g, int b) {
        String label = id.replace('-', ' ');
        Setting<Boolean> enabled = sgStorages.add(new BoolSetting.Builder()
            .name(id)
            .description("Highlight " + label + ".")
            .defaultValue(true)
            .build()
        );
        Setting<SettingColor> color = sgColors.add(new ColorSetting.Builder()
            .name(id + "-color")
            .description("Color for " + label + ".")
            .defaultValue(new SettingColor(r, g, b, 255))
            .build()
        );
        entries.add(new Entry(id, test, enabled, color));
    }

    private Entry match(BlockEntity be) {
        for (Entry e : entries) {
            if (e.enabled.get() && e.test.test(be)) return e;
        }
        return null;
    }

    private void fill(Render3DEvent event, double x, double y, double z, double grow, SettingColor c, int a) {
        if (a <= 0) return;
        side.set(c);
        side.a = Math.min(255, a);
        double half = boxSize.get() / 2.0 + grow;
        event.renderer.box(
            x + 0.5 - half, y + 0.5 - half, z + 0.5 - half,
            x + 0.5 + half, y + 0.5 + half, z + 0.5 + half,
            side, side, ShapeMode.Sides, 0
        );
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        double max = maxDistance.get();
        boolean doGlow = glow.get() && glowStrength.get() > 0;

        for (BlockEntity be : Utils.blockEntities()) {
            Entry e = match(be);
            if (e == null) continue;

            BlockPos pos = be.getPos();
            double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
            if (max > 0 && PlayerUtils.squaredDistanceTo(cx, cy, cz) > max * max) continue;

            SettingColor c = e.color.get();

            // Glow: stacked, slightly larger translucent layers fading outwards
            if (doGlow) {
                double reach = glowSpread.get() / 100.0;
                double strength = glowStrength.get() / 100.0;
                for (int i = GLOW_LAYERS; i >= 1; i--) {
                    double t = (double) i / GLOW_LAYERS;
                    double grow = reach * t;
                    int a = (int) Math.round(120 * strength * (1.0 - t * 0.8) / GLOW_LAYERS * 2);
                    fill(event, pos.getX(), pos.getY(), pos.getZ(), grow, c, a);
                }
            }

            fill(event, pos.getX(), pos.getY(), pos.getZ(), 0, c, alpha.get());

            if (tracers.get()) {
                double ox, oy, oz;
                if (tracerOrigin.get() == PistonEsp.TracerOrigin.Body) {
                    ox = mc.player.getX();
                    oy = mc.player.getY() + mc.player.getHeight() / 2;
                    oz = mc.player.getZ();
                } else {
                    ox = RenderUtils.center.x;
                    oy = RenderUtils.center.y;
                    oz = RenderUtils.center.z;
                }
                line.set(c);
                line.a = 255;
                event.renderer.line(ox, oy, oz, cx, cy, cz, line);
            }
        }
    }
}
