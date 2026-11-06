package com.nihao.client.mixin;

import com.nihao.client.ClientHooks;
import net.minecraft.client.gui.Gui;
//#if MC >= V26_2
import net.minecraft.client.gui.screens.Screen;
//#else
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
//#if MC >= V26_2
    // 26.x moved Minecraft.setScreen to Gui.setScreen (the HUD is drawn by Hud: HudMixin); the screen is initialised by the time this runs
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void nihao$screenSet(Screen requested, CallbackInfo ci) {
        Screen current = ((Gui) (Object) this).screen();
        if (current != null) ClientHooks.onSetScreen(current);
    }
//#else
    @Inject(method = "render", at = @At("TAIL"))
    private void nihao$hud(GuiGraphics g, DeltaTracker tick, CallbackInfo ci) {
        ClientHooks.onHud(g);
    }
//#endif
}
