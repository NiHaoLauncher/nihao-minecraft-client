package com.nihao.client.mixin;

import com.nihao.client.ElySkins;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void nihao$elySkin(CallbackInfoReturnable<PlayerSkin> cir) {
        cir.setReturnValue(ElySkins.apply((PlayerInfo) (Object) this, cir.getReturnValue()));
    }
}
