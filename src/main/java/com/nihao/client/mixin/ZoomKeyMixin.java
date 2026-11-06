package com.nihao.client.mixin;

//#if ZOOM
import com.nihao.client.Zoom;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/** Puts the Zoom key into the Controls screen (before options.txt is read, so the saved key is applied) (versions with the `zoom` feature flag). */
@Mixin(Options.class)
public abstract class ZoomKeyMixin {
    @Shadow @Final @Mutable public KeyMapping[] keyMappings;

    @Inject(method = "load", at = @At("HEAD"))
    private void nihao$key(CallbackInfo ci) {
        if (Arrays.asList(keyMappings).contains(Zoom.KEY)) return;
        keyMappings = Arrays.copyOf(keyMappings, keyMappings.length + 1);
        keyMappings[keyMappings.length - 1] = Zoom.KEY;
    }
}
//#else
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

/** Placeholder: zoom is only on versions with the `zoom` feature flag (they get the real file). */
@Mixin(LivingEntityRenderer.class)
public abstract class ZoomKeyMixin {}
//#endif
