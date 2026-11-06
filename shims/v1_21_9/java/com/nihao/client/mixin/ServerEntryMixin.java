package com.nihao.client.mixin;

import com.nihao.client.ServerAuth;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.multiplayer.ServerSelectionList$OnlineServerEntry")
public abstract class ServerEntryMixin {
    @Shadow @Final private ServerData serverData;

    @Inject(method = "renderContent", at = @At("TAIL"))
    private void nihao$auth(GuiGraphics g, int mx, int my, boolean hovered, float delta, CallbackInfo ci) {
        var e = (net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.Entry) (Object) this;
        ServerAuth.draw(g, serverData, e.getContentX() + e.getContentWidth() + 2, e.getContentY(), mx, my);
    }
}
