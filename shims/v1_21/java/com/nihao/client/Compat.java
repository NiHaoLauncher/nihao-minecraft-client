package com.nihao.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** Version-specific Minecraft API calls (MC 1.21 - 1.21.1). Same class name/signatures in every shims/* folder. */
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
        g.blit(tex, x, y, w, h, 0f, 0f, texW, texH, texW, texH);
    }

    /** Draws the rw x rh region at (u,v) of the texture scaled to w x h. */
    static void blitUv(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, float u, float v, int rw, int rh, int texW, int texH) {
        g.blit(tex, x, y, w, h, u, v, rw, rh, texW, texH);
    }

    static int protocol() {
        return net.minecraft.SharedConstants.getCurrentVersion().getProtocolVersion();
    }

    static void tooltip(GuiGraphics g, net.minecraft.network.chat.Component text, int x, int y) {
        g.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, text, x, y);
    }

    /** One model vertex (position, ARGB colour, uv, overlay, light, normal). */
    static void vertex(com.mojang.blaze3d.vertex.VertexConsumer vc, float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        vc.addVertex(x, y, z, color, u, v, overlay, light, nx, ny, nz);
    }
}
