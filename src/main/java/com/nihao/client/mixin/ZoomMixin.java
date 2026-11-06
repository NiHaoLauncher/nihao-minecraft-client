package com.nihao.client.mixin;

//#if ZOOM && MC >= V26_1
import com.nihao.client.NiHaoConfig;
import com.nihao.client.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The zoom key narrows the field of view of the world and of the hand (Minecraft 26.x). */
@Mixin(Camera.class)
public abstract class ZoomMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void nihao$zoom(float partial, CallbackInfoReturnable<Float> cir) {
        if (NiHaoConfig.zoom) cir.setReturnValue(cir.getReturnValue() * (float) Zoom.factor());
    }

    // Camera.update calls calculateFov first, so the hand sees the same multiplier within the frame
    @Inject(method = "calculateHudFov", at = @At("RETURN"), cancellable = true)
    private void nihao$zoomHand(float partial, CallbackInfoReturnable<Float> cir) {
        if (NiHaoConfig.zoom) cir.setReturnValue(cir.getReturnValue() * (float) Zoom.scale());
    }
}
//#elif ZOOM && MC >= V1_21_2
import com.nihao.client.NiHaoConfig;
import com.nihao.client.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The zoom key narrows the field of view of the world and of the hand (Minecraft 1.21.2 - 1.21.11: getFov returns a float). */
@Mixin(GameRenderer.class)
public abstract class ZoomMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void nihao$zoom(Camera camera, float partial, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        // the world asks first (useFovSetting), the hand (no setting) later in the same frame
        if (NiHaoConfig.zoom) cir.setReturnValue(cir.getReturnValue() * (float) (useFovSetting ? Zoom.factor() : Zoom.scale()));
    }
}
//#elif ZOOM
import com.nihao.client.NiHaoConfig;
import com.nihao.client.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The zoom key narrows the field of view of the world and of the hand (Minecraft 1.20.4 - 1.21.1: getFov returns a double). */
@Mixin(GameRenderer.class)
public abstract class ZoomMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void nihao$zoom(Camera camera, float partial, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        // the world asks first (useFovSetting), the hand (no setting) later in the same frame
        if (NiHaoConfig.zoom) cir.setReturnValue(cir.getReturnValue() * (useFovSetting ? Zoom.factor() : Zoom.scale()));
    }
}
//#else
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

/** Placeholder: zoom is only on versions with the `zoom` feature flag (they get the real file). */
@Mixin(LivingEntityRenderer.class)
public abstract class ZoomMixin {}
//#endif
