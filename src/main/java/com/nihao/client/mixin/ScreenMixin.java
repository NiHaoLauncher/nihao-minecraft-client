package com.nihao.client.mixin;

import com.nihao.client.ClientHooks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    // Vanilla clears the widgets on resize; the mod's buttons are added again.
    @Inject(method = "rebuildWidgets", at = @At("TAIL"))
    private void nihao$rebuilt(CallbackInfo ci) {
        ClientHooks.onInit((Screen) (Object) this);
    }

//#if MC >= V26_1
    @Inject(method = "extractRenderState", at = @At("TAIL"))
//#else
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
//#endif
    private void nihao$render(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ClientHooks.onRender((Screen) (Object) this, g);
    }
}
