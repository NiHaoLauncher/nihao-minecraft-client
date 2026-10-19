package com.nihao.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

/** Phone-style screenshot animation: white flash, the shot shrinks into the corner, stays a moment, slides out. */
public final class ScreenshotFx {
    private static final ResourceLocation TEX = Compat.id("nihao", "screenshot_preview");
    private static final long SHRINK = 350, HOLD = 2000, SLIDE = 300;
    private static final long FLASH = 220;
    private static final int PREVIEW_W = 96, MARGIN = 8;

    private static volatile long start;
    private static boolean ready;
    private static int texW, texH;

    private ScreenshotFx() {}

    /** Decodes a PNG downscaled to at most maxPx wide (null if unreadable). Safe off the render thread. */
    static NativeImage thumbnail(File f, int maxPx) {
        try (InputStream in = Files.newInputStream(f.toPath()); NativeImage src = NativeImage.read(in)) {
            float k = Math.min(1f, (float) maxPx / src.getWidth());
            NativeImage dst = new NativeImage(Math.max(1, Math.round(src.getWidth() * k)), Math.max(1, Math.round(src.getHeight() * k)), false);
            src.resizeSubRectTo(0, 0, src.getWidth(), src.getHeight(), dst);
            return dst;
        } catch (Exception e) {
            return null;
        }
    }

    /** Call on the render thread right after a screenshot was saved. */
    public static void trigger() {
        Minecraft mc = Minecraft.getInstance();
        start = System.currentTimeMillis();
        if (ready) mc.getTextureManager().release(TEX);
        ready = false;
        long mine = start;
        CompletableFuture.supplyAsync(() -> {
            File[] list = new File(mc.gameDirectory, "screenshots").listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
            if (list == null || list.length == 0) return null;
            return thumbnail(Arrays.stream(list).max(Comparator.comparingLong(File::lastModified)).get(), 512);
        }).thenAccept(img -> mc.execute(() -> {
            if (img == null) return;
            if (mine != start) { img.close(); return; } // a newer screenshot replaced this one
            texW = img.getWidth();
            texH = img.getHeight();
            mc.getTextureManager().register(TEX, Compat.dynamicTexture(img));
            ready = true;
        }));
    }

    /** HUD callback. */
    public static void render(GuiGraphics g) {
        long t = System.currentTimeMillis() - start;
        if (start == 0 || t > SHRINK + HOLD + SLIDE) return;
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();

        if (t < FLASH) {
            int alpha = (int) (200 * (1 - (double) t / FLASH));
            g.fill(0, 0, sw, sh, (alpha << 24) | 0xFFFFFF);
        }
        if (!ready) return;

        int pw = PREVIEW_W, ph = Math.max(1, pw * texH / texW);
        int tx = MARGIN, ty = sh - MARGIN - ph;
        double x, y, w, h;
        if (t < SHRINK) { // full screen -> corner, ease-out
            double e = 1 - Math.pow(1 - (double) t / SHRINK, 3);
            x = tx * e;
            y = ty * e;
            w = sw + (pw - sw) * e;
            h = sh + (ph - sh) * e;
        } else {
            w = pw;
            h = ph;
            y = ty;
            long out = t - SHRINK - HOLD;
            x = out > 0 ? tx - (double) out / SLIDE * (pw + MARGIN) : tx;
        }
        int ix = (int) Math.round(x), iy = (int) Math.round(y), iw = (int) Math.round(w), ih = (int) Math.round(h);
        g.fill(ix - 1, iy - 1, ix + iw + 1, iy + ih + 1, 0xFFFFFFFF);
        Compat.blit(g, TEX, ix, iy, iw, ih, texW, texH);
    }
}
