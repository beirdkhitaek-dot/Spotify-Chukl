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
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;

/** Renders storage blocks through walls. */
public class StorageEsp extends Module {
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
        .description("Draws a line from the crosshair to each storage block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> chest = color("chest", 156, 91, 0);
    private final Setting<SettingColor> trappedChest = color("trapped-chest", 255, 205, 64);
    private final Setting<SettingColor> enderChest = color("ender-chest", 127, 91, 255);
    private final Setting<SettingColor> shulker = color("shulker-box", 236, 85, 204);
    private final Setting<SettingColor> spawner = color("spawner", 255, 76, 98);
    private final Setting<SettingColor> barrel = color("barrel", 232, 151, 70);
    private final Setting<SettingColor> trialSpawner = color("trial-spawner", 95, 188, 255);
    private final Setting<SettingColor> vault = color("vault", 151, 224, 88);
    private final Setting<SettingColor> furnace = color("furnace", 102, 168, 255);
    private final Setting<SettingColor> hopper = color("hopper", 0, 0, 255);
    private final Setting<SettingColor> dispenser = color("dispenser", 95, 188, 255);
    private final Setting<SettingColor> dropper = color("dropper", 118, 134, 162);
    private final Setting<SettingColor> bookshelf = color("chiseled-bookshelf", 189, 126, 255);
    private final Setting<SettingColor> brewingStand = color("brewing-stand", 249, 115, 140);
    private final Setting<SettingColor> crafter = color("crafter", 255, 195, 72);
    private final Setting<SettingColor> decoratedPot = color("decorated-pot", 223, 119, 92);

    private final Color side = new Color();
    private final Color line = new Color();

    public StorageEsp() {
        super(AddonTemplate.CATEGORY, "this-is-what-you-get-esp", "Renders storage blocks through walls.");
    }

    private Setting<SettingColor> color(String name, int r, int g, int b) {
        return sgColors.add(new ColorSetting.Builder()
            .name(name)
            .description("Color for " + name.replace('-', ' ') + ".")
            .defaultValue(new SettingColor(r, g, b, 255))
            .build()
        );
    }

    private SettingColor colorFor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return trappedChest.get();
        if (be instanceof ChestBlockEntity) return chest.get();
        if (be instanceof EnderChestBlockEntity) return enderChest.get();
        if (be instanceof ShulkerBoxBlockEntity) return shulker.get();
        if (be instanceof MobSpawnerBlockEntity) return spawner.get();
        if (be instanceof BarrelBlockEntity) return barrel.get();
        if (be instanceof TrialSpawnerBlockEntity) return trialSpawner.get();
        if (be instanceof VaultBlockEntity) return vault.get();
        if (be instanceof AbstractFurnaceBlockEntity) return furnace.get();
        if (be instanceof HopperBlockEntity) return hopper.get();
        if (be instanceof DropperBlockEntity) return dropper.get(); // before Dispenser: it is the subclass
        if (be instanceof DispenserBlockEntity) return dispenser.get();
        if (be instanceof ChiseledBookshelfBlockEntity) return bookshelf.get();
        if (be instanceof BrewingStandBlockEntity) return brewingStand.get();
        if (be instanceof CrafterBlockEntity) return crafter.get();
        if (be instanceof DecoratedPotBlockEntity) return decoratedPot.get();
        return null;
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        double max = maxDistance.get();

        for (BlockEntity be : Utils.blockEntities()) {
            SettingColor c = colorFor(be);
            if (c == null) continue;

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
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, cx, cy, cz, line);
            }
        }
    }
}
