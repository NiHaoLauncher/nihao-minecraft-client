package com.nihao.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class NiHaoClient implements ClientModInitializer {
    private static final ResourceLocation ICON = Compat.id("nihao", "textures/gui/screenshot.png");

    private static String version(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((g, tick) -> ScreenshotFx.render(g));
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof PauseScreen || screen instanceof TitleScreen)) return;
            // Square icon button right after the bottom-most (then right-most) vanilla button.
            AbstractWidget last = null;
            for (AbstractWidget b : Screens.getButtons(screen)) {
                if (b.getHeight() != 20) continue; // skips the title screen's small copyright link, which sits lowest
                if (last == null || b.getY() > last.getY() || (b.getY() == last.getY() && b.getX() > last.getX())) last = b;
            }
            String label = "NiHaoClient " + version("minecraft") + " " + version("nihao").split("\\+")[0];
            ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) ->
                    g.drawString(client.font, label, 4, 4, 0xFFFFFFFF));
            if (last == null) return;
            Button btn = Button.builder(Component.empty(), b -> client.setScreen(new ScreenshotsScreen(screen)))
                    .bounds(last.getX() + last.getWidth() + 4, last.getY(), 20, 20)
                    .tooltip(Tooltip.create(Component.literal("Screenshots"))).build();
            Screens.getButtons(screen).add(btn);
            ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) ->
                    Compat.blit(g, ICON, btn.getX() + 2, btn.getY() + 2, 16, 16, 16, 16));
        });
    }
}
