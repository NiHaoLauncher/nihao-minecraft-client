package com.nihao.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

/** Grid of screenshot thumbnails (newest first), paged; click opens the viewer, right click the context menu. */
public class ScreenshotsScreen extends MouseScreen {
    private static final int CELL_W = 120, CELL_H = 68, GAP = 8, THUMB_PX = 256;
    private static final String[] ITEMS = {"open", "download", "copy", "favourite_add", "delete"}; // translation keys: screenshots.<item>
    private static boolean onlyFavourites; // the "Favourites" folder: show only the starred screenshots
    private static final int ITEM_H = 14;

    private static final class Thumb {
        final ResourceLocation id;
        int w, h;
        volatile boolean ready;
        Thumb(ResourceLocation id) { this.id = id; }
    }

    private final Screen parent;
    private File[] files = new File[0];
    private Thumb[] thumbs = new Thumb[0];
    private int page, cols, perPage, gridX;
    private int generation; // invalidates thumbnails still loading for a previous page

    private int menuCell = -1, menuX, menuY;
    private boolean confirmDelete;
    private volatile String status = "";
    private volatile long statusUntil;

    public ScreenshotsScreen(Screen parent) {
        super(Component.translatable("nihao.screenshots.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        closeMenu();
        File[] list = new File(minecraft.gameDirectory, "screenshots").listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
        files = list == null ? new File[0] : list;
        if (onlyFavourites) files = Arrays.stream(files).filter(f -> Favourites.has("shot", f.getName())).toArray(File[]::new);
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        cols = Math.max(1, (width - 20) / (CELL_W + GAP));
        int rows = Math.max(1, (height - 70) / (CELL_H + GAP));
        perPage = cols * rows;
        gridX = (width - (cols * (CELL_W + GAP) - GAP)) / 2;
        page = Math.min(page, Math.max(0, (files.length - 1) / perPage));

        int first = page * perPage, n = Math.max(0, Math.min(perPage, files.length - first));
        releaseThumbs();
        thumbs = new Thumb[n];
        int gen = ++generation;
        for (int i = 0; i < n; i++) {
            int fileIndex = first + i;
            thumbs[i] = new Thumb(Compat.id("nihao", "thumb/" + i));
            load(thumbs[i], files[fileIndex], gen);
            addRenderableWidget(new Cell(thumbs[i], cellX(i), cellY(i), Favourites.has("shot", files[fileIndex].getName())));
        }

        int y = height - 28, pages = Math.max(1, (files.length + perPage - 1) / perPage);
        addRenderableWidget(new FlatButton(width / 2 - 110, y, 50, 20, "<", () -> turn(-1, pages)));
        addRenderableWidget(new FlatButton(width / 2 - 50, y, 100, 20, Lang.t("back"), this::onClose));
        addRenderableWidget(new FlatButton(width / 2 + 60, y, 50, 20, ">", () -> turn(1, pages)));
        FlatButton folder = new FlatButton(width / 2 + 120, y, 110, 20, Lang.t(onlyFavourites ? "screenshots.folder_favourites" : "screenshots.folder_all"), () -> {
            onlyFavourites = !onlyFavourites;
            page = 0;
            rebuildWidgets();
        });
        folder.selected = onlyFavourites;
        addRenderableWidget(folder);
    }

    private int cellX(int i) { return gridX + (i % cols) * (CELL_W + GAP); }

    private int cellY(int i) { return 24 + (i / cols) * (CELL_H + GAP); }

    private void turn(int d, int pages) {
        page = Math.floorMod(page + d, pages);
        rebuildWidgets();
    }

    private void open(int fileIndex) {
        minecraft.setScreen(new ScreenshotViewScreen(this, files, fileIndex));
    }

    /** Decodes and downsizes off-thread, then uploads the texture on the render thread. */
    private void load(Thumb t, File f, int gen) {
        CompletableFuture.supplyAsync(() -> {
            return ScreenshotFx.thumbnail(f, THUMB_PX); // null for an unreadable file: empty cell
        }).thenAccept(img -> minecraft.execute(() -> {
            if (img == null) return;
            if (gen != generation) { img.close(); return; }
            t.w = img.getWidth();
            t.h = img.getHeight();
            minecraft.getTextureManager().register(t.id, Compat.dynamicTexture(img));
            t.ready = true;
        }));
    }

    private void releaseThumbs() {
        for (Thumb t : thumbs) if (t.ready) minecraft.getTextureManager().release(t.id);
    }

    /** Thumbnail: just the image and a hover outline (clicks are handled in onClick). Not a Button: 1.21.11 forces the button sprite. */
    private static final class Cell extends AbstractWidget {
        private final Thumb thumb;
        private final boolean favourite;

        Cell(Thumb thumb, int x, int y, boolean favourite) {
            super(x, y, CELL_W, CELL_H, Component.empty());
            this.thumb = thumb;
            this.favourite = favourite;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {}

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            if (thumb.ready) {
                float k = Math.min((float) getWidth() / thumb.w, (float) getHeight() / thumb.h);
                int w = Math.round(thumb.w * k), h = Math.round(thumb.h * k);
                Compat.blit(g, thumb.id, getX() + (getWidth() - w) / 2, getY() + (getHeight() - h) / 2, w, h, thumb.w, thumb.h);
            }
            if (favourite) Favourites.drawStar(g, getX() + 3, getY() + 3);
            if (isHoveredOrFocused()) {
                int x1 = getX(), y1 = getY(), x2 = x1 + getWidth(), y2 = y1 + getHeight(), c = 0xFFFFFFFF;
                g.fill(x1 - 1, y1 - 1, x2 + 1, y1, c);
                g.fill(x1 - 1, y2, x2 + 1, y2 + 1, c);
                g.fill(x1 - 1, y1, x1, y2, c);
                g.fill(x2, y1, x2 + 1, y2, c);
            }
        }
    }

    // ---- context menu (right click on a thumbnail) ----

    private int cellAt(double x, double y) {
        for (int i = 0; i < thumbs.length; i++) {
            if (x >= cellX(i) && x < cellX(i) + CELL_W && y >= cellY(i) && y < cellY(i) + CELL_H) return i;
        }
        return -1;
    }

    /** Wide enough for the longest label in the current language. */
    private int menuW() {
        int w = 110;
        for (String k : new String[]{"open", "download", "copy", "favourite_add", "favourite_remove", "delete", "delete_confirm"}) w = Math.max(w, font.width(Lang.t("screenshots." + k)) + 8);
        return w;
    }

    private int menuItemAt(double x, double y) {
        if (x < menuX || x >= menuX + menuW() || y < menuY) return -1;
        int i = (int) (y - menuY) / ITEM_H;
        return i < ITEMS.length ? i : -1;
    }

    private void closeMenu() {
        menuCell = -1;
        confirmDelete = false;
    }

    @Override
    boolean onClick(double x, double y, int button) {
        if (menuCell < 0 && Ui.click(this, x, y, button)) return true;
        if (menuCell >= 0) {
            int item = menuItemAt(x, y);
            if (item == 4 && !confirmDelete && button == 0) { // Delete needs a second click
                confirmDelete = true;
                return true;
            }
            int fileIndex = page * perPage + menuCell;
            closeMenu();
            if (item >= 0 && button == 0) run(item, files[fileIndex], fileIndex);
            return true; // a click outside only closes the menu
        }
        if (button == 0) {
            int c = cellAt(x, y);
            if (c >= 0) {
                open(page * perPage + c);
                return true;
            }
        }
        if (button == 1) {
            int c = cellAt(x, y);
            if (c >= 0) {
                menuCell = c;
                menuX = (int) Math.min(x, width - menuW());
                menuY = (int) Math.min(y, height - ITEMS.length * ITEM_H);
                return true;
            }
        }
        return false;
    }

    @Override
    boolean onDrag(double x, double y, int button, double dx, double dy) {
        return false;
    }

    @Override
    boolean onScroll(double x, double y, double amount) {
        return false;
    }

    private void say(String text) {
        status = text;
        statusUntil = System.currentTimeMillis() + 3000;
    }

    private void run(int item, File f, int fileIndex) {
        switch (item) {
            case 0 -> open(fileIndex);
            case 1 -> CompletableFuture.runAsync(() -> { // native "Save as" dialog; blocks, so off the render thread
                try {
                    String dest = SaveDialog.png(f.getName());
                    if (dest == null) return; // cancelled
                    if (!dest.toLowerCase().endsWith(".png")) dest += ".png";
                    Files.copy(f.toPath(), Path.of(dest), StandardCopyOption.REPLACE_EXISTING);
                    say(Lang.t("screenshots.saved", dest));
                } catch (Throwable e) {
                    say(Lang.t("screenshots.save_failed", e.getMessage()));
                }
            });
            case 2 -> CompletableFuture.runAsync(() -> {
                try {
                    BufferedImage img = ImageIO.read(f);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(img), null);
                    say(Lang.t("screenshots.copied"));
                } catch (Throwable t) { // headless AWT or no clipboard: fall back to the file path
                    minecraft.execute(() -> minecraft.keyboardHandler.setClipboard(f.getAbsolutePath()));
                    say(Lang.t("screenshots.copied_path"));
                }
            });
            case 3 -> {
                say(Lang.t(Favourites.toggle("shot", f.getName()) ? "screenshots.favourite_added" : "screenshots.favourite_removed"));
                rebuildWidgets();
            }
            case 4 -> {
                try {
                    Files.deleteIfExists(f.toPath());
                    say(Lang.t("screenshots.deleted", f.getName()));
                    rebuildWidgets();
                } catch (Exception e) {
                    say(Lang.t("screenshots.delete_failed", e.getMessage()));
                }
            }
            default -> { }
        }
    }

    private record ImageSelection(Image img) implements Transferable {
        public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.imageFlavor}; }

