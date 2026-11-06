package com.nihao.client;

//#if ZOOM
import com.mojang.blaze3d.platform.InputConstants;
//#if MC < V1_21_9
import com.nihao.client.mixin.KeyMappingAccessor;
//#endif
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Hold-to-zoom: a smooth field-of-view multiplier, the mouse wheel changes how far. Versions with the `zoom` feature flag only. */
public final class Zoom {
//#if MC >= V1_21_9
    // 1.21.9+ registers a category itself (label key.category.nihao.main), no sort-order hack needed
    public static final KeyMapping KEY = new KeyMapping("key.nihao.zoom", InputConstants.KEY_Z, KeyMapping.Category.register(Compat.id("nihao", "main"))); // C is taken by "Save Hotbar Activator"
//#else
    private static final String CATEGORY = "key.categories.nihao";
    public static final KeyMapping KEY = key();
//#endif
    private static final double DEFAULT = 0.3, MIN = 0.04, MAX = 0.9;
    private static double target = DEFAULT, current = 1;
    private static long last = System.nanoTime();
    private static boolean hidHud; // the zoom turned the HUD off

    private Zoom() {}

//#if MC < V1_21_9
    /** Vanilla does not know a new category: it has to be added to its sort order, or the Controls screen crashes. */
    private static KeyMapping key() {
        var order = KeyMappingAccessor.nihao$order();
        order.putIfAbsent(CATEGORY, order.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1);
        return new KeyMapping("key.nihao.zoom", InputConstants.KEY_Z, CATEGORY); // C is taken by "Save Hotbar Activator"
    }

//#endif
    private static boolean held() {
        return NiHaoConfig.zoom && Minecraft.getInstance().screen == null && KEY.isDown();
    }

    private static boolean hudHidden() {
//#if MC >= V26_2
        return Minecraft.getInstance().gui.hud.isHidden();
//#else
        return Minecraft.getInstance().options.hideGui;
//#endif
    }

    private static void setHudHidden(boolean hidden) {
//#if MC >= V26_2
        var hud = Minecraft.getInstance().gui.hud;
        if (hud.isHidden() != hidden) hud.toggle(); // 26.x has no setter
//#else
        Minecraft.getInstance().options.hideGui = hidden;
//#endif
    }

    /** The HUD (hotbar, health, crosshair...) is off while zooming, like F1; a HUD the player had already hidden is left alone. */
    private static void hud(boolean zooming) {
        if (zooming && !hudHidden()) {
            setHudHidden(true);
            hidHud = true;
        } else if (!zooming && hidHud) {
            setHudHidden(false);
            hidHud = false;
        }
    }

    /** Multiplier for this frame's field of view (1 = no zoom). */
    public static double factor() {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - last) / 1e9);
        last = now;
        boolean on = held();
        hud(on);
        if (!on) target = DEFAULT;
        current += ((on ? target : 1) - current) * Math.min(1, dt * 14);
        if (Math.abs(current - 1) < 0.002) current = 1;
        return current;
    }

    /** The multiplier of the last frame, for the hand: it zooms with the world. Does not advance the smoothing. */
    public static double scale() {
        return current;
    }

    /** The mouse wheel while zooming; true = consumed (the hotbar does not scroll). */
    public static boolean scroll(double delta) {
        if (!held() || delta == 0) return false;
        target = Math.max(MIN, Math.min(MAX, target * (delta > 0 ? 0.85 : 1 / 0.85)));
        return true;
    }
}
//#endif
