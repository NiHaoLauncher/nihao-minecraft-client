package com.nihao.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Replaces player skins/capes with the ones from ely.by. Falls back to the original skin until loaded or if none exists. */
public final class ElySkins {
    private static final String API = "https://skinsystem.ely.by/textures/";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    // Per-name result: Entry(null) = lookup in flight or failed (not retried this session), otherwise the ready skin.
    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();

    private record Entry(PlayerSkin skin) {}

    private ElySkins() {}

    public static PlayerSkin apply(PlayerInfo info, PlayerSkin original) {
        String name = Compat.name(info);
        Entry e = CACHE.get(name);
        if (e == null) {
            CACHE.put(name, new Entry(null));
            fetch(name, original);
            return original;
        }
        return e.skin() != null ? e.skin() : original;
    }

    private static void fetch(String name, PlayerSkin original) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(API + name)).timeout(Duration.ofSeconds(10)).build();
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                return r.statusCode() == 200 ? JsonParser.parseString(r.body()).getAsJsonObject() : null;
            } catch (Exception ex) {
                return null; // network error: keep vanilla skin
            }
        }).thenAccept(json -> {
            if (json != null) Minecraft.getInstance().execute(() -> register(name, json, original));
        });
    }

    private static void register(String name, JsonObject json, PlayerSkin original) {
        ResourceLocation skin = Compat.skinTexture(original);
        ResourceLocation cape = Compat.capeTexture(original);
        boolean slim = Compat.isSlim(original);
        // Textures are fetched/registered in the background; until then the vanilla skin stays in place.
        if (json.has("SKIN")) {
            JsonObject s = json.getAsJsonObject("SKIN");
            skin = texture(name, "skin", s.get("url").getAsString(), true);
            slim = s.has("metadata") && "slim".equals(s.getAsJsonObject("metadata").get("model").getAsString());
        }
        if (json.has("CAPE")) {
            cape = texture(name, "cape", json.getAsJsonObject("CAPE").get("url").getAsString(), false);
        }
        CACHE.put(name, new Entry(Compat.skin(original, skin, cape, slim)));
    }

    /** Returns the id immediately; the image is downloaded and registered later (missing texture meanwhile). */
    private static ResourceLocation texture(String name, String kind, String url, boolean legacyFix) {
        ResourceLocation id = Compat.id("nihao", kind + "/" + name.toLowerCase().replaceAll("[^a-z0-9_.-]", "_"));
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).build();
        CompletableFuture.supplyAsync(() -> {
            try {
                return HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
            } catch (Exception ex) {
                return null;
            }
        }).thenAccept(bytes -> {
            if (bytes == null) return;
            Minecraft.getInstance().execute(() -> {
                try {
                    NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
                    if (legacyFix && img.getHeight() == 32) img = upgradeLegacy(img);
                    Minecraft.getInstance().getTextureManager().register(id, Compat.dynamicTexture(img));
                } catch (Exception ignored) {
                    // not a PNG: leave the texture unregistered
                }
            });
        });
        return id;
    }

    /** 64x32 skin to 64x64, mirroring the right limbs onto the left ones (same as vanilla HttpTexture). */
    private static NativeImage upgradeLegacy(NativeImage old) {
        NativeImage img = new NativeImage(64, 64, true);
        img.copyFrom(old);
        old.close();
        int[][] r = {{4, 16, 16, 32, 4, 4}, {8, 16, 16, 32, 4, 4}, {0, 20, 24, 32, 4, 12}, {4, 20, 16, 32, 4, 12},
                {8, 20, 8, 32, 4, 12}, {12, 20, 16, 32, 4, 12}, {44, 16, -8, 32, 4, 4}, {48, 16, -8, 32, 4, 4},
                {40, 20, 0, 32, 4, 12}, {44, 20, -8, 32, 4, 12}, {48, 20, -16, 32, 4, 12}, {52, 20, -8, 32, 4, 12}};
        for (int[] c : r) img.copyRect(c[0], c[1], c[2], c[3], c[4], c[5], true, false);
        return img;
    }
}
