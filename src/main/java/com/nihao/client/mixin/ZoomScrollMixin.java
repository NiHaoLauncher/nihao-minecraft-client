package com.nihao.client.mixin;

//#if ZOOM
import com.nihao.client.Zoom;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The mouse wheel changes the zoom instead of the hotbar slot while zooming (versions with the `zoom` feature flag). */
@Mixin(MouseHandler.class)
public abstract class ZoomScrollMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void nihao$scroll(long window, double x, double y, CallbackInfo ci) {
        if (Zoom.scroll(y)) ci.cancel();
    }
}
//#else
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

/** Placeholder: zoom is only on versions with the `zoom` feature flag (they get the real file). */
@Mixin(LivingEntityRenderer.class)
public abstract class ZoomScrollMixin {}
//#endif
