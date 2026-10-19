package com.nihao.client.mixin;

import com.nihao.client.ScreenshotFx;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    // Replaces the "Saved screenshot as ..." chat line with the screenshot animation.
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void nihao$screenshotSaved(Component message, CallbackInfo ci) {
        if (message.getContents() instanceof TranslatableContents t && "screenshot.success".equals(t.getKey())) {
            ScreenshotFx.trigger();
            ci.cancel();
        }
    }
}
