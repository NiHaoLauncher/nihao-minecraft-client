package com.nihao.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** Version-specific Minecraft API calls (MC 1.21.2 - 1.21.4). Same class name/signatures in every shims/* folder. */
final class Compat {
    private Compat() {}

    static ResourceLocation id(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    static String name(PlayerInfo info) {
        return info.getProfile().getName();
    }

    static ResourceLocation skinTexture(PlayerSkin s) {
        return s.texture();
    }

    static ResourceLocation capeTexture(PlayerSkin s) {
        return s.capeTexture();
    }

    static boolean isSlim(PlayerSkin s) {
        return s.model() == PlayerSkin.Model.SLIM;
    }

    static PlayerSkin skin(PlayerSkin base, ResourceLocation tex, ResourceLocation cape, boolean slim) {
        return new PlayerSkin(tex, base.textureUrl(), cape, base.elytraTexture(),
                slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, base.secure());
    }

    static DynamicTexture dynamicTexture(NativeImage img) {
        return new DynamicTexture(img);
    }

    static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int texW, int texH) {
        g.blit(net.minecraft.client.renderer.RenderType::guiTextured, tex, x, y, 0f, 0f, w, h, texW, texH, texW, texH);
    }
}