        public boolean isDataFlavorSupported(DataFlavor f) { return DataFlavor.imageFlavor.equals(f); }

        public Object getTransferData(DataFlavor f) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(f)) throw new UnsupportedFlavorException(f);
            return img;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // widgets ignore hover while the menu is open
        super.render(g, menuCell >= 0 ? -1 : mouseX, menuCell >= 0 ? -1 : mouseY, partial);
        if (files.length == 0) {
            g.drawCenteredString(font, Lang.t(onlyFavourites ? "screenshots.empty_favourites" : "screenshots.empty"), width / 2, height / 2, 0xFFFFFFFF);
            return;
        }
        int pages = (files.length + perPage - 1) / perPage;
        g.drawCenteredString(font, Lang.t("screenshots.heading", files.length, page + 1, pages), width / 2, 8, 0xFFFFFFFF);
        if (System.currentTimeMillis() < statusUntil) g.drawCenteredString(font, status, width / 2, height - 40, 0xFFFFFFAA);
        if (menuCell < 0) return;
        g.fill(menuX - 1, menuY - 1, menuX + menuW() + 1, menuY + ITEMS.length * ITEM_H + 1, 0xFF000000);
        int hover = menuItemAt(mouseX, mouseY);
        for (int i = 0; i < ITEMS.length; i++) {
            int y = menuY + i * ITEM_H;
            g.fill(menuX, y, menuX + menuW(), y + ITEM_H, i == hover ? 0xFF555577 : 0xFF222222);
            String label = Lang.t(i == 4 && confirmDelete ? "screenshots.delete_confirm" : "screenshots." + ITEMS[i]);
            if (i == 3) label = Lang.t(Favourites.has("shot", files[page * perPage + menuCell].getName()) ? "screenshots.favourite_remove" : "screenshots.favourite_add");
            g.drawString(font, label, menuX + 4, y + 3, i == 4 ? 0xFFFF6666 : 0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        generation++;
        releaseThumbs();
    }
}
