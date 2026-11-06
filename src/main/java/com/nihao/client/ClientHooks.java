package com.nihao.client;

import com.nihao.client.mixin.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

/** Everything the mod adds to vanilla screens. Called from the mixins, so it is the same for every loader. */
public final class ClientHooks {
    private static final ResourceLocation ICON = Compat.id("nihao", "textures/gui/screenshot.png");
    private static final int X = 4, Y = 4, PROFILE_H = 24, GAP = 4;

    private static final java.util.Map<AbstractWidget, Integer> shifted = new java.util.HashMap<>(); // Options widgets moved down, with their own y
    private static final java.util.List<AbstractWidget> optionsRow = new java.util.ArrayList<>(); // the widgets of the row the button took
    private static int optionsRowY;
    private static java.util.List<Integer> rowsY = java.util.List.of();
    private static int pitch = 24, rowH = 20;
    private static NiButton optionsBtn;
    private static NiButton screenshotsBtn, updateBtn;

    private ClientHooks() {}

    /** Minecraft.setScreen finished: the new screen has been initialised. */
    public static void onSetScreen(Screen screen) {
        if (screen instanceof JoinMultiplayerScreen) ServerAuth.refresh();
        if (screen instanceof TitleScreen) {
            Updater.Release applied = Updater.takeApplied();
            if (applied != null) Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new ChangelogScreen(screen, applied.tag(), applied.notes())));
        }
        onInit(screen);
    }

    /** Adds the mod's buttons; also called after a rebuild (resize), when vanilla has cleared the widgets. */
    public static void onInit(Screen screen) {
        if (screen instanceof OptionsScreen) {
            addOptionsButton(screen);
            return;
        }
        if (!(screen instanceof PauseScreen || screen instanceof TitleScreen)) return;
        if (screenshotsBtn != null && screen.children().contains(screenshotsBtn)) return; // already added
        Minecraft mc = Minecraft.getInstance();
        ScreenAccessor acc = (ScreenAccessor) screen;
        // The mod's buttons sit in a row below the profile frame in the top-left corner.
        int x = X, y = Y + PROFILE_H + GAP;
        screenshotsBtn = new NiButton(x, y, 20, 20, "", ICON, Lang.t("screenshots.title"), () -> mc.setScreen(new ScreenshotsScreen(screen)));
        acc.nihao$addRenderableWidget(screenshotsBtn);
        updateBtn = null;
        if (screen instanceof TitleScreen) {
            // Update banner: hidden until the background check finds a newer release; clicking downloads it (applied on exit).
            updateBtn = new NiButton(screen.width / 2 - 150, screen.height - 32, 300, 20, "", null, null, Updater::download);
            updateBtn.visible = false;
            acc.nihao$addRenderableWidget(updateBtn);
        }
    }

    /** A NiHaoClient button between the first rows of the Options screen and "Skin Customization...". */
    private static void addOptionsButton(Screen screen) {
        AbstractWidget skin = null;
        for (var c : screen.children()) {
            if (c instanceof NiButton) return; // already added
            if (c instanceof AbstractWidget w && w.getMessage().getContents() instanceof TranslatableContents t && t.getKey().equals("options.skinCustomisation")) skin = w;
        }
        if (skin == null) return;
        shifted.clear();
        int rowY = skin.getY(), minX = Integer.MAX_VALUE, maxX = 0;
        for (var c : screen.children()) {
            if (!(c instanceof AbstractWidget w) || w.getY() < rowY || w.getY() > screen.height - 36) continue; // the footer (Done) stays
            if (w.getY() == rowY) {
                minX = Math.min(minX, w.getX());
                maxX = Math.max(maxX, w.getX() + w.getWidth());
            }
            shifted.put(w, w.getY()); // moved down in onRender (the layout puts them back whenever it repositions)
        }
        Minecraft mc = Minecraft.getInstance();
        int bottom = shifted.keySet().stream().mapToInt(w -> shifted.get(w) + w.getHeight()).max().orElse(rowY);
        int footerTop = screen.height - 33;
        for (var c : screen.children()) if (c instanceof AbstractWidget w && w.getY() > screen.height - 36) footerTop = Math.min(footerTop, w.getY());
        // rows (distinct y) from the skin row down, plus the new one on top; on a small window they are pulled closer so "Done" stays free
        rowsY = shifted.values().stream().distinct().sorted().toList();
        int n = rowsY.size() + 1;
        pitch = Math.min(24, (footerTop - 4 - rowY - 20) / Math.max(1, n - 1));
        rowH = Math.max(12, Math.min(20, pitch - 2));
        optionsRow.clear();
        optionsRow.addAll(shifted.keySet().stream().filter(w -> shifted.get(w) == rowY).toList());
        optionsBtn = new NiButton(minX, rowY, maxX - minX, rowH, "NiHaoClient...", null, null, () -> mc.setScreen(new NiHaoSettingsScreen(screen)));
        optionsRowY = rowY;
        ((ScreenAccessor) screen).nihao$addRenderableWidget(optionsBtn);
    }

    /** Screen.render finished. */
    public static void onRender(Screen screen, GuiGraphics g) {
        if (screen instanceof OptionsScreen && optionsBtn != null) {
            shifted.forEach((w, y) -> {
                w.setY(optionsRowY + (rowsY.indexOf(y) + 1) * pitch);
                w.setHeight(rowH);
            });
            // the layout may have moved the row sideways (window resized) since the button was made
            int minX = optionsRow.stream().mapToInt(AbstractWidget::getX).min().orElse(optionsBtn.getX());
            int maxX = optionsRow.stream().mapToInt(w -> w.getX() + w.getWidth()).max().orElse(minX + optionsBtn.getWidth());
            optionsBtn.setX(minX);
            optionsBtn.setWidth(maxX - minX);
            optionsBtn.setY(optionsRowY);
            optionsBtn.setHeight(rowH);
        }
        if (!(screen instanceof PauseScreen || screen instanceof TitleScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        User u = mc.getUser();
        String type = Session.type();
        int width = 28 + Math.max(mc.font.width(u.getName()), 11 + mc.font.width(type)) + 6;
        Ui.panel(g, X + 1, Y + 1, X + width - 1, Y + PROFILE_H - 1);
        PlayerSkin skin = ElySkins.apply(u.getName(), DefaultPlayerSkin.get(u.getProfileId()));
        ResourceLocation tex = Compat.skinTexture(skin);
        Compat.blitUv(g, tex, X + 4, Y + 4, 16, 16, 8, 8, 8, 8, 64, 64); // face
        Compat.blitUv(g, tex, X + 4, Y + 4, 16, 16, 40, 8, 8, 8, 64, 64); // hat layer
        g.drawString(mc.font, u.getName(), X + 24, Y + 4, 0xFFFFFFFF);
        ServerAuth.icon(g, type, X + 24, Y + 13, 9);
        g.drawString(mc.font, type, X + 35, Y + 13, 0xFFAAAAAA);

        Updater.Release r = Updater.available;
        if (updateBtn == null) return;
        updateBtn.visible = r != null;
        if (r == null) return;
        String st = Updater.state;
        updateBtn.active = !st.equals("downloading") && !st.equals("ready");
        updateBtn.setMessage(Component.literal(switch (st) {
            case "downloading" -> Lang.t("update.downloading");
            case "ready" -> Lang.t("update.ready");
            case "failed" -> Lang.t("update.failed");
            default -> Lang.t("update.available", r.tag().replaceFirst("^v", ""));
        }));
    }

    /** In-game HUD (screenshot animation). */
    public static void onHud(GuiGraphics g) {
        ScreenshotFx.render(g);
    }
}
