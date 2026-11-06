package com.nihao.client.mixin;

//#if MC >= V26_2
import com.nihao.client.ClientHooks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The in-game HUD (26.x split it out of Gui; older versions hook Gui.render in GuiMixin). */
@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void nihao$hud(GuiGraphics g, DeltaTracker tick, CallbackInfo ci) {
        ClientHooks.onHud(g);
    }
}
//#else
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

/** Placeholder: the HUD has its own class only since 26.x (older versions hook Gui.render in GuiMixin). */
@Mixin(LivingEntityRenderer.class)
public abstract class HudMixin {}
//#endif
