package com.nihao.client;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Loader-specific facts (Fabric). Same class name/signatures in every loaders/* folder. */
final class Platform {
    private Platform() {}

    static String loader() {
        return "fabric";
    }

    static Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    static String version(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("0");
    }

    /** This mod's jar, or null when running from a directory (dev environment). */
    static Path modJar() {
        try {
            Path p = FabricLoader.getInstance().getModContainer("nihao").orElseThrow().getOrigin().getPaths().get(0);
            return Files.isRegularFile(p) ? p : null;
        } catch (Exception e) {
            return null;
        }
    }
}
