package com.nihao.client.mixin;

import com.nihao.client.ClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    // 26.x moved Minecraft.setScreen to Gui.setScreen: it is hooked in GuiMixin
//#if MC < V26_2
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void nihao$screenSet(Screen requested, CallbackInfo ci) {
        Screen current = ((Minecraft) (Object) this).screen;
        if (current != null) ClientHooks.onSetScreen(current);
    }
//#endif
}
