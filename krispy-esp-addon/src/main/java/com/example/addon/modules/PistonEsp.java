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
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.util.math.BlockPos;

/** Highlights moving piston heads only (not regular pistons) through walls. */
public class PistonEsp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the boxes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<Integer> fillOpacity = sgGeneral.add(new IntSetting.Builder()
        .name("fill-opacity")
        .description("Opacity of the box fill.")
        .defaultValue(40)
        .range(0, 255)
        .sliderMax(255)
        .visible(() -> shapeMode.get() != ShapeMode.Lines)
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
        .description("Draws a line from the crosshair to each moving piston head.")
        .defaultValue(false)
        .build()
    );

    private final Setting<TracerOrigin> tracerOrigin = sgGeneral.add(new EnumSetting.Builder<TracerOrigin>()
        .name("tracer-origin")
        .description("Where tracers start: the crosshair or your body.")
        .defaultValue(TracerOrigin.Body)
        .visible(tracers::get)
        .build()
    );

    private final Setting<SettingColor> headColor = sgColors.add(new ColorSetting.Builder()
        .name("piston-head-color")
        .description("Color for moving piston heads.")
        .defaultValue(new SettingColor(0, 255, 0, 255))
        .build()
    );

    public enum TracerOrigin { Crosshair, Body }

    private final Color side = new Color();
    private final Color line = new Color();

    public PistonEsp() {
        super(AddonTemplate.CATEGORY, "piston-esp", "Highlights moving piston heads through walls.");
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        double max = maxDistance.get();
        SettingColor c = headColor.get();

        for (BlockEntity be : Utils.blockEntities()) {
            // Only moving pistons whose moving block is a piston head
            if (!(be instanceof PistonBlockEntity piston)) continue;
            if (!piston.getPushedBlock().isOf(Blocks.PISTON_HEAD)) continue;

            BlockPos pos = be.getPos();
            double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;

            if (max > 0 && PlayerUtils.squaredDistanceTo(cx, cy, cz) > max * max) continue;

            line.set(c);
            side.set(c);
            side.a = fillOpacity.get();

            event.renderer.box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                side, line, shapeMode.get(), 0
            );

            if (tracers.get()) {
                double ox, oy, oz;
                if (tracerOrigin.get() == TracerOrigin.Body) {
                    ox = mc.player.getX();
                    oy = mc.player.getY() + mc.player.getHeight() / 2;
                    oz = mc.player.getZ();
                } else {
                    ox = RenderUtils.center.x;
                    oy = RenderUtils.center.y;
                    oz = RenderUtils.center.z;
                }
                event.renderer.line(ox, oy, oz, cx, cy, cz, line);
            }
        }
    }
}
