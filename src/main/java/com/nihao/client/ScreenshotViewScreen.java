package com.nihao.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/** Full-size view of one screenshot: prev/next, mouse wheel zoom (towards the cursor), drag to pan. */
public class ScreenshotViewScreen extends MouseScreen {
    private static final ResourceLocation TEX = Compat.id("nihao", "screenshot_view");
    private static final double MAX_ZOOM = 32;

    private final Screen parent;
    private final File[] files;
    private int index;
    private int texW, texH;
    private boolean loaded;
    private double zoom = 1, offX, offY;

    public ScreenshotViewScreen(Screen parent, File[] files, int index) {
        super(Component.literal("Screenshots"));
        this.parent = parent;
        this.files = files;
        this.index = index;
    }

    @Override
    protected void init() {
        show();
        int y = height - 28;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> step(-1)).bounds(width / 2 - 110, y, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(width / 2 - 50, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> step(1)).bounds(width / 2 + 60, y, 50, 20).build());
    }

    private void step(int d) {
        if (files.length == 0) return;
        index = Math.floorMod(index + d, files.length);
        show();
    }

    private void show() {
        loaded = false;
        zoom = 1;
        offX = offY = 0;
        if (files.length == 0) return;
        try (InputStream in = Files.newInputStream(files[index].toPath())) {
            NativeImage img = NativeImage.read(in);
            texW = img.getWidth();
            texH = img.getHeight();
            minecraft.getTextureManager().register(TEX, Compat.dynamicTexture(img)); // replaces and closes the previous one
            loaded = true;
        } catch (Exception ignored) {
            // unreadable file: show the name only
        }
    }

    // Image area: below the title, above the buttons.
    private int areaTop() { return 20; }

    private int areaBottom() { return height - 34; }

    private double fitScale() {
        return Math.min((width - 40.0) / texW, (areaBottom() - areaTop() - 8.0) / texH);
    }

    private double centerY() { return (areaTop() + areaBottom()) / 2.0; }

    private boolean inArea(double y) { return y >= areaTop() && y <= areaBottom(); }

    private void clampOffset() {
        double s = fitScale() * zoom;
        offX = Math.max(-texW * s / 2, Math.min(texW * s / 2, offX));
        offY = Math.max(-texH * s / 2, Math.min(texH * s / 2, offY));
        if (zoom == 1) offX = offY = 0;
    }

    @Override
    boolean onScroll(double x, double y, double amount) {
        if (!loaded || amount == 0 || !inArea(y)) return false;
        double nz = Math.max(1, Math.min(MAX_ZOOM, zoom * (amount > 0 ? 1.25 : 0.8)));
        double k = nz / zoom - 1;
        offX -= (x - width / 2.0 - offX) * k;
        offY -= (y - centerY() - offY) * k;
        zoom = nz;
        clampOffset();
        return true;
    }

    @Override
    boolean onDrag(double x, double y, int button, double dx, double dy) {
        if (!loaded || button != 0 || zoom == 1 || !inArea(y)) return false;
        offX += dx;
        offY += dy;
        clampOffset();
        return true;
    }

    @Override
    boolean onClick(double x, double y, int button) {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        if (files.length == 0) {
            g.drawCenteredString(font, "No screenshots found", width / 2, height / 2, 0xFFFFFF);
            return;
        }
        String title = files[index].getName() + "  (" + (index + 1) + "/" + files.length + ")";
        if (zoom > 1) title += "  x" + String.format("%.1f", zoom);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        if (!loaded) return;
        double s = fitScale() * zoom;
        int w = (int) Math.round(texW * s), h = (int) Math.round(texH * s);
        int x = (int) Math.round(width / 2.0 + offX - w / 2.0), y = (int) Math.round(centerY() + offY - h / 2.0);
        g.enableScissor(0, areaTop(), width, areaBottom());
        Compat.blit(g, TEX, x, y, w, h, texW, texH);
        g.disableScissor();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        minecraft.getTextureManager().release(TEX);
    }
}
